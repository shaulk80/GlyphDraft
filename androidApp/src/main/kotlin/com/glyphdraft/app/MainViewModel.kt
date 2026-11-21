package com.glyphdraft.app

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glyphdraft.core.LoggingOcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.ResultBridge
import com.glyphdraft.core.knownLanguages
import com.glyphdraft.engine.OnDeviceEngine
import com.glyphdraft.engine.OnnxSelfTest
import com.glyphdraft.engine.TesseractSelfTest
import com.glyphdraft.engine.log.FileLog
import com.glyphdraft.engine.ocr.AndroidImageDecoder
import com.glyphdraft.engine.settings.FileSettingsStore
import com.glyphdraft.ui.state.AppController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Composition root on Android. It outlives the activity, so the screen state survives rotation; the activity
 * only attaches its picker launchers to the two bridges.
 *
 * The data folder is the app-private `filesDir`: models in `models/`, the log file in `logs/`. Messages go to logcat
 * (the smoke test reads it) and to the log file.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val imageBridge = ResultBridge<String, Uri>()
    val saveBridge = ResultBridge<String, Uri>()

    private val dataDirectory = application.filesDir.toPath()
    private val fileLog = FileLog(dataDirectory.resolve("logs").resolve("glyphdraft.log"))

    // The models are opened on first use, so building the engine costs nothing at start.
    private val engine = OnDeviceEngine(dataDirectory.resolve("models"), AndroidImageDecoder())

    val controller =
        AppController(
            scope = viewModelScope,
            engine = LoggingOcrEngine(engine.engine, ::logRecognition),
            picker = AndroidImagePicker(application.contentResolver, imageBridge),
            saver = AndroidTextSaver(application.contentResolver, saveBridge),
            catalog = engine.catalog,
            installer = engine.installer,
            settingsStore =
                FileSettingsStore(
                    dataDirectory.resolve("settings.properties"),
                    engine.catalog.knownLanguages().toList(),
                    fileLog,
                ),
        )

    init {
        // Proves that ONNX Runtime and Tesseract load on this device; scripts/smoke-android.sh looks for the lines.
        viewModelScope.launch(Dispatchers.Default) {
            when (val outcome = OnnxSelfTest.run()) {
                is Outcome.Success -> logInfo(outcome.value)
                is Outcome.Failure -> logError("engine self-test failed: ${outcome.message}", outcome.cause)
            }

            when (val outcome = TesseractSelfTest.run()) {
                is Outcome.Success -> logInfo(outcome.value)
                is Outcome.Failure -> logError("tesseract self-test failed: ${outcome.message}", outcome.cause)
            }
        }
    }

    override fun onCleared() {
        engine.close()
        super.onCleared()
    }

    private fun logRecognition(message: String, cause: Throwable?) {
        if (cause == null) {
            logInfo(message)
        } else {
            logError(message, cause)
        }
    }

    private fun logInfo(message: String) {
        Log.i(TAG, message)
        fileLog.info(message)
    }

    private fun logError(message: String, cause: Throwable?) {
        Log.e(TAG, message, cause)
        if (cause == null) {
            fileLog.info(message)
        } else {
            fileLog.error(message, cause)
        }
    }

    private companion object {
        const val TAG = "GlyphDraft"
    }
}

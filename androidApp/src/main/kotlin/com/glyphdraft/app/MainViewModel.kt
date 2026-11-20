package com.glyphdraft.app

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.ResultBridge
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.engine.OnnxSelfTest
import com.glyphdraft.ui.state.AppController
import kotlinx.coroutines.launch

/**
 * Composition root on Android. It outlives the activity, so the screen state survives rotation; the activity
 * only attaches its picker launchers to the two bridges.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val imageBridge = ResultBridge<String, Uri>()
    val saveBridge = ResultBridge<String, Uri>()

    val controller =
        AppController(
            scope = viewModelScope,
            engine = StubOcrEngine(),
            picker = AndroidImagePicker(application.contentResolver, imageBridge),
            saver = AndroidTextSaver(application.contentResolver, saveBridge),
        )

    init {
        // Proves that ONNX Runtime loads on this device; scripts/smoke-android.sh looks for the line in logcat.
        viewModelScope.launch {
            when (val outcome = OnnxSelfTest.run()) {
                is Outcome.Success -> Log.i(TAG, outcome.value)
                is Outcome.Failure -> Log.e(TAG, "engine self-test failed: ${outcome.message}", outcome.cause)
            }
        }
    }

    private companion object {
        const val TAG = "GlyphDraft"
    }
}

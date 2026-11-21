package com.glyphdraft.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.glyphdraft.core.LoggingOcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.JavaCppNativeLibraries
import com.glyphdraft.engine.OnDeviceEngine
import com.glyphdraft.engine.OnnxNativeLibraries
import com.glyphdraft.engine.OnnxSelfTest
import com.glyphdraft.engine.TesseractSelfTest
import com.glyphdraft.engine.log.FileLog
import com.glyphdraft.engine.ocr.AwtImageDecoder
import com.glyphdraft.ui.App
import com.glyphdraft.ui.state.AppController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.file.Path

private const val DEV_VERSION = "dev"

fun main() {
    val version = System.getProperty("jpackage.app-version") ?: DEV_VERSION
    val dataDirectory = DataDirectory.resolve()
    val log = FileLog(dataDirectory.resolve("logs").resolve("glyphdraft.log"))
    val logged = log.info("GlyphDraft $version starting")
    if (logged is Outcome.Failure) {
        System.err.println(logged.message)
    }
    prepareNativeLibraries(dataDirectory, log)
    runEngineSelfTest(log)

    // The models are opened on first use, so building the engine costs nothing at start.
    val engine = OnDeviceEngine(dataDirectory.resolve("models"), AwtImageDecoder())
    application {
        Window(
            onCloseRequest = {
                engine.close()
                exitApplication()
            },
            title = "GlyphDraft",
        ) {
            val scope = rememberCoroutineScope()
            val controller =
                remember {
                    val chooser = FileKitFileChooser(window, LastFolder(dataDirectory.resolve("last-folder.txt"), log), log)
                    AppController(
                        scope,
                        LoggingOcrEngine(engine.engine) { message, cause -> logRecognition(log, message, cause) },
                        DesktopImagePicker(chooser, log),
                        DesktopTextSaver(chooser, log),
                        engine.catalog,
                        engine.installer,
                    )
                }
            App(controller, version)
        }
    }
}

private fun logRecognition(log: FileLog, message: String, cause: Throwable?) {
    if (cause == null) {
        log.info(message)
    } else {
        log.error(message, cause)
    }
}

/**
 * Points ONNX Runtime and JavaCPP at the data folder. Both must be done before the first use of the library, which is
 * why the engine is built after this.
 */
private fun prepareNativeLibraries(dataDirectory: Path, log: FileLog) {
    val libraries = OnnxNativeLibraries.prepare(dataDirectory.resolve("native"))
    if (libraries is Outcome.Failure) {
        log.info("ONNX Runtime stays in the temp folder: ${libraries.message}")
    }

    val cache = JavaCppNativeLibraries.prepare(dataDirectory.resolve("native").resolve("javacpp"))
    if (cache is Outcome.Failure) {
        log.info("JavaCPP stays in its default cache folder: ${cache.message}")
    }
}

/** Loads ONNX Runtime and Tesseract in the background and logs the results; the smoke test of the package looks for them. */
private fun runEngineSelfTest(log: FileLog) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        when (val outcome = OnnxSelfTest.run()) {
            is Outcome.Success -> log.info(outcome.value)
            is Outcome.Failure -> log.info("engine self-test failed: ${outcome.message}")
        }

        when (val outcome = TesseractSelfTest.run()) {
            is Outcome.Success -> log.info(outcome.value)
            is Outcome.Failure -> log.info("tesseract self-test failed: ${outcome.message}")
        }
    }
}

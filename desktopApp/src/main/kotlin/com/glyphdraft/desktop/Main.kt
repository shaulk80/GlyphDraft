package com.glyphdraft.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.engine.OnnxNativeLibraries
import com.glyphdraft.engine.OnnxSelfTest
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
    runEngineSelfTest(dataDirectory, log)

    application {
        Window(onCloseRequest = ::exitApplication, title = "GlyphDraft") {
            val scope = rememberCoroutineScope()
            val controller =
                remember {
                    val chooser = FileKitFileChooser(window, LastFolder(dataDirectory.resolve("last-folder.txt"), log), log)
                    AppController(scope, StubOcrEngine(), DesktopImagePicker(chooser, log), DesktopTextSaver(chooser, log))
                }
            App(controller, version)
        }
    }
}

/** Loads ONNX Runtime in the background and logs the result; the smoke test of the package looks for it. */
private fun runEngineSelfTest(dataDirectory: Path, log: FileLog) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        // Before the first use of ONNX Runtime, so that it loads from the data folder and not from the temp folder.
        val libraries = OnnxNativeLibraries.prepare(dataDirectory.resolve("native"))
        if (libraries is Outcome.Failure) {
            log.info("ONNX Runtime stays in the temp folder: ${libraries.message}")
        }

        when (val outcome = OnnxSelfTest.run()) {
            is Outcome.Success -> log.info(outcome.value)
            is Outcome.Failure -> log.info("engine self-test failed: ${outcome.message}")
        }
    }
}

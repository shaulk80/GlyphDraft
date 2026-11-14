package com.glyphdraft.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.ui.App
import com.glyphdraft.ui.state.AppController

private const val DEV_VERSION = "dev"

fun main() {
    val version = System.getProperty("jpackage.app-version") ?: DEV_VERSION
    val dataDirectory = DataDirectory.resolve()
    val log = FileLog(dataDirectory.resolve("logs").resolve("glyphdraft.log"))
    val logged = log.info("GlyphDraft $version starting")
    if (logged is Outcome.Failure) {
        System.err.println(logged.message)
    }

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

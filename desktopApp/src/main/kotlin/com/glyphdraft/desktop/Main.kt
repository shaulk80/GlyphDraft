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
    val logFile = DataDirectory.resolve().resolve("logs").resolve("glyphdraft.log")
    val logged = FileLog(logFile).info("GlyphDraft $version starting")
    if (logged is Outcome.Failure) {
        System.err.println(logged.message)
    }

    application {
        val scope = rememberCoroutineScope()
        val controller = remember { AppController(scope, StubOcrEngine(), PlaceholderImagePicker(), PlaceholderTextSaver()) }
        Window(onCloseRequest = ::exitApplication, title = "GlyphDraft") {
            App(controller, version)
        }
    }
}

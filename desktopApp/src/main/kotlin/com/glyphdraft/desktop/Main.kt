package com.glyphdraft.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.glyphdraft.core.Outcome
import com.glyphdraft.ui.App

private const val DEV_VERSION = "dev"

fun main() {
    val version = System.getProperty("jpackage.app-version") ?: DEV_VERSION
    val logFile = DataDirectory.resolve().resolve("logs").resolve("glyphdraft.log")
    val logged = FileLog(logFile).info("GlyphDraft $version starting")
    if (logged is Outcome.Failure) {
        System.err.println(logged.message)
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "GlyphDraft") {
            App(version)
        }
    }
}

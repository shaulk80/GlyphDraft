package com.glyphdraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TextSaver
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.ui.App
import com.glyphdraft.ui.state.AppController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { AppController(scope, StubOcrEngine(), PlaceholderImagePicker(), PlaceholderTextSaver()) }
            App(controller, BuildConfig.VERSION_NAME, onDarkThemeResolved = ::matchStatusBarToTheme)
        }
    }

    /** Edge-to-edge draws under the status bar: dark icons on a light theme, light icons on a dark one. */
    private fun matchStatusBarToTheme(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
    }
}

/** Temporary until the system pickers are wired (M1 task 6). */
private class PlaceholderImagePicker : ImagePicker {
    override suspend fun pick(): Outcome<ImageInput?> = Outcome.Success(ImageInput("sample.png", byteArrayOf(0)))
}

private class PlaceholderTextSaver : TextSaver {
    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> = Outcome.Failure("Saving is not wired yet.")
}

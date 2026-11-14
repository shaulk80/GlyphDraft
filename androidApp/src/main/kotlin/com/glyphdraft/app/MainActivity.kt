package com.glyphdraft.app

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.glyphdraft.core.Outcome
import com.glyphdraft.ui.App

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Must be registered before the activity is started; the results go to the view model bridges.
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> viewModel.imageBridge.deliver(uri) }
    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument(TEXT_MIME_TYPE)) { uri: Uri? -> viewModel.saveBridge.deliver(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.imageBridge.launcher = { mimeType -> launchSafely("image picker") { pickImage.launch(mimeType) } }
        viewModel.saveBridge.launcher = { name -> launchSafely("file saver") { createDocument.launch(name) } }
        setContent {
            val controller = viewModel.controller
            val state by controller.state.collectAsState()
            // The system Back key cancels a running recognition or goes one screen back; on Home it leaves the app.
            BackHandler(enabled = state.handlesBack, onBack = controller::handleBack)
            App(controller, BuildConfig.VERSION_NAME, onDarkThemeResolved = ::matchStatusBarToTheme)
        }
    }

    override fun onDestroy() {
        viewModel.imageBridge.launcher = null
        viewModel.saveBridge.launcher = null
        super.onDestroy()
    }

    private fun launchSafely(what: String, launch: () -> Unit): Outcome<Unit> {
        try {
            launch()
            return Outcome.Success(Unit)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No app can open the $what", e)
            return Outcome.Failure("This device has no app that can open the $what.", e)
        }
    }

    /** Edge-to-edge draws under the status bar: dark icons on a light theme, light icons on a dark one. */
    private fun matchStatusBarToTheme(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
    }

    private companion object {
        const val TAG = "GlyphDraft"
        const val TEXT_MIME_TYPE = "text/plain"
    }
}

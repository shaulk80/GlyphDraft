package com.glyphdraft.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glyphdraft.core.ResultBridge
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.ui.state.AppController

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
}

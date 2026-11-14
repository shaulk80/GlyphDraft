package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Language
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.TextSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the screen state and runs the user's actions. Not tied to any UI framework or lifecycle library.
 * Call it from one thread (the UI thread); [scope] decides where the work runs, the engines are suspending.
 */
class AppController(
    private val scope: CoroutineScope,
    private val engine: OcrEngine,
    private val picker: ImagePicker,
    private val saver: TextSaver,
) {
    private val mutableState = MutableStateFlow(AppState())
    private var recognizeJob: Job? = null

    val state: StateFlow<AppState> = mutableState.asStateFlow()

    fun pickImage() {
        launchExclusive(Activity.Picking) {
            when (val outcome = picker.pick()) {
                is Outcome.Success -> acceptPickedImage(outcome)
                is Outcome.Failure -> showNotice(outcome.message, isError = true)
            }
        }
    }

    fun selectLanguage(language: Language?) {
        mutableState.update { it.copy(language = language) }
    }

    fun recognize() {
        val image = state.value.image
        if (image == null) {
            return
        }

        recognizeJob =
            launchExclusive(Activity.Recognizing) {
                when (val outcome = engine.recognize(image, state.value.language)) {
                    is Outcome.Success -> showResult(outcome.value)
                    is Outcome.Failure -> showNotice(outcome.message, isError = true)
                }
            }
    }

    fun cancelRecognition() {
        if (state.value.activity == Activity.Recognizing) {
            recognizeJob?.cancel()
        }
    }

    fun saveResult() {
        val result = state.value.result
        val imageName = state.value.image?.name
        if (result == null || imageName == null) {
            return
        }

        launchExclusive(Activity.Saving) {
            when (val outcome = saver.save(suggestedFileName(imageName), result.text)) {
                is Outcome.Success -> confirmSaved(outcome.value)
                is Outcome.Failure -> showNotice(outcome.message, isError = true)
            }
        }
    }

    fun showCopied() {
        showNotice("Copied to the clipboard.", isError = false)
    }

    fun openSettings() {
        navigateTo(Screen.Settings)
    }

    /** Pops one screen; Home stays. Ignored while busy so a running action never finishes on a screen the user left. */
    fun back() {
        mutableState.update { current ->
            if (current.busy || current.backStack.size < 2) {
                current
            } else {
                current.copy(backStack = current.backStack.dropLast(1), notice = null)
            }
        }
    }

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    fun setTheme(theme: ThemeMode) {
        mutableState.update { it.copy(settings = it.settings.copy(theme = theme)) }
    }

    /** Also preselects the language on the Home screen, so the change is visible at once. */
    fun setDefaultLanguage(language: Language?) {
        mutableState.update { it.copy(settings = it.settings.copy(defaultLanguage = language), language = language) }
    }

    private fun acceptPickedImage(outcome: Outcome.Success<ImageInput?>) {
        val image = outcome.value
        if (image == null) {
            return
        }

        mutableState.update { it.copy(image = image, result = null) }
    }

    private fun navigateTo(screen: Screen) {
        mutableState.update { current ->
            if (current.screen == screen) {
                current
            } else {
                current.copy(backStack = current.backStack + screen, notice = null)
            }
        }
    }

    private fun showResult(result: RecognitionResult) {
        mutableState.update { it.copy(result = result, backStack = it.backStack + Screen.Result) }
    }

    private fun confirmSaved(saved: Boolean) {
        if (saved) {
            showNotice("Saved.", isError = false)
        }
    }

    private fun showNotice(text: String, isError: Boolean) {
        mutableState.update { it.copy(notice = Notice(text, isError)) }
    }

    /** Starts [block] unless something is already running; always returns to Idle, also on cancellation. */
    private fun launchExclusive(activity: Activity, block: suspend () -> Unit): Job? {
        if (state.value.busy) {
            return null
        }

        mutableState.update { it.copy(activity = activity, notice = null) }
        return scope.launch {
            try {
                block()
            } finally {
                mutableState.update { it.copy(activity = Activity.Idle) }
            }
        }
    }

    private fun suggestedFileName(imageName: String): String = imageName.substringBeforeLast('.') + ".txt"
}

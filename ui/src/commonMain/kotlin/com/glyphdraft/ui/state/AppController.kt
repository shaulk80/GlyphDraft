package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.SettingsStore
import com.glyphdraft.core.TextSaver
import com.glyphdraft.core.ThemeMode
import com.glyphdraft.core.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds the screen state and runs the user's actions. Not tied to any UI framework or lifecycle library.
 * Call it from one thread (the UI thread); [scope] decides where the work runs, the engines are suspending.
 */
class AppController(
    private val scope: CoroutineScope,
    private val engine: OcrEngine,
    private val translator: Translator,
    private val picker: ImagePicker,
    private val saver: TextSaver,
    catalog: ModelCatalog,
    installer: ModelInstaller,
    private val settingsStore: SettingsStore,
) {
    private val mutableState =
        MutableStateFlow(AppState(packs = catalog.packs, installedPackIds = catalog.installedPackIds.value))
    private val installFlow = InstallFlow(mutableState, catalog, installer)
    private val saveLock = Mutex()
    private var workJob: Job? = null

    /** Ends when every cancelled job has really ended; the next job waits for it before it starts its work. */
    private var stopping: Job? = null

    /** Number of the latest job; a job that was overtaken (cancelled, then replaced) must not reset the activity. */
    private var generation = 0
    private var settingsChanged = false

    val state: StateFlow<AppState> = mutableState.asStateFlow()

    init {
        scope.launch { loadSettings() }
    }

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

    /** The language to translate the next result into; null means "do not translate". Does not translate by itself, see [translate]. */
    fun selectTarget(target: Language?) {
        mutableState.update { current -> current.copy(target = target, translation = current.translation?.takeIf { it.to == target }) }
    }

    /**
     * Reads the chosen image, then translates the text when a target is chosen. When the models for the language are
     * not installed, offers the download first.
     */
    fun recognize() {
        val image = state.value.image
        if (image == null || !state.value.canRecognize) {
            return
        }

        val pack = installFlow.packNeededFor(state.value.language)
        if (pack != null) {
            installFlow.offer(listOf(pack), AfterInstall.Recognize)
            return
        }

        workJob = launchExclusive(Activity.Recognizing) { read(image) }
    }

    /**
     * Translates the current result into the chosen target (also after the target was changed on the Result screen);
     * offers the download of the models of the route first, with their total size.
     */
    fun translate() {
        val result = state.value.result
        val target = state.value.target
        if (result == null || target == null || !state.value.canTranslate) {
            return
        }

        workJob = launchExclusive(Activity.Translating) { translateResult(result, target) }
    }

    /** Offers the download of [pack] from the settings screen; nothing is recognised afterwards. */
    fun requestInstall(pack: ModelPack) {
        if (state.value.busy || state.value.installPrompt != null || state.value.isInstalled(pack)) {
            return
        }

        installFlow.offer(listOf(pack), AfterInstall.Nothing)
    }

    /** The user agreed to the download of the offered pack (or asked to retry a failed one). */
    fun confirmInstall() {
        val prompt = state.value.installPrompt
        if (prompt == null || prompt.running) {
            return
        }

        workJob =
            launchExclusive(Activity.Installing) {
                if (installFlow.run(prompt)) {
                    continueAfterInstall(prompt.then)
                }
            }
    }

    private suspend fun continueAfterInstall(then: AfterInstall) {
        val image = state.value.image
        val result = state.value.result
        val target = state.value.target
        if (then == AfterInstall.Recognize && image != null) {
            mutableState.update { it.copy(activity = Activity.Recognizing) }
            read(image)
        }

        if (then == AfterInstall.Translate && result != null && target != null) {
            runTranslation(result, target)
        }
    }

    /** Closes the download dialog; a running download is cancelled and its partial file removed. */
    fun dismissInstall() {
        if (state.value.activity == Activity.Installing) {
            workJob?.cancel()
        } else {
            installFlow.dismiss()
        }
    }

    /**
     * Cancels a running download, recognition or translation. A recognition or translation is shown as cancelled and the
     * screen is free at once ([Activity.Cancelling]); the engine needs a moment to leave its current native step, and
     * whatever starts next waits for it ([stopping]), so two engine runs never go side by side. A cancelled translation
     * leaves the recognised text on the screen.
     */
    fun cancelRecognition() {
        val activity = state.value.activity
        val engineRuns = activity == Activity.Recognizing || activity == Activity.Translating
        if (engineRuns) {
            stopping = stoppedAfter(stopping, workJob)
            mutableState.update { it.copy(activity = Activity.Cancelling) }
        }

        if (engineRuns || activity == Activity.Installing) {
            workJob?.cancel()
        }
    }

    /** A job that ends when [earlier] and [cancelled] have both ended. */
    private fun stoppedAfter(earlier: Job?, cancelled: Job?): Job =
        scope.launch {
            earlier?.join()
            cancelled?.join()
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

    /**
     * The system Back key: cancels a running download or recognition, closes the download dialog, otherwise goes back
     * one screen (see [AppState.handlesBack]).
     */
    fun handleBack() {
        when {
            state.value.installPrompt != null -> dismissInstall()
            state.value.activity == Activity.Recognizing || state.value.activity == Activity.Translating -> cancelRecognition()
            else -> back()
        }
    }

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    fun setTheme(theme: ThemeMode) {
        mutableState.update { it.copy(settings = it.settings.copy(theme = theme)) }
        persistSettings()
    }

    /** Also preselects the language on the Home screen, so the change is visible at once. */
    fun setDefaultLanguage(language: Language?) {
        mutableState.update { it.copy(settings = it.settings.copy(defaultLanguage = language), language = language) }
        persistSettings()
    }

    /** Also preselects the target on the Home screen, so the change is visible at once; null means "do not translate". */
    fun setDefaultTarget(target: Language?) {
        mutableState.update { it.copy(settings = it.settings.copy(defaultTarget = target), target = target) }
        persistSettings()
    }

    /** Applies the stored settings, unless the user has already changed one while they were being read. */
    private suspend fun loadSettings() {
        val loaded = settingsStore.load()
        mutableState.update { current ->
            if (settingsChanged) {
                current
            } else {
                current.copy(
                    settings = loaded,
                    language = current.language ?: loaded.defaultLanguage,
                    target = current.target ?: loaded.defaultTarget,
                )
            }
        }
    }

    /** Saves the current settings after the saves already queued; a failure is shown, the change stays for this run. */
    private fun persistSettings() {
        settingsChanged = true
        scope.launch {
            saveLock.withLock {
                val outcome = settingsStore.save(state.value.settings)
                if (outcome is Outcome.Failure) {
                    showNotice(outcome.message, isError = true)
                }
            }
        }
    }

    private suspend fun read(image: ImageInput) {
        when (val outcome = engine.recognize(image, state.value.language)) {
            is Outcome.Success -> readSucceeded(outcome.value)
            is Outcome.Failure -> showNotice(outcome.message, isError = true)
        }
    }

    /** Shows the text, then translates it when a target is chosen and the engine trusts what it read. */
    private suspend fun readSucceeded(result: RecognitionResult) {
        showResult(result)
        val target = state.value.target
        if (target != null && !result.lowQuality) {
            translateResult(result, target)
        }
    }

    /** Translates [result] into [target]: says so when nothing is to do, offers the missing models, else runs the route. */
    private suspend fun translateResult(result: RecognitionResult, target: Language) {
        if (result.language == target) {
            showNotice("The text is already in ${target.displayName}.", isError = false)
            return
        }

        val missing = installFlow.packsNeededForTranslation(result.language, target)
        when {
            missing == null ->
                showNotice("There is no translation from ${result.language.displayName} to ${target.displayName}.", isError = true)

            missing.isNotEmpty() -> installFlow.offer(missing, AfterInstall.Translate)
            else -> runTranslation(result, target)
        }
    }

    private suspend fun runTranslation(result: RecognitionResult, target: Language) {
        mutableState.update { it.copy(activity = Activity.Translating, translation = null) }
        when (val outcome = translator.translate(result.text, result.language, target)) {
            is Outcome.Success -> mutableState.update { it.copy(translation = outcome.value) }
            is Outcome.Failure -> showNotice(outcome.message, isError = true)
        }
    }

    private fun acceptPickedImage(outcome: Outcome.Success<ImageInput?>) {
        val image = outcome.value
        if (image == null) {
            return
        }

        mutableState.update { it.copy(image = image, result = null, translation = null) }
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
        mutableState.update { it.copy(result = result, translation = null, backStack = it.backStack + Screen.Result) }
    }

    private fun confirmSaved(saved: Boolean) {
        if (saved) {
            showNotice("Saved.", isError = false)
        }
    }

    private fun showNotice(text: String, isError: Boolean) {
        mutableState.update { it.copy(notice = Notice(text, isError)) }
    }

    /**
     * Starts [block] unless something is already running, after any cancelled job has really ended; returns to Idle when
     * it ends, also on cancellation, unless a newer job has taken over.
     */
    private fun launchExclusive(activity: Activity, block: suspend () -> Unit): Job? {
        if (state.value.busy) {
            return null
        }

        val waitFor = stopping
        val mine = ++generation
        mutableState.update { it.copy(activity = activity, notice = null) }
        return scope.launch {
            try {
                waitFor?.join()
                block()
            } finally {
                if (generation == mine) {
                    mutableState.update { it.copy(activity = Activity.Idle) }
                }
            }
        }
    }

    private fun suggestedFileName(imageName: String): String = imageName.substringBeforeLast('.') + ".txt"
}

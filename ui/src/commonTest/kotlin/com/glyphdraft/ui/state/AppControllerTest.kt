package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Settings
import com.glyphdraft.core.SettingsStore
import com.glyphdraft.core.TextSaver
import com.glyphdraft.core.ThemeMode
import com.glyphdraft.core.stub.StubModelCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val photo = ImageInput("photo.png", byteArrayOf(1))
private val sample = RecognitionResult("hello", Language.English, detected = true)

private class FakePicker(var outcome: Outcome<ImageInput?> = Outcome.Success(photo)) : ImagePicker {
    var calls = 0

    override suspend fun pick(): Outcome<ImageInput?> {
        calls++
        return outcome
    }
}

/** Waits on [gate] when set, so a test can look at the state while recognition is running. */
private class FakeEngine(var outcome: Outcome<RecognitionResult> = Outcome.Success(sample)) : OcrEngine {
    var gate: CompletableDeferred<Unit>? = null
    var lastHint: Language? = null
    var calls = 0

    /** When set, waiting on [gate] cannot be interrupted, like a native call; cancellation shows once it returns. */
    var uncancellable = false

    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> {
        calls++
        lastHint = languageHint
        if (uncancellable) {
            withContext(NonCancellable) { gate?.await() }
            coroutineContext.ensureActive()
        } else {
            gate?.await()
        }
        return outcome
    }
}

private class FakeSaver(var outcome: Outcome<Boolean> = Outcome.Success(true)) : TextSaver {
    var savedName: String? = null
    var savedText: String? = null

    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> {
        savedName = suggestedName
        savedText = text
        return outcome
    }
}

/** Marks the pack installed after a successful call; waits on [gate] first when set, so a test can cancel mid-download. */
private class FakeInstaller(private val catalog: () -> StubModelCatalog) : ModelInstaller {
    var outcomes = ArrayDeque<Outcome<Unit>>()
    var gate: CompletableDeferred<Unit>? = null
    val installed = ArrayList<String>()

    override suspend fun install(pack: ModelPack, onProgress: (InstallProgress) -> Unit): Outcome<Unit> {
        onProgress(InstallProgress(pack.sizeBytes / 2, pack.sizeBytes))
        gate?.await()
        val outcome = outcomes.removeFirstOrNull() ?: Outcome.Success(Unit)
        if (outcome is Outcome.Success) {
            installed.add(pack.id)
            catalog().markInstalled(pack.id)
        }

        return outcome
    }
}

private class FakeSettingsStore(var stored: Settings = Settings()) : SettingsStore {
    var saveOutcome: Outcome<Unit> = Outcome.Success(Unit)
    val saves = ArrayList<Settings>()

    override suspend fun load(): Settings = stored

    override suspend fun save(settings: Settings): Outcome<Unit> {
        saves.add(settings)
        return saveOutcome
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {
    private val picker = FakePicker()
    private val engine = FakeEngine()
    private val saver = FakeSaver()
    private var catalog = StubModelCatalog()
    private val installer = FakeInstaller { catalog }
    private val settingsStore = FakeSettingsStore()

    private fun TestScope.controller() = AppController(this, engine, picker, saver, catalog, installer, settingsStore)

    private fun TestScope.controllerWithImage(): AppController {
        val controller = controller()
        controller.pickImage()
        advanceUntilIdle()
        return controller
    }

    @Test
    fun pickingStoresImageAndCancelKeepsPreviousOne() =
        runTest {
            val controller = controllerWithImage()

            picker.outcome = Outcome.Success(null)
            controller.pickImage()
            advanceUntilIdle()

            assertEquals(photo, controller.state.value.image)
            assertEquals(Activity.Idle, controller.state.value.activity)
        }

    @Test
    fun pickFailureBecomesErrorNotice() =
        runTest {
            picker.outcome = Outcome.Failure("cannot read")
            val controller = controller()

            controller.pickImage()
            advanceUntilIdle()

            assertEquals(Notice("cannot read", isError = true), controller.state.value.notice)
            assertNull(controller.state.value.image)
            assertEquals(Activity.Idle, controller.state.value.activity)
        }

    @Test
    fun recognizeFailureBecomesErrorNoticeAndStaysOnHome() =
        runTest {
            engine.outcome = Outcome.Failure("no engine")
            val controller = controllerWithImage()

            controller.recognize()
            advanceUntilIdle()

            assertEquals(Notice("no engine", isError = true), controller.state.value.notice)
            assertEquals(Screen.Home, controller.state.value.screen)
            assertEquals(Activity.Idle, controller.state.value.activity)
        }

    @Test
    fun recognizeWithoutImageDoesNothing() =
        runTest {
            val controller = controller()

            controller.recognize()
            advanceUntilIdle()

            assertEquals(Activity.Idle, controller.state.value.activity)
            assertNull(controller.state.value.result)
        }

    @Test
    fun recognizeSendsLanguageHintAndOpensResult() =
        runTest {
            val cases = listOf<Language?>(null, Language.Russian)

            for (hint in cases) {
                val controller = controllerWithImage()
                controller.selectLanguage(hint)

                controller.recognize()
                advanceUntilIdle()

                assertEquals(hint, engine.lastHint)
                assertEquals(sample, controller.state.value.result)
                assertEquals(Screen.Result, controller.state.value.screen)
            }
        }

    @Test
    fun requestsWhileRecognizingAreIgnoredAndCancelReturnsToIdle() =
        runTest {
            val controller = controllerWithImage()
            engine.gate = CompletableDeferred()

            controller.recognize()
            runCurrent()
            controller.pickImage()
            controller.openSettings()
            controller.back()
            runCurrent()

            assertEquals(Activity.Recognizing, controller.state.value.activity)
            assertEquals(1, picker.calls)

            controller.cancelRecognition()
            advanceUntilIdle()

            assertEquals(Activity.Idle, controller.state.value.activity)
            assertNull(controller.state.value.result)
        }

    @Test
    fun saveUsesImageNameWithTxtExtensionAndConfirmsOnlyWhenSaved() =
        runTest {
            val cases: List<Pair<Outcome<Boolean>, Notice?>> =
                listOf(
                    Outcome.Success(true) to Notice("Saved.", isError = false),
                    Outcome.Success(false) to null,
                    Outcome.Failure("disk full") to Notice("disk full", isError = true),
                )

            for ((saveOutcome, expectedNotice) in cases) {
                saver.outcome = saveOutcome
                val controller = controllerWithImage()
                controller.recognize()
                advanceUntilIdle()

                controller.saveResult()
                advanceUntilIdle()

                assertEquals("photo.txt", saver.savedName)
                assertEquals("hello", saver.savedText)
                assertEquals(expectedNotice, controller.state.value.notice)
            }
        }

    @Test
    fun navigationKeepsHomeAtTheBottom() =
        runTest {
            val controller = controller()

            controller.back()
            controller.openSettings()
            controller.openSettings()
            assertEquals(listOf(Screen.Home, Screen.Settings), controller.state.value.backStack)

            controller.back()
            controller.back()
            assertEquals(listOf(Screen.Home), controller.state.value.backStack)
        }

    /** Hand test finding 9: the progress indicator stayed until the engine's current native step returned. */
    @Test
    fun cancelShowsAtOnceWhileTheEngineIsStillFinishingItsStep() =
        runTest {
            val controller = controllerWithImage()
            engine.gate = CompletableDeferred()
            engine.uncancellable = true

            controller.recognize()
            runCurrent()
            controller.cancelRecognition()
            runCurrent()

            assertEquals(Activity.Cancelling, controller.state.value.activity)
            assertEquals(true, controller.state.value.busy)
            assertEquals(false, controller.state.value.canRecognize)
            assertEquals(true, controller.state.value.handlesBack)

            engine.gate?.complete(Unit)
            advanceUntilIdle()

            assertEquals(Activity.Idle, controller.state.value.activity)
            assertNull(controller.state.value.result)
            assertEquals(true, controller.state.value.canRecognize)
        }

    @Test
    fun backKeyCancelsRecognitionThenGoesBackAndIsNotHandledOnHome() =
        runTest {
            val controller = controllerWithImage()
            assertEquals(false, controller.state.value.handlesBack)

            engine.gate = CompletableDeferred()
            controller.recognize()
            runCurrent()
            assertEquals(true, controller.state.value.handlesBack)
            controller.handleBack()
            advanceUntilIdle()
            assertEquals(Activity.Idle, controller.state.value.activity)
            assertEquals(Screen.Home, controller.state.value.screen)

            controller.openSettings()
            assertEquals(true, controller.state.value.handlesBack)
            controller.handleBack()
            assertEquals(Screen.Home, controller.state.value.screen)
            assertEquals(false, controller.state.value.handlesBack)
        }

    @Test
    fun defaultLanguagePreselectsLanguageAndThemeIsStored() =
        runTest {
            val controller = controller()

            controller.setDefaultLanguage(Language.Hebrew)
            controller.setTheme(ThemeMode.Dark)

            val state = controller.state.value
            assertEquals(Language.Hebrew, state.language)
            assertEquals(Language.Hebrew, state.settings.defaultLanguage)
            assertEquals(ThemeMode.Dark, state.settings.theme)
        }

    @Test
    fun storedSettingsAreAppliedAtStartAndPreselectTheLanguage() =
        runTest {
            settingsStore.stored = Settings(ThemeMode.Dark, Language.Hebrew)
            val controller = controller()

            advanceUntilIdle()

            val state = controller.state.value
            assertEquals(ThemeMode.Dark, state.settings.theme)
            assertEquals(Language.Hebrew, state.settings.defaultLanguage)
            assertEquals(Language.Hebrew, state.language)
            assertEquals(0, settingsStore.saves.size)
        }

    @Test
    fun everyChangeIsSavedAndALaterOneWinsOverTheLoad() =
        runTest {
            settingsStore.stored = Settings(ThemeMode.Light, Language.German)
            val controller = controller()

            controller.setTheme(ThemeMode.Dark)
            controller.setDefaultLanguage(Language.Russian)
            advanceUntilIdle()

            val expected = Settings(ThemeMode.Dark, Language.Russian)
            assertEquals(expected, controller.state.value.settings)
            assertEquals(listOf(expected, expected), settingsStore.saves)
        }

    @Test
    fun aFailedSaveIsShownAndTheChangeStaysForThisRun() =
        runTest {
            settingsStore.saveOutcome = Outcome.Failure("disk full")
            val controller = controller()

            controller.setTheme(ThemeMode.Dark)
            advanceUntilIdle()

            assertEquals(ThemeMode.Dark, controller.state.value.settings.theme)
            assertEquals(Notice("disk full", isError = true), controller.state.value.notice)
        }

    @Test
    fun noticeCanBeDismissedAndIsClearedByNavigation() =
        runTest {
            val controller = controller()
            controller.showCopied()
            assertNotNull(controller.state.value.notice)

            controller.dismissNotice()
            assertNull(controller.state.value.notice)

            controller.showCopied()
            controller.openSettings()
            assertNull(controller.state.value.notice)
        }

    @Test
    fun recognizeOffersTheDownloadOnlyWhenTheModelsAreMissing() =
        runTest {
            // language, installed packs, pack offered (null: reads at once)
            val cases =
                listOf(
                    Triple(Language.Russian, emptySet<String>(), "eslav"),
                    Triple(null, emptySet<String>(), "latin"),
                    Triple(Language.English, setOf("eslav"), "latin"),
                    Triple(Language.English, setOf("latin"), null),
                    Triple(null, setOf("hebrew"), null),
                )

            for ((language, installedPacks, offered) in cases) {
                catalog = StubModelCatalog(installed = installedPacks)
                engine.calls = 0
                val controller = controllerWithImage()
                controller.selectLanguage(language)

                controller.recognize()
                advanceUntilIdle()

                assertEquals(offered, controller.state.value.installPrompt?.pack?.id, "language $language, installed $installedPacks")
                assertEquals(if (offered == null) 1 else 0, engine.calls, "language $language, installed $installedPacks")
            }
        }

    @Test
    fun confirmingTheDownloadInstallsThePackAndThenReads() =
        runTest {
            catalog = StubModelCatalog(installed = emptySet())
            val controller = controllerWithImage()
            controller.selectLanguage(Language.Russian)
            controller.recognize()

            controller.confirmInstall()
            advanceUntilIdle()

            val state = controller.state.value
            assertEquals(listOf("eslav"), installer.installed)
            assertEquals(setOf("eslav"), state.installedPackIds)
            assertNull(state.installPrompt)
            assertEquals(sample, state.result)
            assertEquals(Screen.Result, state.screen)
            assertEquals(Activity.Idle, state.activity)
        }

    @Test
    fun installingFromSettingsDoesNotRecognize() =
        runTest {
            catalog = StubModelCatalog(installed = emptySet())
            val controller = controllerWithImage()
            val hebrew = catalog.packs.first { it.id == "hebrew" }

            controller.requestInstall(hebrew)
            assertEquals(hebrew, controller.state.value.installPrompt?.pack)
            controller.confirmInstall()
            advanceUntilIdle()

            assertEquals(setOf("hebrew"), controller.state.value.installedPackIds)
            assertNull(controller.state.value.installPrompt)
            assertEquals(0, engine.calls)
            assertEquals(Screen.Home, controller.state.value.screen)
        }

    @Test
    fun failedDownloadKeepsTheDialogWithItsMessageAndRetrySucceeds() =
        runTest {
            catalog = StubModelCatalog(installed = emptySet())
            installer.outcomes.add(Outcome.Failure("No network."))
            val controller = controllerWithImage()
            controller.recognize()
            controller.confirmInstall()
            advanceUntilIdle()

            val failed = controller.state.value
            assertEquals("No network.", failed.installPrompt?.error)
            assertEquals(false, failed.installPrompt?.running)
            assertEquals(Activity.Idle, failed.activity)
            assertEquals(emptySet(), failed.installedPackIds)

            controller.confirmInstall()
            advanceUntilIdle()

            assertNull(controller.state.value.installPrompt)
            assertEquals(sample, controller.state.value.result)
        }

    @Test
    fun cancellingTheDownloadAndDismissingTheOfferCloseTheDialog() =
        runTest {
            catalog = StubModelCatalog(installed = emptySet())
            val controller = controllerWithImage()
            controller.recognize()

            controller.dismissInstall()
            assertNull(controller.state.value.installPrompt)

            installer.gate = CompletableDeferred()
            controller.recognize()
            controller.confirmInstall()
            runCurrent()
            assertEquals(Activity.Installing, controller.state.value.activity)
            assertEquals(InstallProgress(bytesDone = 6_815_744, bytesTotal = 13_631_488), controller.state.value.installPrompt?.progress)

            controller.handleBack()
            advanceUntilIdle()

            assertNull(controller.state.value.installPrompt)
            assertEquals(Activity.Idle, controller.state.value.activity)
            assertEquals(emptySet(), controller.state.value.installedPackIds)
            assertEquals(0, engine.calls)
        }

    @Test
    fun pickersListEveryKnownLanguageAndMarkTheOnesThatNeedADownload() =
        runTest {
            catalog = StubModelCatalog(installed = setOf("latin"))
            val state = controller().state.value

            assertEquals(Language.Supported.toSet(), state.languages.toSet())
            assertEquals("German", state.labelOf(Language.German))
            assertEquals("Hebrew (download needed)", state.labelOf(Language.Hebrew))
        }
}

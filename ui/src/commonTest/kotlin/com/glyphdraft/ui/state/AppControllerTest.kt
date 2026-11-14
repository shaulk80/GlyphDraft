package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Language
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.TextSaver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> {
        lastHint = languageHint
        gate?.await()
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {
    private val picker = FakePicker()
    private val engine = FakeEngine()
    private val saver = FakeSaver()

    private fun TestScope.controller() = AppController(this, engine, picker, saver)

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
}

package com.glyphdraft.ui.state

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Settings
import com.glyphdraft.core.Translation
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.Translator
import com.glyphdraft.core.stub.StubModelCatalog
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
import kotlin.test.assertTrue

private const val MB = 1024L * 1024L

private fun modelOf(from: Language, to: Language, megabytes: Long): ModelPack =
    ModelPack("mt-${from.code}-${to.code}", "${from.displayName} to ${to.displayName}", emptySet(), megabytes * MB, TranslationDirection(from, to))

private val englishToGerman = modelOf(Language.English, Language.German, 100)
private val englishToRussian = modelOf(Language.English, Language.Russian, 110)
private val russianToHebrew = modelOf(Language.Russian, Language.Hebrew, 144)

/** Answers with the text marked by the target code, like the stub; waits on [gate] when set. */
internal class FakeTranslator(var failure: String? = null) : Translator {
    var gate: CompletableDeferred<Unit>? = null
    val calls = ArrayList<Triple<String, Language, Language>>()

    override suspend fun translate(text: String, from: Language, to: Language): Outcome<Translation> {
        calls.add(Triple(text, from, to))
        gate?.await()
        val message = failure
        if (message != null) {
            return Outcome.Failure(message)
        }

        return Outcome.Success(Translation("[${to.code}] $text", from, to, listOf(from, to)))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTranslationTest {
    private val picker = FakePicker()
    private val engine = FakeEngine()
    private val translator = FakeTranslator()
    private val saver = FakeSaver()
    private val installer = FakeInstaller { catalog }
    private val settingsStore = FakeSettingsStore()
    private var catalog = catalogWith(installed = setOf(englishToGerman.id))

    private fun catalogWith(installed: Set<String>) =
        StubModelCatalog(StubModelCatalog.defaultPacks() + listOf(englishToGerman, englishToRussian, russianToHebrew), installed + StubModelCatalog.defaultPacks().map { it.id })

    private fun TestScope.controllerWithImage(target: Language?): AppController {
        val controller = AppController(this, engine, translator, picker, saver, catalog, installer, settingsStore)
        controller.pickImage()
        advanceUntilIdle()
        controller.selectTarget(target)
        return controller
    }

    @Test
    fun withoutATargetNothingIsTranslated() =
        runTest {
            val controller = controllerWithImage(target = null)

            controller.recognize()
            advanceUntilIdle()

            assertEquals(sample, controller.state.value.result)
            assertNull(controller.state.value.translation)
            assertEquals(0, translator.calls.size)
        }

    @Test
    fun aTargetWithInstalledModelsTranslatesRightAfterTheRead() =
        runTest {
            val controller = controllerWithImage(Language.German)

            controller.recognize()
            advanceUntilIdle()

            val state = controller.state.value
            assertEquals(listOf(Triple("hello", Language.English, Language.German)), translator.calls)
            assertEquals("[de] hello", state.translation?.text)
            assertEquals(sample, state.result)
            assertEquals(Screen.Result, state.screen)
            assertEquals(Activity.Idle, state.activity)
            assertNull(state.installPrompt)
        }

    @Test
    fun theRunningTextIsTranslatedAndTheLinesOfThePageStayTheReadText() =
        runTest {
            val read = RecognitionResult("one wrapped\nline", Language.English, detected = true, runningText = "one wrapped line")
            engine.outcome = Outcome.Success(read)
            val controller = controllerWithImage(Language.German)

            controller.recognize()
            advanceUntilIdle()
            controller.saveResult()
            advanceUntilIdle()

            assertEquals("one wrapped line", translator.calls.single().first)
            assertEquals("one wrapped\nline", saver.savedText)
        }

    @Test
    fun aReadThatIsNotToBeTrustedOrNeedsNoTranslationIsNotTranslated() =
        runTest {
            val cases =
                listOf(
                    Triple(RecognitionResult("hello", Language.English, detected = true, lowQuality = true), Language.German, null),
                    Triple(RecognitionResult("hallo", Language.German, detected = true), Language.German, "The text is already in German."),
                )

            for ((read, target, notice) in cases) {
                engine.outcome = Outcome.Success(read)
                translator.calls.clear()
                val controller = controllerWithImage(target)

                controller.recognize()
                advanceUntilIdle()

                assertEquals(read, controller.state.value.result, "read $read")
                assertNull(controller.state.value.translation, "read $read")
                assertEquals(0, translator.calls.size, "read $read")
                assertEquals(notice, controller.state.value.notice?.text, "read $read")
            }
        }

    @Test
    fun aMissingModelIsOfferedAfterTheReadAndTheRouteRunsOnceDownloaded() =
        runTest {
            catalog = catalogWith(installed = emptySet())
            val controller = controllerWithImage(Language.Hebrew)
            controller.recognize()
            advanceUntilIdle()

            val offer = controller.state.value.installPrompt
            assertEquals(listOf(englishToRussian.id, russianToHebrew.id), offer?.packs?.map { it.id })
            assertEquals((110 + 144) * MB, offer?.totalBytes)
            assertEquals(AfterInstall.Translate, offer?.then)
            assertEquals(sample, controller.state.value.result)
            assertEquals(0, translator.calls.size)

            controller.confirmInstall()
            advanceUntilIdle()

            val state = controller.state.value
            assertEquals(listOf(englishToRussian.id, russianToHebrew.id), installer.installed)
            assertEquals("[he] hello", state.translation?.text)
            assertNull(state.installPrompt)
            assertEquals(Activity.Idle, state.activity)
        }

    @Test
    fun aFailedDownloadOfARouteIsRetriedWithoutTheModelsAlreadyInstalled() =
        runTest {
            catalog = catalogWith(installed = emptySet())
            installer.outcomes.addAll(listOf(Outcome.Success(Unit), Outcome.Failure("No network.")))
            val controller = controllerWithImage(Language.Hebrew)
            controller.recognize()
            advanceUntilIdle()
            controller.confirmInstall()
            advanceUntilIdle()

            assertEquals("No network.", controller.state.value.installPrompt?.error)
            assertEquals(listOf(englishToRussian.id), installer.installed)

            controller.confirmInstall()
            advanceUntilIdle()

            assertEquals(listOf(englishToRussian.id, russianToHebrew.id), installer.installed)
            assertEquals("[he] hello", controller.state.value.translation?.text)
        }

    @Test
    fun theProgressOfARouteCountsAllItsModels() =
        runTest {
            catalog = catalogWith(installed = emptySet())
            val controller = controllerWithImage(Language.Hebrew)
            controller.recognize()
            advanceUntilIdle()
            installer.gate = CompletableDeferred()

            controller.confirmInstall()
            runCurrent()

            val total = (110 + 144) * MB
            assertEquals(InstallProgress(55 * MB, total), controller.state.value.installPrompt?.progress)

            controller.dismissInstall()
            advanceUntilIdle()
        }

    @Test
    fun aPairWithoutARouteAndAFailedTranslationAreShownAsErrors() =
        runTest {
            val cases =
                listOf(
                    Triple(Language.Japanese, null, "There is no translation from English to Japanese."),
                    Triple(Language.German, "Out of memory.", "Out of memory."),
                )

            for ((target, failure, message) in cases) {
                translator.failure = failure
                val controller = controllerWithImage(target)

                controller.recognize()
                advanceUntilIdle()

                val state = controller.state.value
                assertEquals(Notice(message, isError = true), state.notice, "target $target")
                assertNull(state.translation, "target $target")
                assertEquals(sample, state.result, "target $target")
                assertEquals(Activity.Idle, state.activity, "target $target")
            }
        }

    @Test
    fun translateOnTheResultScreenUsesTheTargetChosenThere() =
        runTest {
            val controller = controllerWithImage(target = null)
            controller.recognize()
            advanceUntilIdle()

            controller.translate()
            advanceUntilIdle()
            assertEquals(0, translator.calls.size)

            controller.selectTarget(Language.German)
            controller.translate()
            advanceUntilIdle()

            assertEquals("[de] hello", controller.state.value.translation?.text)

            controller.selectTarget(Language.Russian)
            controller.translate()
            advanceUntilIdle()

            assertTrue(controller.state.value.installPrompt != null)
            assertNull(controller.state.value.translation)
        }

    @Test
    fun cancellingATranslationKeepsTheReadText() =
        runTest {
            translator.gate = CompletableDeferred()
            val controller = controllerWithImage(Language.German)
            controller.recognize()
            runCurrent()

            assertEquals(Activity.Translating, controller.state.value.activity)
            assertEquals(sample, controller.state.value.result)

            controller.handleBack()
            advanceUntilIdle()

            val state = controller.state.value
            assertEquals(Activity.Idle, state.activity)
            assertEquals(sample, state.result)
            assertNull(state.translation)
            assertEquals(Screen.Result, state.screen)
        }

    @Test
    fun anotherImageClearsTheOldTranslation() =
        runTest {
            val controller = controllerWithImage(Language.German)
            controller.recognize()
            advanceUntilIdle()
            assertNotNull(controller.state.value.translation)

            controller.back()
            controller.pickImage()
            advanceUntilIdle()

            assertNull(controller.state.value.translation)
            assertNull(controller.state.value.result)
        }

    @Test
    fun theDefaultTargetIsStoredAppliedAtStartAndPreselectsTheTarget() =
        runTest {
            settingsStore.stored = Settings(defaultTarget = Language.French)
            val loaded = AppController(this, engine, translator, picker, saver, catalog, installer, settingsStore)
            advanceUntilIdle()

            assertEquals(Language.French, loaded.state.value.target)

            loaded.setDefaultTarget(null)
            advanceUntilIdle()

            assertNull(loaded.state.value.target)
            assertEquals(listOf(Settings()), settingsStore.saves)
        }

    @Test
    fun saveWritesTheTranslationUnderTheNameOfTheTargetOrTheOriginalWhenAskedFor() =
        runTest {
            val controller = controllerWithImage(Language.German)
            controller.recognize()
            advanceUntilIdle()
            val name = photo.name.substringBeforeLast('.')

            controller.saveResult(translated = true)
            advanceUntilIdle()
            assertEquals("$name.de.txt", saver.savedName)
            assertEquals("[de] hello", saver.savedText)

            controller.saveResult()
            advanceUntilIdle()
            assertEquals("$name.txt", saver.savedName)
            assertEquals("hello", saver.savedText)
        }

    @Test
    fun saveOfATranslationThatDoesNotExistSavesTheRecognisedText() =
        runTest {
            val controller = controllerWithImage(target = null)
            controller.recognize()
            advanceUntilIdle()

            controller.saveResult(translated = true)
            advanceUntilIdle()

            assertEquals("hello", saver.savedText)
            assertEquals(photo.name.substringBeforeLast('.') + ".txt", saver.savedName)
        }
}

package com.glyphdraft.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Language
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.TextSaver
import com.glyphdraft.core.stub.InMemorySettingsStore
import com.glyphdraft.core.stub.StubModelCatalog
import com.glyphdraft.core.stub.StubModelInstaller
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.core.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val WAIT_MILLIS = 5_000L

private class FakePicker(var outcome: Outcome<ImageInput?>) : ImagePicker {
    override suspend fun pick(): Outcome<ImageInput?> = outcome
}

private class FakeSaver : TextSaver {
    var savedName: String? = null
    var savedText: String? = null

    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> {
        savedName = suggestedName
        savedText = text
        return Outcome.Success(true)
    }
}

private class FixedEngine(var result: RecognitionResult) : OcrEngine {
    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> = Outcome.Success(result)
}

/** In-memory clipboard so the test never touches the system clipboard (there may be no display on CI). */
@OptIn(ExperimentalComposeUiApi::class)
private class FakeClipboard : Clipboard {
    var entry: ClipEntry? = null

    @Suppress("OVERRIDE_DEPRECATION")
    override val nativeClipboard = java.awt.datatransfer.Clipboard("test")

    override suspend fun getClipEntry(): ClipEntry? = entry

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        entry = clipEntry
    }

    fun text(): String? {
        val transferable = entry?.nativeClipEntry as? Transferable
        return transferable?.getTransferData(DataFlavor.stringFlavor) as? String
    }
}

/** Drives the real screens with fake platform parts and the stub engine, the way the owner clicks through them. */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class ClickThroughTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val image = ImageInput("scan.png", byteArrayOf(1, 2, 3))
    private val picker = FakePicker(Outcome.Success(image))
    private val saver = FakeSaver()
    private val clipboard = FakeClipboard()
    private val catalog = StubModelCatalog()
    private val controller =
        AppController(scope, StubOcrEngine(latencyMillis = 0), picker, saver, catalog, StubModelInstaller(catalog, steps = 2, stepMillis = 0), InMemorySettingsStore())

    private fun ComposeUiTest.showApp(shown: AppController = controller) {
        setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                App(shown, "test")
            }
        }
    }

    private fun ComposeUiTest.waitForText(text: String, substring: Boolean = false) {
        waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Clicks the clickable element showing [text]; a label of the same text elsewhere (a pack row) does not count. */
    private fun ComposeUiTest.click(text: String) {
        onNode(hasText(text) and hasClickAction()).performClick()
    }

    @Test
    fun pickChooseLanguageRecognizeCopySaveAndGoBack() =
        runComposeUiTest {
            showApp()
            onNodeWithText("No image chosen yet.").assertExists()
            onNodeWithText("Recognize text").assertIsNotEnabled()

            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            onNodeWithText("Recognize text").assertIsEnabled()

            click("Auto-detect")
            click("Russian")
            waitForText("Russian")
            click("Recognize text")
            waitForText("Language: Russian (chosen)")
            onNodeWithText("Sample text from scan.png.", substring = true).assertExists()

            click("Copy")
            waitForText("Copied to the clipboard.")
            assertNotNull(clipboard.entry)
            assertEquals("Sample text from scan.png.\nThis is fixed output of the stub engine; real recognition arrives in M2.", clipboard.text())

            click("Save as…")
            waitForText("Saved.")
            assertEquals("scan.txt", saver.savedName)
            assertEquals(clipboard.text(), saver.savedText)

            click("← Back")
            waitForText("scan.png (3 bytes)")
            onNodeWithText("Recognize text").assertIsEnabled()
        }

    @Test
    fun autoDetectShowsDetectedLanguage() =
        runComposeUiTest {
            showApp()

            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            click("Recognize text")

            waitForText("Language: English (detected)")
        }

    @Test
    fun firstUseOffersTheDownloadThenReadsAndSettingsListsThePacks() =
        runComposeUiTest {
            val empty = StubModelCatalog(installed = emptySet())
            val firstUse =
                AppController(scope, StubOcrEngine(latencyMillis = 0), picker, saver, empty, StubModelInstaller(empty, steps = 2, stepMillis = 0), InMemorySettingsStore())
            showApp(firstUse)

            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            click("Auto-detect")
            waitForText("Russian (download needed)")
            click("Russian (download needed)")
            click("Recognize text")
            waitForText("Download language models?")
            waitForText("Russian models", substring = true)

            click("Cancel")
            waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText("Download language models?")).fetchSemanticsNodes().isEmpty() }
            onNodeWithText("Recognize text").assertIsEnabled()

            click("Recognize text")
            waitForText("Download language models?")
            click("Download")
            waitForText("Language: Russian (chosen)")
            assertEquals(setOf("eslav"), firstUse.state.value.installedPackIds)

            click("← Back")
            click("Settings")
            waitForText("Language models")
            waitForText("Download 2.0 MB")
        }

    @Test
    fun aDoubtedReadingShowsTheHintAboveTheTextAndAGoodOneDoesNot() =
        runComposeUiTest {
            val engine = FixedEngine(RecognitionResult(", . 4", Language.English, detected = true, lowQuality = true))
            val doubting = AppController(scope, engine, picker, saver, catalog, StubModelInstaller(catalog, steps = 2, stepMillis = 0), InMemorySettingsStore())
            showApp(doubting)

            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            click("Recognize text")
            waitForText("Language: English (detected)")
            onNodeWithText(LOW_QUALITY_HINT).assertExists()
            onNodeWithText(", . 4").assertExists()
            onNodeWithText("Copy").assertIsEnabled()

            click("← Back")
            engine.result = RecognitionResult("Hello world", Language.English, detected = true)
            click("Recognize text")
            waitForText("Hello world")
            onNodeWithText(LOW_QUALITY_HINT).assertDoesNotExist()
        }

    @Test
    fun selectedResultTextCopiesWithCtrlCAndCtrlInsert() =
        runComposeUiTest {
            val engine = FixedEngine(RecognitionResult("Hello world", Language.English, detected = true))
            val reading = AppController(scope, engine, picker, saver, catalog, StubModelInstaller(catalog, steps = 2, stepMillis = 0), InMemorySettingsStore())
            showApp(reading)
            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            click("Recognize text")
            waitForText("Hello world")

            for (copyKey in listOf(Key.C, Key.Insert)) {
                clipboard.entry = null
                onNodeWithText("Hello world").performTextInputSelection(TextRange(0, "Hello world".length))
                onNodeWithText("Hello world").performKeyInput {
                    keyDown(Key.CtrlLeft)
                    pressKey(copyKey)
                    keyUp(Key.CtrlLeft)
                }
                waitUntil(timeoutMillis = WAIT_MILLIS) { clipboard.entry != null }
                assertEquals("Hello world", clipboard.text(), "Ctrl+${copyKey}")
            }
        }

    /** Hand test, finding 14: Hebrew was laid out left to right, so it sat at the left edge with the full stop on the right. */
    @Test
    fun hebrewResultTextIsLaidOutRightToLeftAndAlignedRight() = checkResultTextLayout("שלום עולם.\nשלום.", ResolvedTextDirection.Rtl)

    @Test
    fun englishResultTextIsLaidOutLeftToRightAndAlignedLeft() = checkResultTextLayout("Hello world.\nHello.", ResolvedTextDirection.Ltr)

    private fun checkResultTextLayout(text: String, direction: ResolvedTextDirection) =
        runComposeUiTest {
            val engine = FixedEngine(RecognitionResult(text, Language.English, detected = true))
            val reading = AppController(scope, engine, picker, saver, catalog, StubModelInstaller(catalog, steps = 2, stepMillis = 0), InMemorySettingsStore())
            showApp(reading)
            click("Choose image…")
            waitForText("scan.png (3 bytes)")
            click("Recognize text")
            waitForText("Language: English (detected)")

            val layout = resultTextLayout(text)
            for (line in 0 until layout.lineCount) {
                assertEquals(direction, layout.getParagraphDirection(layout.getLineStart(line)), "line $line of $text")
            }
            val atRightEdge = layout.getLineRight(0) >= layout.size.width - 1f
            assertEquals(direction == ResolvedTextDirection.Rtl, atRightEdge, "first line of $text at the right edge")
        }

    private fun ComposeUiTest.resultTextLayout(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        val node = onNodeWithText(text).fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.first()
    }

    @Test
    fun pickerErrorIsShownAndCanBeDismissed() =
        runComposeUiTest {
            picker.outcome = Outcome.Failure("Cannot read scan.png: access denied")
            showApp()

            click("Choose image…")
            waitForText("Cannot read scan.png: access denied")
            onNodeWithText("Recognize text").assertIsNotEnabled()

            click("Dismiss")
            waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText("Cannot read", substring = true)).fetchSemanticsNodes().isEmpty() }
        }

    @Test
    fun settingsChangeThemeAndDefaultLanguageThatHomeShows() =
        runComposeUiTest {
            showApp()

            click("Settings")
            click("Dark")
            click("Auto-detect")
            click("Hebrew")
            click("← Back")

            waitForText("Hebrew")
            assertEquals(ThemeMode.Dark, controller.state.value.settings.theme)
            assertEquals("Hebrew", controller.state.value.language?.displayName)
        }
}

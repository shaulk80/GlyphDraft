package com.glyphdraft.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TextSaver
import com.glyphdraft.core.stub.StubOcrEngine
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.ThemeMode
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
    private val controller = AppController(scope, StubOcrEngine(latencyMillis = 0), picker, saver)

    private fun ComposeUiTest.showApp() {
        setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                App(controller, "test")
            }
        }
    }

    private fun ComposeUiTest.waitForText(text: String, substring: Boolean = false) {
        waitUntil(timeoutMillis = WAIT_MILLIS) { onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.click(text: String) {
        onNodeWithText(text).performClick()
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

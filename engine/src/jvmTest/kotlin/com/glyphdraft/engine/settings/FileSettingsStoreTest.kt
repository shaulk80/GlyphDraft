package com.glyphdraft.engine.settings

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Settings
import com.glyphdraft.core.ThemeMode
import com.glyphdraft.engine.log.FileLog
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest

class FileSettingsStoreTest {
    private val folder: Path = Files.createTempDirectory("glyphdraft-settings")
    private val file = folder.resolve("settings.properties")
    private val languages = listOf(Language.English, Language.Hebrew, Language.Russian)

    private fun store(target: Path = file) = FileSettingsStore(target, languages, FileLog(folder.resolve("log.txt")), Dispatchers.Unconfined)

    @Test
    fun savedSettingsComeBackUnchanged() =
        runTest {
            val cases =
                listOf(
                    Settings(),
                    Settings(ThemeMode.Dark, null),
                    Settings(ThemeMode.Light, Language.Hebrew),
                    Settings(ThemeMode.System, Language.Russian),
                )

            for (settings in cases) {
                assertTrue(store().save(settings) is Outcome.Success)
                assertEquals(settings, store().load(), "settings $settings")
            }
        }

    @Test
    fun missingUnknownOrBrokenContentGivesTheDefaultsForThatPart() =
        runTest {
            val cases =
                mapOf<String?, Settings>(
                    null to Settings(),
                    "" to Settings(),
                    "theme=Dark\ndefaultLanguage=xx\nfutureKey=1\n" to Settings(ThemeMode.Dark, null),
                    "theme=Purple\ndefaultLanguage=he\n" to Settings(ThemeMode.System, Language.Hebrew),
                    "theme=Light\ndefaultLanguage=\\uZZZZ\n" to Settings(),
                )

            for ((content, expected) in cases) {
                Files.deleteIfExists(file)
                if (content != null) {
                    Files.write(file, content.toByteArray(Charsets.UTF_8))
                }

                assertEquals(expected, store().load(), "content $content")
            }
        }

    @Test
    fun aFailedSaveIsReportedAndLeavesNoHalfWrittenFile() =
        runTest {
            val blocker = folder.resolve("not-a-folder")
            Files.write(blocker, byteArrayOf(1))

            val outcome = store(blocker.resolve("settings.properties")).save(Settings(ThemeMode.Dark, null))

            assertTrue(outcome is Outcome.Failure)
            assertEquals(listOf("log.txt", "not-a-folder"), Files.list(folder).use { names -> names.map { it.fileName.toString() }.sorted().toList() })
        }

    @Test
    fun aSecondSaveReplacesTheFirst() =
        runTest {
            store().save(Settings(ThemeMode.Dark, Language.English))
            store().save(Settings(ThemeMode.Light, null))

            assertEquals(Settings(ThemeMode.Light, null), store().load())
            assertTrue(Files.notExists(folder.resolve("settings.properties.tmp")))
        }
}

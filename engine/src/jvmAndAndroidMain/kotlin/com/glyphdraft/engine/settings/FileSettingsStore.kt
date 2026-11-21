package com.glyphdraft.engine.settings

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Settings
import com.glyphdraft.core.SettingsStore
import com.glyphdraft.core.ThemeMode
import com.glyphdraft.engine.log.FileLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [Settings] as a small properties file in the data folder (`settings.properties`). Unknown keys and values are
 * ignored so that a file from another version still loads; a language code that [languages] does not know counts as
 * "auto-detect". The file is written next to itself first and then moved over the old one, so a crash during a save
 * leaves the previous settings intact.
 */
class FileSettingsStore(
    private val file: Path,
    private val languages: List<Language>,
    private val log: FileLog,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsStore {
    override suspend fun load(): Settings =
        withContext(dispatcher) {
            if (!Files.isRegularFile(file)) {
                return@withContext Settings()
            }

            try {
                parse(Files.readAllBytes(file))
            } catch (e: IOException) {
                log.error("Cannot read $file, using the default settings", e)
                Settings()
            } catch (e: IllegalArgumentException) {
                log.error("Cannot parse $file, using the default settings", e)
                Settings()
            }
        }

    override suspend fun save(settings: Settings): Outcome<Unit> =
        withContext(dispatcher) {
            val temporary = file.resolveSibling(file.fileName.toString() + ".tmp")
            try {
                Files.createDirectories(file.parent)
                Files.write(temporary, serialize(settings))
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
                Outcome.Success(Unit)
            } catch (e: IOException) {
                log.error("Cannot write $file", e)
                Outcome.Failure("Cannot save the settings: ${e.message}", e)
            }
        }

    private fun parse(bytes: ByteArray): Settings {
        val properties = Properties()
        properties.load(bytes.inputStream().reader(Charsets.UTF_8))
        val theme = ThemeMode.entries.firstOrNull { it.name == properties.getProperty(THEME) } ?: ThemeMode.System
        val code = properties.getProperty(DEFAULT_LANGUAGE)?.trim()
        return Settings(theme = theme, defaultLanguage = languages.firstOrNull { it.code == code })
    }

    private fun serialize(settings: Settings): ByteArray {
        val properties = Properties()
        properties.setProperty(THEME, settings.theme.name)
        settings.defaultLanguage?.let { properties.setProperty(DEFAULT_LANGUAGE, it.code) }
        val output = ByteArrayOutputStream()
        output.writer(Charsets.UTF_8).use { properties.store(it, null) }
        return output.toByteArray()
    }

    private companion object {
        const val THEME = "theme"
        const val DEFAULT_LANGUAGE = "defaultLanguage"
    }
}

package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TextSaver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Lets the user choose where to save the text and writes it as UTF-8. The chosen file may be anywhere:
 * the "only the data folder" rule is about what the app writes on its own.
 */
class DesktopTextSaver(
    private val chooser: FileChooser,
    private val log: FileLog,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TextSaver {
    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> =
        withContext(dispatcher) {
            when (val chosen = chooser.chooseToSave("Save text", suggestedName)) {
                is Outcome.Failure -> chosen
                is Outcome.Success -> writeIfChosen(chosen.value, text)
            }
        }

    private fun writeIfChosen(path: Path?, text: String): Outcome<Boolean> {
        if (path == null) {
            return Outcome.Success(false)
        }

        return write(path, text)
    }

    private fun write(path: Path, text: String): Outcome<Boolean> {
        try {
            Files.writeString(path, text)
            return Outcome.Success(true)
        } catch (e: IOException) {
            log.error("Cannot write $path", e)
            return Outcome.Failure("Cannot save ${path.fileName}: ${e.message}", e)
        }
    }
}

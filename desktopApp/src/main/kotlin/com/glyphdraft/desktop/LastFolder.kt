package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

/** Remembers the folder of the last file the user chose, in one small file in the data folder. */
class LastFolder(private val file: Path, private val log: FileLog) {
    /** The remembered folder, or null when there is none, the file is unreadable or the folder no longer exists. */
    fun load(): Path? {
        if (!Files.isRegularFile(file)) {
            return null
        }

        try {
            val folder = Path.of(Files.readString(file).trim())
            return folder.takeIf { Files.isDirectory(it) }
        } catch (e: IOException) {
            log.error("Cannot read $file", e)
            return null
        } catch (e: InvalidPathException) {
            log.error("Invalid folder in $file", e)
            return null
        }
    }

    fun save(folder: Path): Outcome<Unit> {
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, folder.toString())
            return Outcome.Success(Unit)
        } catch (e: IOException) {
            log.error("Cannot write $file", e)
            return Outcome.Failure("Cannot remember the folder: ${e.message}", e)
        }
    }
}

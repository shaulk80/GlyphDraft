package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

/** Minimal append-only log file. The log folder is created on first write. */
class FileLog(private val file: Path) {
    fun info(message: String): Outcome<Unit> =
        try {
            Files.createDirectories(file.parent)
            Files.writeString(
                file,
                "${Instant.now()} INFO $message\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure("Cannot write log file $file: ${e.message}", e)
        }
}

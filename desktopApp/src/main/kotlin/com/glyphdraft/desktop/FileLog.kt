package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

/** Minimal append-only log file. The log folder is created on first write. */
class FileLog(private val file: Path) {
    fun info(message: String): Outcome<Unit> = append("INFO", message)

    /** Logs [message] with the exception's type and message; the stack trace is kept too. */
    fun error(message: String, cause: Throwable): Outcome<Unit> = append("ERROR", "$message: ${cause.stackTraceToString()}")

    private fun append(level: String, message: String): Outcome<Unit> =
        try {
            Files.createDirectories(file.parent)
            Files.writeString(
                file,
                "${Instant.now()} $level $message\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure("Cannot write log file $file: ${e.message}", e)
        }
}

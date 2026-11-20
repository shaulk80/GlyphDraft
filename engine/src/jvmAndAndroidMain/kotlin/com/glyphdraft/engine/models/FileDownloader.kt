package com.glyphdraft.engine.models

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Downloads one [ModelFile] to its place on disk. The bytes go to `<target>.part`, are hashed on the way, and the file
 * gets its final name by an atomic rename only when size and SHA-256 match; otherwise the partial file is deleted. A
 * download that was interrupted starts again from zero (the files are at most 17 MB). `HttpURLConnection` is used
 * because it is the same on the JVM and on Android and adds no dependency.
 */
class FileDownloader(
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
) {
    /**
     * Expected failures come back as [Outcome.Failure]. Cancelling the calling coroutine closes the connection,
     * deletes the partial file and rethrows the cancellation. [onBytes] receives the number of bytes of this file
     * received so far.
     */
    suspend fun download(file: ModelFile, target: Path, onBytes: (Long) -> Unit): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            val partial = target.resolveSibling("${target.fileName}$PARTIAL_SUFFIX")
            try {
                val outcome = fetch(file, partial, onBytes)
                if (outcome is Outcome.Failure) {
                    return@withContext outcome
                }

                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                Outcome.Success(Unit)
            } catch (e: IOException) {
                // A closed connection after a cancel shows up as an IOException; the cancellation wins.
                coroutineContext.ensureActive()
                Outcome.Failure(describe(e, file), e)
            } finally {
                Files.deleteIfExists(partial)
            }
        }

    private suspend fun fetch(file: ModelFile, partial: Path, onBytes: (Long) -> Unit): Outcome<Unit> =
        coroutineScope {
            Files.createDirectories(partial.parent)
            val connection = URI.create(file.url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.setRequestProperty("User-Agent", USER_AGENT)
            // A blocked read does not see the cancellation; closing the connection from here makes it fail at once.
            val closeOnCancel = launch { closeWhenCancelled(connection) }
            try {
                download(connection, file, partial, onBytes)
            } finally {
                closeOnCancel.cancel()
                connection.disconnect()
            }
        }

    private suspend fun closeWhenCancelled(connection: HttpURLConnection) {
        try {
            awaitCancellation()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun download(connection: HttpURLConnection, file: ModelFile, partial: Path, onBytes: (Long) -> Unit): Outcome<Unit> {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return Outcome.Failure("The download server answered HTTP ${connection.responseCode} for ${file.path}.")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val received = connection.inputStream.use { copy(it, partial, digest, file.sizeBytes, onBytes) }
        return verify(file, received, digest)
    }

    /** Returns the number of bytes written; stops reading shortly after [limit] is passed, so a wrong file cannot fill the disk. */
    private suspend fun copy(input: InputStream, partial: Path, digest: MessageDigest, limit: Long, onBytes: (Long) -> Unit): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var received = 0L
        Files.newOutputStream(partial).use { output ->
            while (received <= limit) {
                coroutineContext.ensureActive()
                val count = input.read(buffer)
                if (count < 0) {
                    break
                }

                output.write(buffer, 0, count)
                digest.update(buffer, 0, count)
                received += count
                onBytes(received)
            }
        }

        return received
    }

    private fun verify(file: ModelFile, received: Long, digest: MessageDigest): Outcome<Unit> {
        if (received > file.sizeBytes) {
            return Outcome.Failure("${file.path} is larger than expected (${file.sizeBytes} bytes); it was discarded.")
        }

        if (received < file.sizeBytes) {
            return Outcome.Failure("${file.path} is incomplete (got $received of ${file.sizeBytes} bytes); it was discarded.")
        }

        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != file.sha256) {
            return Outcome.Failure("${file.path} does not match its checksum; it was discarded.")
        }

        return Outcome.Success(Unit)
    }

    private fun describe(e: IOException, file: ModelFile): String =
        when (e) {
            is UnknownHostException, is NoRouteToHostException, is SocketTimeoutException, is ConnectException ->
                "Cannot reach the download server. Check the network connection and try again."
            else -> "Downloading ${file.path} failed: ${e.message}"
        }

    companion object {
        const val PARTIAL_SUFFIX = ".part"
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 30_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val USER_AGENT = "GlyphDraft"
    }
}

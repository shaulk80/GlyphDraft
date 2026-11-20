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
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive

/**
 * Downloads one [ModelFile] to its place on disk. The bytes go to `<target>.part-<n>`, are hashed on the way, and the
 * file gets its final name by an atomic rename only when size and SHA-256 match; otherwise the partial file is
 * deleted. A download that was interrupted starts again from zero (the files are at most 17 MB). `HttpURLConnection`
 * is used because it is the same on the JVM and on Android and adds no dependency.
 *
 * The transfer runs in a coroutine of its own, not in the caller's. A blocked socket read cannot be interrupted
 * (`HttpURLConnection.disconnect()` waits for the read on the JDK), so if the caller were waiting inside the read, a
 * cancel on a stalled network would hang until the read timeout. Instead the caller is released at once and the
 * transfer ends by itself at its next chunk or at the read timeout, and deletes its partial file then. Each attempt has
 * its own partial file name, so a transfer that is still ending cannot disturb the next one.
 */
class FileDownloader(
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
) {
    private val transfers = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val attempts = AtomicLong()

    /**
     * Expected failures come back as [Outcome.Failure]. Cancelling the calling coroutine returns at once (by
     * rethrowing the cancellation) and stops the transfer, which removes its partial file as soon as it ends.
     * [onBytes] receives the number of bytes of this file received so far; it is not called after a cancel except
     * possibly once for a chunk that was already being handled.
     */
    suspend fun download(file: ModelFile, target: Path, onBytes: (Long) -> Unit): Outcome<Unit> {
        val transfer = transfers.async { transfer(file, target, onBytes) }
        try {
            return transfer.await()
        } catch (e: CancellationException) {
            transfer.cancel()
            throw e
        }
    }

    private suspend fun transfer(file: ModelFile, target: Path, onBytes: (Long) -> Unit): Outcome<Unit> {
        val partial = target.resolveSibling("${target.fileName}$PARTIAL_SUFFIX-${attempts.incrementAndGet()}")
        try {
            Files.createDirectories(partial.parent)
            deleteStalePartials(target)
            val outcome = fetch(file, partial, onBytes)
            if (outcome is Outcome.Failure) {
                return outcome
            }

            coroutineContext.ensureActive()
            Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return Outcome.Success(Unit)
        } catch (e: IOException) {
            // A failure after a cancel is not worth reporting; the cancellation wins.
            coroutineContext.ensureActive()
            return Outcome.Failure(describe(e, file), e)
        } finally {
            Files.deleteIfExists(partial)
        }
    }

    /** Partial files of earlier attempts that ended without cleaning up (the app was killed). */
    private fun deleteStalePartials(target: Path) {
        Files.newDirectoryStream(target.parent, "${target.fileName}$PARTIAL_SUFFIX-*").use { stale ->
            for (path in stale) {
                Files.deleteIfExists(path)
            }
        }
    }

    private suspend fun fetch(file: ModelFile, partial: Path, onBytes: (Long) -> Unit): Outcome<Unit> {
        val connection = URI.create(file.url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return Outcome.Failure("The download server answered HTTP ${connection.responseCode} for ${file.path}.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val received = connection.inputStream.use { copy(it, partial, digest, file.sizeBytes, onBytes) }
            return verify(file, received, digest)
        } finally {
            connection.disconnect()
        }
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

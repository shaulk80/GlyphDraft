package com.glyphdraft.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.ResultBridge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Opens the system document picker for images and reads the chosen one into memory (off the main thread). */
class AndroidImagePicker(
    private val contentResolver: ContentResolver,
    private val bridge: ResultBridge<String, Uri>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImagePicker {
    override suspend fun pick(): Outcome<ImageInput?> =
        when (val chosen = bridge.request(IMAGE_MIME_TYPE)) {
            is Outcome.Failure -> chosen
            is Outcome.Success -> readIfChosen(chosen.value)
        }

    private suspend fun readIfChosen(uri: Uri?): Outcome<ImageInput?> {
        if (uri == null) {
            return Outcome.Success(null)
        }

        return withContext(dispatcher) { read(uri) }
    }

    private fun read(uri: Uri): Outcome<ImageInput?> {
        try {
            val (name, size) = describe(uri)
            if (size != null && size > ImageInput.MAX_BYTES) {
                return Outcome.Failure(ImageInput.tooLargeMessage(name, size))
            }

            val stream = contentResolver.openInputStream(uri)
            if (stream == null) {
                return Outcome.Failure("Cannot open $name.")
            }

            val bytes = stream.use { readLimited(it) }
            if (bytes == null) {
                return Outcome.Failure(ImageInput.tooLargeMessage(name, ImageInput.MAX_BYTES + 1))
            }

            return Outcome.Success(ImageInput(name, bytes))
        } catch (e: IOException) {
            return failed(uri, e)
        } catch (e: SecurityException) {
            return failed(uri, e)
        }
    }

    private fun failed(uri: Uri, cause: Exception): Outcome.Failure {
        Log.e(TAG, "Cannot read image $uri", cause)
        return Outcome.Failure("Cannot read the image: ${cause.message}", cause)
    }

    /** File name and size as the provider reports them; either may be missing. */
    private fun describe(uri: Uri): Pair<String, Long?> {
        var name: String? = null
        var size: Long? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
                size = if (cursor.isNull(1)) null else cursor.getLong(1)
            }
        }
        return Pair(name ?: uri.lastPathSegment ?: DEFAULT_NAME, size)
    }

    /** Reads the whole stream, or returns null as soon as it is larger than the limit (the size may be unknown up front). */
    private fun readLimited(stream: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) {
                return output.toByteArray()
            }

            output.write(buffer, 0, count)
            if (output.size() > ImageInput.MAX_BYTES) {
                return null
            }
        }
    }

    private companion object {
        const val TAG = "GlyphDraft"
        const val IMAGE_MIME_TYPE = "image/*"
        const val DEFAULT_NAME = "image"
        const val BUFFER_BYTES = 64 * 1024
    }
}

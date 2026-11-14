package com.glyphdraft.app

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.ResultBridge
import com.glyphdraft.core.TextSaver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Opens the system "create document" screen and writes the text to the chosen file as UTF-8. */
class AndroidTextSaver(
    private val contentResolver: ContentResolver,
    private val bridge: ResultBridge<String, Uri>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TextSaver {
    override suspend fun save(suggestedName: String, text: String): Outcome<Boolean> =
        when (val chosen = bridge.request(suggestedName)) {
            is Outcome.Failure -> chosen
            is Outcome.Success -> writeIfChosen(chosen.value, text)
        }

    private suspend fun writeIfChosen(uri: Uri?, text: String): Outcome<Boolean> {
        if (uri == null) {
            return Outcome.Success(false)
        }

        return withContext(dispatcher) { write(uri, text) }
    }

    private fun write(uri: Uri, text: String): Outcome<Boolean> {
        try {
            val stream = contentResolver.openOutputStream(uri, "wt")
            if (stream == null) {
                return Outcome.Failure("Cannot open the file for writing.")
            }

            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            return Outcome.Success(true)
        } catch (e: IOException) {
            return failed(uri, e)
        } catch (e: SecurityException) {
            return failed(uri, e)
        }
    }

    private fun failed(uri: Uri, cause: Exception): Outcome.Failure {
        Log.e(TAG, "Cannot write $uri", cause)
        return Outcome.Failure("Cannot save the file: ${cause.message}", cause)
    }

    private companion object {
        const val TAG = "GlyphDraft"
    }
}

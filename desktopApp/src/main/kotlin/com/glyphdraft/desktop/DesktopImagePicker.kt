package com.glyphdraft.desktop

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.ImagePicker
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** Lets the user choose an image file and reads it into memory. The dialog and the read run on [dispatcher]. */
class DesktopImagePicker(
    private val chooser: FileChooser,
    private val log: FileLog,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImagePicker {
    override suspend fun pick(): Outcome<ImageInput?> =
        withContext(dispatcher) {
            val path = chooser.chooseToOpen("Choose an image", IMAGE_EXTENSIONS)
            if (path == null) {
                Outcome.Success(null)
            } else {
                read(path)
            }
        }

    private fun read(path: Path): Outcome<ImageInput?> {
        try {
            val size = Files.size(path)
            if (size > MAX_IMAGE_BYTES) {
                return Outcome.Failure("The file ${path.fileName} is too large (${size / BYTES_PER_MB} MB; the limit is ${MAX_IMAGE_BYTES / BYTES_PER_MB} MB).")
            }

            return Outcome.Success(ImageInput(path.fileName.toString(), Files.readAllBytes(path)))
        } catch (e: IOException) {
            log.error("Cannot read image $path", e)
            return Outcome.Failure("Cannot read ${path.fileName}: ${e.message}", e)
        }
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "bmp", "gif", "webp", "tif", "tiff")
        const val BYTES_PER_MB = 1024L * 1024L
        const val MAX_IMAGE_BYTES = 50L * BYTES_PER_MB
    }
}

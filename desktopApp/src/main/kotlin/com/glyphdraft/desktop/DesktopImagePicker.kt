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
            when (val chosen = chooser.chooseToOpen("Choose an image", IMAGE_EXTENSIONS)) {
                is Outcome.Failure -> chosen
                is Outcome.Success -> readIfChosen(chosen.value)
            }
        }

    private fun readIfChosen(path: Path?): Outcome<ImageInput?> {
        if (path == null) {
            return Outcome.Success(null)
        }

        return read(path)
    }

    private fun read(path: Path): Outcome<ImageInput?> {
        try {
            val size = Files.size(path)
            if (size > ImageInput.MAX_BYTES) {
                return Outcome.Failure(ImageInput.tooLargeMessage(path.fileName.toString(), size))
            }

            return Outcome.Success(ImageInput(path.fileName.toString(), Files.readAllBytes(path)))
        } catch (e: IOException) {
            log.error("Cannot read image $path", e)
            return Outcome.Failure("Cannot read ${path.fileName}: ${e.message}", e)
        }
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "bmp", "gif", "webp", "tif", "tiff")
    }
}

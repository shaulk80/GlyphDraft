package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO

/** [ImageDecoder] for the desktop: Java's ImageIO (PNG, JPEG, GIF, BMP). Transparent pixels are put on white. */
class AwtImageDecoder : ImageDecoder {
    override fun decode(bytes: ByteArray): Outcome<PixelImage> =
        try {
            decodeOrFailure(bytes)
        } catch (e: IOException) {
            Outcome.Failure("The image could not be read: ${e.message}", e)
        }

    private fun decodeOrFailure(bytes: ByteArray): Outcome<PixelImage> {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull()
            if (reader == null) {
                return Outcome.Failure("The file is not an image this app can read (it reads PNG, JPEG, GIF and BMP).")
            }

            try {
                reader.input = stream
                if (reader.getWidth(0).toLong() * reader.getHeight(0) > PixelImage.MAX_PIXELS) {
                    return Outcome.Failure("The image is too large (more than ${PixelImage.MAX_PIXELS / MEGAPIXEL} megapixels).")
                }

                val image = reader.read(0)
                val rgb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
                return Outcome.Success(PixelImage(image.width, image.height, IntArray(rgb.size) { PixelImage.onWhite(rgb[it]) }))
            } finally {
                reader.dispose()
            }
        }
    }

    private companion object {
        const val MEGAPIXEL = 1024L * 1024L
    }
}

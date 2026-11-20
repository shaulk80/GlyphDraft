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
                return Outcome.Success(PixelImage(image.width, image.height, IntArray(rgb.size) { onWhite(rgb[it]) }))
            } finally {
                reader.dispose()
            }
        }
    }

    /** Blends an ARGB pixel with white by its alpha; the result is plain RGB. */
    private fun onWhite(argb: Int): Int {
        val alpha = argb ushr 24
        if (alpha == OPAQUE) {
            return argb and PixelImage.WHITE
        }

        val red = blend((argb shr 16) and 0xFF, alpha)
        val green = blend((argb shr 8) and 0xFF, alpha)
        val blue = blend(argb and 0xFF, alpha)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun blend(channel: Int, alpha: Int): Int = (channel * alpha + WHITE_CHANNEL * (OPAQUE - alpha)) / OPAQUE

    private companion object {
        const val OPAQUE = 255
        const val WHITE_CHANNEL = 255
        const val MEGAPIXEL = 1024L * 1024L
    }
}

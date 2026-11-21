package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome

/**
 * A decoded image: [pixels] row by row, one `0xRRGGBB` value each (no alpha; the decoder has already put the image
 * on white). The recognition pipeline works on this type only, so it has no platform imaging code in it.
 */
class PixelImage(val width: Int, val height: Int, val pixels: IntArray) {
    fun red(index: Int): Int = (pixels[index] shr 16) and 0xFF

    fun green(index: Int): Int = (pixels[index] shr 8) and 0xFF

    fun blue(index: Int): Int = pixels[index] and 0xFF

    companion object {
        /** Largest image the pipeline accepts, in pixels (a 64 MP photo is 256 MB as ints). */
        const val MAX_PIXELS = 64L * 1024L * 1024L

        const val WHITE = 0xFFFFFF

        private const val OPAQUE = 255
        private const val WHITE_CHANNEL = 255

        /** Blends an `0xAARRGGBB` pixel with white by its alpha; the result is plain `0xRRGGBB`. */
        fun onWhite(argb: Int): Int {
            val alpha = argb ushr 24
            if (alpha == OPAQUE) {
                return argb and WHITE
            }

            val red = blend((argb shr 16) and 0xFF, alpha)
            val green = blend((argb shr 8) and 0xFF, alpha)
            val blue = blend(argb and 0xFF, alpha)
            return (red shl 16) or (green shl 8) or blue
        }

        private fun blend(channel: Int, alpha: Int): Int = (channel * alpha + WHITE_CHANNEL * (OPAQUE - alpha)) / OPAQUE
    }
}

/** Turns encoded bytes (PNG, JPEG, ...) into a [PixelImage]; the JVM and Android have one implementation each. */
interface ImageDecoder {
    /** A [Outcome.Failure] with a message for the user when the bytes are not an image this platform can read. */
    fun decode(bytes: ByteArray): Outcome<PixelImage>
}

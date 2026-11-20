package com.glyphdraft.engine.ocr

import kotlin.math.abs
import kotlin.math.tan

/** Small images and maps for the tests, built in code. */
internal object TestImages {
    /** A [width] by [height] image whose pixel at x, y has red = 10 * x and green = 10 * y. */
    fun gradient(width: Int, height: Int): PixelImage =
        PixelImage(width, height, IntArray(width * height) { (((it % width) * 10) shl 16) or (((it / width) * 10) shl 8) })

    fun solid(width: Int, height: Int, color: Int): PixelImage = PixelImage(width, height, IntArray(width * height) { color })

    /**
     * A probability map of [width] by [height] with [value] inside a bar of half-thickness [halfThickness] that runs
     * from x = [from] to x = [to] through the point ([centerX], [centerY]), turned by [angle] radians; 0 elsewhere.
     */
    fun bar(
        width: Int,
        height: Int,
        centerX: Double,
        centerY: Double,
        from: Int,
        to: Int,
        halfThickness: Double,
        angle: Double,
        value: Float = 0.9f,
    ): FloatArray {
        val map = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in from..to) {
                val lineY = centerY + tan(angle) * (x - centerX)
                if (abs(y - lineY) <= halfThickness) {
                    map[y * width + x] = value
                }
            }
        }

        return map
    }
}

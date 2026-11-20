package com.glyphdraft.engine.ocr

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pixel operations of the pipeline, written out so that the JVM and Android give the same numbers. */
object ImageOps {
    /** Bilinear resize; pixel centres map to pixel centres, as Java 2D does. */
    fun resize(source: PixelImage, width: Int, height: Int): PixelImage {
        val scaleX = source.width.toDouble() / width
        val scaleY = source.height.toDouble() / height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val sy = (y + 0.5) * scaleY - 0.5
            for (x in 0 until width) {
                val sx = (x + 0.5) * scaleX - 0.5
                pixels[y * width + x] = sample(source, sx, sy, clamp = true)
            }
        }

        return PixelImage(width, height, pixels)
    }

    /**
     * Cuts the oriented [box] out of [source] and turns it upright: the result is `length` by `thickness` pixels and
     * its rows run along the text. Parts of the box outside the image are white.
     */
    fun cropUpright(source: PixelImage, box: TextBox): PixelImage {
        val width = max(1, box.length.roundToInt())
        val height = max(1, box.thickness.roundToInt())
        val cos = cos(box.angle)
        val sin = sin(box.angle)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val v = y + 0.5 - height / 2.0
            for (x in 0 until width) {
                val u = x + 0.5 - width / 2.0
                val sx = box.cx + u * cos - v * sin
                val sy = box.cy + u * sin + v * cos
                pixels[y * width + x] = sample(source, sx, sy, clamp = false)
            }
        }

        return PixelImage(width, height, pixels)
    }

    /**
     * Bilinear value at [x], [y] (pixel centres at integers). Outside the image the nearest edge pixel counts when
     * [clamp] is set, else white.
     */
    private fun sample(image: PixelImage, x: Double, y: Double, clamp: Boolean): Int {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        for (dy in 0..1) {
            for (dx in 0..1) {
                val weight = (if (dx == 0) 1 - fx else fx) * (if (dy == 0) 1 - fy else fy)
                val color = pixelAt(image, x0 + dx, y0 + dy, clamp)
                red += weight * ((color shr 16) and 0xFF)
                green += weight * ((color shr 8) and 0xFF)
                blue += weight * (color and 0xFF)
            }
        }

        return (red.roundToInt() shl 16) or (green.roundToInt() shl 8) or blue.roundToInt()
    }

    private fun pixelAt(image: PixelImage, x: Int, y: Int, clamp: Boolean): Int {
        if (clamp) {
            return image.pixels[y.coerceIn(0, image.height - 1) * image.width + x.coerceIn(0, image.width - 1)]
        }

        val inside = x >= 0 && y >= 0 && x < image.width && y < image.height
        if (!inside) {
            return PixelImage.WHITE
        }

        return image.pixels[y * image.width + x]
    }
}

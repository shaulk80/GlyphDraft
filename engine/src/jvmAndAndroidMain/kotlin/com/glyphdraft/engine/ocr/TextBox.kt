package com.glyphdraft.engine.ocr

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * An oriented rectangle around one line of text, in pixels of the original image: centre ([cx], [cy]), [length]
 * along the text, [thickness] across it, and the [angle] of the text direction in radians (0 = left to right,
 * positive = turned clockwise on screen, because y grows downwards).
 */
data class TextBox(val cx: Double, val cy: Double, val length: Double, val thickness: Double, val angle: Double) {
    companion object {
        /** Boxes less elongated than this are taken as axis-aligned: the principal axis of a blob that round is noise. */
        private const val MIN_ELONGATION_FOR_ROTATION = 1.5

        /**
         * The smallest rectangle around the pixels given by [xs] and [ys] (parallel arrays, first [count] entries,
         * in image pixels), aligned with their principal axis. Null for no pixels.
         */
        fun around(xs: DoubleArray, ys: DoubleArray, count: Int): TextBox? {
            if (count == 0) {
                return null
            }

            val axisAligned = fit(xs, ys, count, 0.0)
            val elongation = max(axisAligned.length, axisAligned.thickness) / max(1.0, min(axisAligned.length, axisAligned.thickness))
            if (elongation < MIN_ELONGATION_FOR_ROTATION) {
                return axisAligned
            }

            return fit(xs, ys, count, principalAngle(xs, ys, count))
        }

        private fun principalAngle(xs: DoubleArray, ys: DoubleArray, count: Int): Double {
            var meanX = 0.0
            var meanY = 0.0
            for (i in 0 until count) {
                meanX += xs[i]
                meanY += ys[i]
            }

            meanX /= count
            meanY /= count
            var sxx = 0.0
            var syy = 0.0
            var sxy = 0.0
            for (i in 0 until count) {
                val dx = xs[i] - meanX
                val dy = ys[i] - meanY
                sxx += dx * dx
                syy += dy * dy
                sxy += dx * dy
            }

            return 0.5 * atan2(2 * sxy, sxx - syy)
        }

        /** The bounding rectangle of the pixels in the frame turned by [angle]. */
        private fun fit(xs: DoubleArray, ys: DoubleArray, count: Int, angle: Double): TextBox {
            val cos = cos(angle)
            val sin = sin(angle)
            var minU = Double.MAX_VALUE
            var maxU = -Double.MAX_VALUE
            var minV = Double.MAX_VALUE
            var maxV = -Double.MAX_VALUE
            for (i in 0 until count) {
                val u = xs[i] * cos + ys[i] * sin
                val v = -xs[i] * sin + ys[i] * cos
                minU = min(minU, u)
                maxU = max(maxU, u)
                minV = min(minV, v)
                maxV = max(maxV, v)
            }

            val centerU = (minU + maxU) / 2
            val centerV = (minV + maxV) / 2
            return TextBox(
                cx = centerU * cos - centerV * sin,
                cy = centerU * sin + centerV * cos,
                length = maxU - minU + 1,
                thickness = maxV - minV + 1,
                angle = angle,
            )
        }
    }
}

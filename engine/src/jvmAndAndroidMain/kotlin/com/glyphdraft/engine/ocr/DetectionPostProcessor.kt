package com.glyphdraft.engine.ocr

import kotlin.math.min

/**
 * Turns the probability map of the text detector (one value per pixel of the resized input: how likely it is text)
 * into oriented boxes. The steps are those of the PP-OCRv5 reference (DB post-processing), with the box fitted by
 * principal axis instead of a convex hull: threshold, connected components, drop weak and tiny ones, fit a rotated
 * rectangle, grow it by the unclip ratio (the network predicts a shrunken core of each line).
 */
class DetectionPostProcessor(
    private val threshold: Float = THRESHOLD,
    private val boxThreshold: Double = BOX_THRESHOLD,
    private val unclipRatio: Double = UNCLIP_RATIO,
    private val maxBoxes: Int = MAX_BOXES,
) {
    /**
     * [probabilities] is [width] by [height], row by row. A map pixel stands for [scaleX] by [scaleY] pixels of the
     * original image, and the boxes come back in original pixels.
     */
    fun boxes(probabilities: FloatArray, width: Int, height: Int, scaleX: Double, scaleY: Double): List<TextBox> {
        val boxes = ArrayList<TextBox>()
        val seen = BooleanArray(width * height)
        val queue = IntArray(width * height)
        val minSide = MIN_SIDE * min(scaleX, scaleY)
        for (start in probabilities.indices) {
            if (boxes.size >= maxBoxes) {
                break
            }

            if (seen[start] || probabilities[start] <= threshold) {
                continue
            }

            val size = collectComponent(probabilities, width, height, start, seen, queue)
            val box = boxOf(queue, size, probabilities, width, scaleX, scaleY, minSide)
            if (box != null) {
                boxes.add(box)
            }
        }

        return boxes
    }

    /** Fills [queue] with the pixels connected to [start] (4-neighbourhood, above the threshold); returns how many. */
    private fun collectComponent(
        probabilities: FloatArray,
        width: Int,
        height: Int,
        start: Int,
        seen: BooleanArray,
        queue: IntArray,
    ): Int {
        var head = 0
        var tail = 0
        queue[tail++] = start
        seen[start] = true
        while (head < tail) {
            val pixel = queue[head++]
            val x = pixel % width
            val y = pixel / width
            for (direction in NEIGHBOUR_DX.indices) {
                val nx = x + NEIGHBOUR_DX[direction]
                val ny = y + NEIGHBOUR_DY[direction]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    continue
                }

                val neighbour = ny * width + nx
                if (!seen[neighbour] && probabilities[neighbour] > threshold) {
                    seen[neighbour] = true
                    queue[tail++] = neighbour
                }
            }
        }

        return tail
    }

    private fun boxOf(
        queue: IntArray,
        size: Int,
        probabilities: FloatArray,
        width: Int,
        scaleX: Double,
        scaleY: Double,
        minSide: Double,
    ): TextBox? {
        var scoreSum = 0.0
        val xs = DoubleArray(size)
        val ys = DoubleArray(size)
        for (i in 0 until size) {
            val pixel = queue[i]
            scoreSum += probabilities[pixel]
            // Pixel centre of the map in pixels of the original image.
            xs[i] = ((pixel % width) + 0.5) * scaleX - 0.5
            ys[i] = ((pixel / width) + 0.5) * scaleY - 0.5
        }

        val box = TextBox.around(xs, ys, size)
        if (box == null || scoreSum / size < boxThreshold || min(box.length, box.thickness) < minSide) {
            return null
        }

        return unclip(box)
    }

    private fun unclip(box: TextBox): TextBox {
        val distance = box.length * box.thickness * unclipRatio / (2.0 * (box.length + box.thickness))
        return box.copy(length = box.length + 2 * distance, thickness = box.thickness + 2 * distance)
    }

    companion object {
        /** Values of the PP-OCRv5 mobile detector's `inference.yml` (thresh, box_thresh, unclip_ratio, max_candidates). */
        const val THRESHOLD = 0.3f
        const val BOX_THRESHOLD = 0.6
        const val UNCLIP_RATIO = 1.5
        const val MAX_BOXES = 1000

        /** Components thinner than this many map pixels are noise. */
        private const val MIN_SIDE = 3.0

        private val NEIGHBOUR_DX = intArrayOf(1, -1, 0, 0)
        private val NEIGHBOUR_DY = intArrayOf(0, 0, 1, -1)
    }
}

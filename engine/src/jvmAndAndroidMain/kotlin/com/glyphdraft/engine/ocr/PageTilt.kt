package com.glyphdraft.engine.ocr

import com.glyphdraft.engine.layout.PlacedLine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The tilt of a page, taken out of the places of its rows so that their edges and distances compare: on a photo turned
 * by a few degrees the far end of a line sits a whole line lower than its beginning.
 */
internal class PageTilt(private val radians: Double) {
    /** [row] (boxes of one line from left to right, and their [text]) as it would lie on a level page. */
    fun place(row: List<TextBox>, text: String): PlacedLine =
        PlacedLine(
            text = text,
            start = row.minOf { along(it) - it.length / 2 },
            end = row.maxOf { along(it) + it.length / 2 },
            center = row.map { across(it) }.average(),
            height = row.map { it.thickness }.average(),
        )

    private fun along(box: TextBox): Double = box.cx * cos(radians) + box.cy * sin(radians)

    private fun across(box: TextBox): Double = -box.cx * sin(radians) + box.cy * cos(radians)

    companion object {
        /** Boxes shorter than this many times their thickness do not tell a direction. */
        private const val MIN_ELONGATION = 3.0

        /** The median angle of the long, roughly horizontal boxes; level when there is none. */
        fun of(boxes: List<TextBox>): PageTilt {
            val angles =
                boxes
                    .filter { it.length >= MIN_ELONGATION * it.thickness && abs(it.angle) < PI / 4 }
                    .map { it.angle }
                    .sorted()

            return PageTilt(angles.getOrElse(angles.size / 2) { 0.0 })
        }
    }
}

package com.glyphdraft.engine.ocr

import kotlin.math.min

/** Puts detected boxes in reading order: rows from top to bottom, boxes of a row from left to right. */
object ReadingOrder {
    /**
     * A box belongs to a row when its centre is within half the thickness of the row's centre (the thinner of the two
     * counts, so a tall box does not swallow its neighbours). Rows are found from the top down, so a skewed page whose
     * lines drift is still cut into its lines, as long as the drift within one row stays small.
     */
    fun rows(boxes: List<TextBox>): List<List<TextBox>> = rows(boxes) { it }

    /** The same for [items] that each lie in the box [boxOf] gives. */
    fun <T> rows(items: List<T>, boxOf: (T) -> TextBox): List<List<T>> {
        val rows = ArrayList<MutableList<T>>()
        for (item in items.sortedBy { boxOf(it).cy }) {
            val row = rows.lastOrNull()
            if (row != null && sameRow(row.map(boxOf), boxOf(item))) {
                row.add(item)
            } else {
                rows.add(mutableListOf(item))
            }
        }

        return rows.map { row -> row.sortedBy { boxOf(it).cx } }
    }

    private fun sameRow(row: List<TextBox>, box: TextBox): Boolean {
        val rowCenter = row.sumOf { it.cy } / row.size
        val rowThickness = row.sumOf { it.thickness } / row.size
        val tolerance = min(rowThickness, box.thickness) / 2
        return box.cy - rowCenter < tolerance
    }
}

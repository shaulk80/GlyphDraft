package com.glyphdraft.engine.ocr

import kotlin.math.min

/** Puts detected boxes in reading order: rows from top to bottom, boxes of a row from left to right. */
object ReadingOrder {
    /**
     * A box belongs to a row when its centre is within half the thickness of the row's centre (the thinner of the two
     * counts, so a tall box does not swallow its neighbours). Rows are found from the top down, so a skewed page whose
     * lines drift is still cut into its lines, as long as the drift within one row stays small.
     */
    fun rows(boxes: List<TextBox>): List<List<TextBox>> {
        val rows = ArrayList<MutableList<TextBox>>()
        for (box in boxes.sortedBy { it.cy }) {
            val row = rows.lastOrNull()
            if (row != null && sameRow(row, box)) {
                row.add(box)
            } else {
                rows.add(mutableListOf(box))
            }
        }

        return rows.map { row -> row.sortedBy { it.cx } }
    }

    private fun sameRow(row: List<TextBox>, box: TextBox): Boolean {
        val rowCenter = row.sumOf { it.cy } / row.size
        val rowThickness = row.sumOf { it.thickness } / row.size
        val tolerance = min(rowThickness, box.thickness) / 2
        return box.cy - rowCenter < tolerance
    }
}

package com.glyphdraft.engine.ocr

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingOrderTest {
    private fun box(cx: Double, cy: Double, thickness: Double = 20.0) = TextBox(cx, cy, 100.0, thickness, 0.0)

    @Test
    fun rowsRunTopToBottomAndLeftToRight() {
        val a = box(50.0, 20.0)
        val b = box(250.0, 24.0)
        val c = box(100.0, 60.0)
        val d = box(300.0, 100.0)

        val rows = ReadingOrder.rows(listOf(d, b, c, a))

        assertEquals(listOf(listOf(a, b), listOf(c), listOf(d)), rows)
    }

    @Test
    fun aBoxJoinsARowOnlyWithinHalfTheThinnerThickness() {
        val thick = box(50.0, 20.0, thickness = 40.0)
        val thinNear = box(250.0, 22.0, thickness = 6.0)
        val thinFar = box(450.0, 28.0, thickness = 6.0)

        val rows = ReadingOrder.rows(listOf(thick, thinNear, thinFar))

        assertEquals(listOf(listOf(thick, thinNear), listOf(thinFar)), rows)
    }

    @Test
    fun noBoxesNoRows() {
        assertEquals(emptyList(), ReadingOrder.rows(emptyList()))
    }
}

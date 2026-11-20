package com.glyphdraft.engine.ocr

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetectionPostProcessorTest {
    private val processor = DetectionPostProcessor()

    @Test
    fun barsAreFoundWithTheirAngle() {
        // Angles in radians of the bar drawn into a 200 x 100 map: level, 3.5 degrees, minus 5 degrees.
        for (angle in listOf(0.0, 0.061, -0.087)) {
            val map = TestImages.bar(200, 100, 100.0, 50.0, from = 20, to = 180, halfThickness = 4.0, angle = angle)

            val boxes = processor.boxes(map, 200, 100, 1.0, 1.0)

            assertEquals(1, boxes.size, "angle $angle")
            val box = boxes.single()
            assertTrue(abs(box.angle - angle) < 0.02, "angle $angle was ${box.angle}")
            assertTrue(abs(box.cx - 100) < 1.5 && abs(box.cy - 50) < 1.5, "centre of angle $angle was ${box.cx}, ${box.cy}")
            assertTrue(box.length > 160 && box.thickness > 9, "size of angle $angle was ${box.length} x ${box.thickness}")
        }
    }

    @Test
    fun boxesAreReturnedInPixelsOfTheOriginalImage() {
        val map = TestImages.bar(200, 100, 100.0, 50.0, from = 20, to = 180, halfThickness = 4.0, angle = 0.0)

        val box = processor.boxes(map, 200, 100, 2.0, 3.0).single()

        assertTrue(abs(box.cx - 200.5) < 3 && abs(box.cy - 150.5) < 3, "centre was ${box.cx}, ${box.cy}")
        assertTrue(box.length > 320, "length was ${box.length}")
    }

    @Test
    fun twoSeparateBarsAreTwoBoxes() {
        val upper = TestImages.bar(200, 100, 100.0, 20.0, from = 20, to = 180, halfThickness = 4.0, angle = 0.0)
        val lower = TestImages.bar(200, 100, 100.0, 70.0, from = 20, to = 180, halfThickness = 4.0, angle = 0.0)
        val map = FloatArray(upper.size) { upper[it] + lower[it] }

        assertEquals(2, processor.boxes(map, 200, 100, 1.0, 1.0).size)
    }

    @Test
    fun weakAndTinyBlobsAreDropped() {
        val weak = TestImages.bar(200, 100, 100.0, 50.0, from = 20, to = 180, halfThickness = 4.0, angle = 0.0, value = 0.5f)
        val tiny = TestImages.bar(200, 100, 100.0, 50.0, from = 20, to = 180, halfThickness = 0.4, angle = 0.0)

        assertEquals(0, processor.boxes(weak, 200, 100, 1.0, 1.0).size, "weak")
        assertEquals(0, processor.boxes(tiny, 200, 100, 1.0, 1.0).size, "tiny")
    }

    @Test
    fun aBlobThatIsNearlySquareStaysAxisAligned() {
        val map = TestImages.bar(100, 100, 50.0, 50.0, from = 44, to = 56, halfThickness = 7.0, angle = 0.0)

        val box = processor.boxes(map, 100, 100, 1.0, 1.0).single()

        assertEquals(0.0, box.angle)
    }

    @Test
    fun anEmptyMapHasNoBoxes() {
        assertEquals(emptyList(), processor.boxes(FloatArray(64 * 64), 64, 64, 1.0, 1.0))
    }

    @Test
    fun noMoreBoxesThanTheLimit() {
        val bars = (0 until 4).map { TestImages.bar(100, 100, 50.0, 12.0 + 25 * it, from = 10, to = 90, halfThickness = 3.0, angle = 0.0) }
        val map = FloatArray(100 * 100) { index -> bars.sumOf { it[index].toDouble() }.toFloat() }

        assertEquals(2, DetectionPostProcessor(maxBoxes = 2).boxes(map, 100, 100, 1.0, 1.0).size)
    }
}

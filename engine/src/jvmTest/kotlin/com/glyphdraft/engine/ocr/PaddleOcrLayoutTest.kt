package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class PaddleOcrLayoutTest {
    /** Reads nothing from a crop that is low, as from a speck, and a word from any other. */
    private class SpeckRecognizer : LineRecognizer {
        override fun recognize(line: PixelImage): Outcome<LineText> {
            val text = if (line.height < 40) "" else "word"
            return Outcome.Success(LineText(text, 0.9f))
        }

        override fun close() {}
    }

    @Test
    fun aSpeckThatReadsAsNothingDoesNotSplitARow() =
        runTest {
            // The places of the hand-test photo (M3 finding 1): the speck lies between the two boxes of one row.
            val left = TextBox(cx = 200.0, cy = 100.0, length = 300.0, thickness = 90.0, angle = 0.0)
            val speck = TextBox(cx = 500.0, cy = 112.0, length = 27.0, thickness = 19.0, angle = 0.0)
            val right = TextBox(cx = 700.0, cy = 115.0, length = 180.0, thickness = 100.0, angle = 0.0)
            val rows = ReadingOrder.rows(listOf(left, speck, right))
            assertEquals(2, rows.size, "the speck splits the detected rows")
            val image = TestImages.solid(900, 200, PixelImage.WHITE)

            val read = PaddleOcr(FakeDetector(emptyList())).read(image, rows, SpeckRecognizer())

            assertIs<Outcome.Success<OcrPage>>(read)
            assertEquals("word word", read.value.text)
        }

    @Test
    fun theTiltOfThePageIsTakenOutOfThePlaces() {
        val tilt = 0.1
        // Two lines of one left edge, 50 apart, on a page turned clockwise: the longer one ends further down.
        fun box(length: Double, down: Double): TextBox {
            val x = length / 2
            return TextBox(x * cos(tilt) - down * sin(tilt), x * sin(tilt) + down * cos(tilt), length, 20.0, tilt)
        }
        val page = OcrPage(listOf(listOf(OcrLine("long", 0.9f, box(600.0, 100.0))), listOf(OcrLine("short", 0.9f, box(200.0, 150.0)))))

        val (first, second) = page.placedLines()

        assertEquals(0.0, first.start, 0.001)
        assertEquals(0.0, second.start, 0.001)
        assertEquals(600.0, first.end, 0.001)
        assertEquals(200.0, second.end, 0.001)
        assertEquals(50.0, second.center - first.center, 0.001)
    }
}

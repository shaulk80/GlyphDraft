package com.glyphdraft.engine.ocr

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadQualityTest {
    /** One box of 10 line heights (200 by 20 pixels). */
    private val boxes = listOf(lineBox(20.0, 200.0))

    @Test
    fun densityIsCharactersWithoutSpacesPerLineHeightOfAllBoxes() {
        val twoBoxes = listOf(lineBox(20.0, 200.0), lineBox(50.0, 100.0))

        assertEquals(2.0, ReadQuality.density(boxes, "ab cd ef gh ij kl mn op qr st"))
        assertEquals(1.0, ReadQuality.density(twoBoxes, "x".repeat(15)))
        assertEquals(0.0, ReadQuality.density(emptyList(), "text"))
    }

    @Test
    fun paddleReadingsAreJudgedByTheTextPerLineHeightWithALowerBarForIdeographs() {
        // (text, expected low). 10 line heights: the alphabetic bar is 12 characters, the ideographic bar 8.
        val cases =
            listOf(
                "a".repeat(20) to false,
                "a".repeat(12) to false,
                "a".repeat(11) to true,
                ", . 4" to true,
                "" to true,
                "今天天气很好我们一起去公园散步" to false,
                "今天天气很好我们一" to false,
                "今天天气很好我" to true,
                "こんにちは、世界の皆さん" to false,
            )

        for ((text, expectedLow) in cases) {
            assertEquals(expectedLow, ReadQuality.isLowPaddleReading(boxes, text), "'$text'")
        }
    }

    @Test
    fun withoutBoxesAReadingIsLowQuality() {
        assertEquals(true, ReadQuality.isLowPaddleReading(emptyList(), "plenty of text here"))
    }

    @Test
    fun hebrewReadingsAreJudgedByTheTesseractConfidence() {
        val cases = listOf(0.95f to false, 0.6f to false, 0.59f to true, 0.18f to true)

        for ((confidence, expectedLow) in cases) {
            assertEquals(expectedLow, ReadQuality.isLowHebrewReading(confidence), "confidence $confidence")
        }
    }
}

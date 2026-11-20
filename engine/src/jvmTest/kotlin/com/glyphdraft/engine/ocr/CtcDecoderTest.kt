package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class CtcDecoderTest {
    // Classes: 0 blank, 1 'a', 2 'b', 3 space.
    private val dictionary = (CharacterDictionary.parse("character_dict:\n  - a\n  - b\n") as Outcome.Success).value

    @Test
    fun repeatsAreCollapsedAndBlanksSeparateRealRepeats() {
        val steps =
            arrayOf(
                floatArrayOf(0.1f, 0.9f, 0f, 0f), // a
                floatArrayOf(0.1f, 0.9f, 0f, 0f), // a again: the same character
                floatArrayOf(0.9f, 0f, 0f, 0f), // blank
                floatArrayOf(0.1f, 0.8f, 0f, 0f), // a after a blank: a second one
                floatArrayOf(0f, 0f, 0.7f, 0f), // b
                floatArrayOf(0f, 0f, 0f, 0.6f), // space
            )

        val line = CtcDecoder.decode(steps, dictionary)

        assertEquals("aab ", line.text)
        assertEquals((0.9f + 0.8f + 0.7f + 0.6f) / 4, line.confidence, 1e-6f)
    }

    @Test
    fun onlyBlanksGiveEmptyTextAndZeroConfidence() {
        val line = CtcDecoder.decode(arrayOf(floatArrayOf(1f, 0f, 0f, 0f), floatArrayOf(1f, 0f, 0f, 0f)), dictionary)

        assertEquals(LineText("", 0f), line)
    }
}

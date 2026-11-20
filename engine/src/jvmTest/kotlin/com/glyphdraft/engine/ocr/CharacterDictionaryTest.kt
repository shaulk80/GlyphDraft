package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CharacterDictionaryTest {
    private val yaml =
        listOf(
            "Global:",
            "  model_name: x",
            "PostProcess:",
            "  name: CTCLabelDecode",
            "  character_dict:",
            "  - '!'",
            "  - ''''",
            "  - a",
            "  - ы",
            "  - '0'",
            "  - \\",
            "  - '|'",
        ).joinToString("\r\n")

    @Test
    fun symbolsAreReadFromTheListWithTheirQuotesRemoved() {
        val dictionary = (CharacterDictionary.parse(yaml) as Outcome.Success).value

        // 0 is the blank, then the list in order, then the space.
        val symbols = (0..8).map { dictionary.symbol(it) }
        assertEquals(listOf("", "!", "'", "a", "ы", "0", "\\", "|", " "), symbols)
        assertEquals(9, dictionary.classCount)
    }

    @Test
    fun anIndexOutsideTheDictionaryIsEmpty() {
        val dictionary = (CharacterDictionary.parse(yaml) as Outcome.Success).value

        assertEquals("", dictionary.symbol(9))
        assertEquals("", dictionary.symbol(-1))
    }

    @Test
    fun aFileWithoutAListIsAFailure() {
        for (text in listOf("", "Global:\n  model_name: x\n", "PostProcess:\n  character_dict:\nOther: 1\n")) {
            assertIs<Outcome.Failure>(CharacterDictionary.parse(text), text)
        }
    }
}

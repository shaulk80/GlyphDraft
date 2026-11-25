package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SegmentedTextTest {
    private val segmented = TextSegmenter().segment("One. Two.\n\nThree. Four. Five.")

    @Test
    fun joinPutsTheTranslationBackIntoParagraphs() {
        val translated = listOf("Eins.", "Zwei.", "Drei.", "Vier.", "Fünf.")

        assertEquals(Outcome.Success("Eins. Zwei.\n\nDrei. Vier. Fünf."), segmented.join(translated, Language.German))
    }

    @Test
    fun joinKeepsTheLinesAndDropsThoseLeftEmpty() {
        val lines = TextSegmenter().segment("One. Two.\nThree.\nFour.\n\nFive.")

        val cases: List<Pair<List<String>, String>> =
            listOf(
                listOf("Eins.", "Zwei.", "Drei.", "Vier.", "Fünf.") to "Eins. Zwei.\nDrei.\nVier.\n\nFünf.",
                listOf("Eins.", "Zwei.", " ", "Vier.", "Fünf.") to "Eins. Zwei.\nVier.\n\nFünf.",
            )

        for ((translated, expected) in cases) {
            assertEquals(Outcome.Success(expected), lines.join(translated, Language.German), "translated: $translated")
        }
    }

    @Test
    fun joinUsesNoSpaceForTargetsWithoutSpaces() {
        val translated = listOf("一。", "二。", "三。", "四。", "五。")

        for (target in listOf(Language.Chinese, Language.Japanese)) {
            assertEquals(Outcome.Success("一。二。\n\n三。四。五。"), segmented.join(translated, target), "target: ${target.code}")
        }
    }

    @Test
    fun joinDropsEmptySentencesAndEmptyParagraphs() {
        val cases: List<Pair<List<String>, String>> =
            listOf(
                listOf("Eins.", " ", "Drei.", "", "Fünf.") to "Eins.\n\nDrei. Fünf.",
                listOf("", "  ", "Drei.", "Vier.", "Fünf.") to "Drei. Vier. Fünf.",
                listOf("", "", "", "", "") to "",
            )

        for ((translated, expected) in cases) {
            assertEquals(Outcome.Success(expected), segmented.join(translated, Language.German), "translated: $translated")
        }
    }

    @Test
    fun joinFailsWhenTheCountDiffers() {
        for (count in listOf(0, 4, 6)) {
            val result = segmented.join(List(count) { "x" }, Language.German)

            assertIs<Outcome.Failure>(result, "count: $count")
        }
    }

    @Test
    fun emptyTextHasNoSentencesAndJoinsToNothing() {
        val empty = TextSegmenter().segment("")

        assertEquals(emptyList(), empty.sentences)
        assertEquals(Outcome.Success(""), empty.join(emptyList(), Language.German))
    }
}

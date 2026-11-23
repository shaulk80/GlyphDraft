package com.glyphdraft.engine.translation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The 30 reference sentences of every language (`src/test/resources/translation`) are real sentences of the languages the
 * segmenter has to handle: as one line each they must stay whole, and as one block of 30 lines they must come out as the
 * same 30 sentences.
 */
class ReferenceSetSegmentationTest {
    private val directory = File("src/test/resources/translation")
    private val segmenter = TextSegmenter()
    private val languages = listOf("en", "de", "fr", "es", "ru", "zh", "ja", "he")

    @Test
    fun everyReferenceSentenceStaysWhole() {
        for (language in languages) {
            for (line in lines(language)) {
                assertEquals(listOf(line), segmenter.segment(line).sentences, "$language: $line")
            }
        }
    }

    @Test
    fun aBlockOfReferenceLinesComesOutAsTheSameSentences() {
        for (language in languages) {
            val lines = lines(language)

            assertEquals(lines, segmenter.segment(lines.joinToString("\n")).sentences, language)
        }
    }

    private fun lines(language: String): List<String> =
        File(directory, "$language.txt").readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
}

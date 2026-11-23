package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarianVocabularyReaderTest {
    private val vocabJson = """{"</s>": 0, "<unk>": 1, "▁hello": 2, "▁world": 3, "llo": 4, "<pad>": 5}"""
    private val tokenizerJson =
        """{"version": "1.0", "model": {"type": "Unigram", "vocab": [["<unk>", 0.0], ["▁hello", -2.5], ["▁world", -3.0], ["llo", -4.0], ["▁unused", -1.0]]}}"""

    @Test
    fun readsIdsAndScoresIntoATokenizerThatWorks() {
        val vocabulary = (MarianVocabularyReader.read(vocabJson, tokenizerJson) as Outcome.Success).value
        val tokenizer = UnigramTokenizer(vocabulary)

        assertEquals(listOf(2, 3, 0), tokenizer.encode("hello world").toList())
        assertEquals("hello world", tokenizer.decode(listOf(2, 3, 0)))
        assertEquals(5, tokenizer.padId)
        assertEquals(6, tokenizer.vocabularySize)
    }

    @Test
    fun problemsAreFailures() {
        val cases: List<Pair<String, String>> =
            listOf(
                "not json" to tokenizerJson,
                "[1, 2]" to tokenizerJson,
                """{"▁hello": "two"}""" to tokenizerJson,
                vocabJson to "not json",
                vocabJson to """{"model": {}}""",
                vocabJson to """{"model": {"vocab": [["▁hello"]]}}""",
                vocabJson to """{"model": {"vocab": [[1, 2.0]]}}""",
                // Files of different models: no piece is in both.
                vocabJson to """{"model": {"vocab": [["▁hallo", -1.0], ["▁welt", -2.0]]}}""",
            )

        for ((vocab, tokenizer) in cases) {
            val result = MarianVocabularyReader.read(vocab, tokenizer)

            assertIs<Outcome.Failure>(result, "vocab: $vocab, tokenizer: $tokenizer")
        }
    }
}

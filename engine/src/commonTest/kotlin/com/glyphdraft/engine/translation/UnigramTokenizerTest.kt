package com.glyphdraft.engine.translation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnigramTokenizerTest {
    private val tokenizer = UnigramTokenizer(TestVocabulary.create())

    @Test
    fun encodesWordsWithTheBestScoringPieces() {
        val cases: List<Pair<String, List<Int>>> =
            listOf(
                // "▁hello" (-2) beats "▁he" + "llo" (-6) and "▁hell" + "o" (-5).
                "hello" to listOf(HELLO, EOS),
                "hello world" to listOf(HELLO, WORLD, EOS),
                "  hello \n\t world  " to listOf(HELLO, WORLD, EOS),
                "world hello" to listOf(WORLD, HELLO, EOS),
                // No piece for "hellos": "▁hello" + "s" is the only cover.
                "hellos" to listOf(HELLO, S, EOS),
                "" to listOf(EOS),
                "   " to listOf(EOS),
            )

        for ((text, expected) in cases) {
            assertEquals(expected, tokenizer.encode(text).toList(), "text: $text")
        }
    }

    @Test
    fun anUnknownCharacterIsOneUnknownIdAndARunCollapses() {
        val cases: List<Pair<String, List<Int>>> =
            listOf(
                "hello€" to listOf(HELLO, UNK, EOS),
                "€" to listOf(MARK, UNK, EOS),
                "€€€" to listOf(MARK, UNK, EOS),
                "€ €" to listOf(MARK, UNK, MARK, UNK, EOS),
                "he€llo" to listOf(HE, UNK, LLO, EOS),
            )

        for ((text, expected) in cases) {
            assertEquals(expected, tokenizer.encode(text).toList(), "text: $text")
        }
    }

    @Test
    fun textThatLooksLikeASpecialTokenIsOrdinaryText() {
        val ids = tokenizer.encode("</s> <pad> <unk>").toList()

        assertEquals(EOS, ids.last())
        assertFalse(ids.dropLast(1).contains(EOS), "ids: $ids")
        assertFalse(ids.contains(PAD), "ids: $ids")
    }

    @Test
    fun decodeJoinsPiecesAndSkipsSpecialIds() {
        val cases: List<Pair<List<Int>, String>> =
            listOf(
                listOf(HELLO, WORLD, EOS) to "hello world",
                listOf(MARK, HE, LLO, EOS, PAD) to "hello",
                listOf(HELLO, UNK, WORLD) to "hello world",
                listOf(HELLO, S) to "hellos",
                listOf(HELLO, 999, WORLD) to "hello world",
                emptyList<Int>() to "",
                listOf(EOS) to "",
            )

        for ((ids, expected) in cases) {
            assertEquals(expected, tokenizer.decode(ids), "ids: $ids")
        }
    }

    @Test
    fun decodeUndoesEncode() {
        for (text in listOf("hello world", "world hello hello", "hellos world")) {
            assertEquals(text, tokenizer.decode(tokenizer.encode(text).toList()), "text: $text")
        }
    }

    @Test
    fun exposesTheSpecialIdsAndTheOutputWidth() {
        assertEquals(EOS, tokenizer.endOfSequenceId)
        assertEquals(PAD, tokenizer.padId)
        assertEquals(PAD + 1, tokenizer.vocabularySize)
    }
}

/** A tiny vocabulary with the Marian layout: end of sequence 0, unknown 1, padding last. */
internal object TestVocabulary {
    fun create(): UnigramVocabulary =
        UnigramVocabulary(
            scores =
                mapOf(
                    "</s>" to 0f,
                    "<unk>" to 0f,
                    "▁" to -4f,
                    "▁hello" to -2f,
                    "▁hell" to -2.5f,
                    "▁he" to -3f,
                    "llo" to -3f,
                    "o" to -2.5f,
                    "s" to -2f,
                    "▁world" to -2f,
                    "▁unscored" to -1f,
                ),
            pieceIds =
                mapOf(
                    "</s>" to EOS,
                    "<unk>" to UNK,
                    "▁hello" to HELLO,
                    "▁world" to WORLD,
                    "▁" to MARK,
                    "▁he" to HE,
                    "llo" to LLO,
                    "s" to S,
                    "▁hell" to HELL,
                    "o" to O,
                    "<pad>" to PAD,
                ),
        )
}

private const val EOS = 0
private const val UNK = 1
private const val HELLO = 2
private const val WORLD = 3
private const val MARK = 4
private const val HE = 5
private const val LLO = 6
private const val S = 7
private const val HELL = 8
private const val O = 9
private const val PAD = 10

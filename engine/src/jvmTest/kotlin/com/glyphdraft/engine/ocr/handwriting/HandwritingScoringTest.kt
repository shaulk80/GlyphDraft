package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.core.Language
import kotlin.test.Test
import kotlin.test.assertEquals

/** How the baseline counts: which differences are mistakes, and how pages add up to a figure per language. */
class HandwritingScoringTest {
    @Test
    fun onlyReadingMistakesCount() {
        // expected, read, error rate
        val cases =
            listOf(
                Triple("ו' בכסלו תשפ\"ז", "ו׳ בכסלו תשפ״ז", 0.0),
                Triple("\"a\" - 'b'", "“a” — ‘b’", 0.0),
                Triple("«oui»", "\"oui\"", 0.0),
                Triple("שמי ____ ואני", "שמי __ ואני", 0.0),
                Triple("好，吗？", "好,吗?", 0.0),
                Triple("two words", "twowords", 0.0),
                Triple("abcd", "abed", 0.25),
                Triple("a-b_", "ab", 0.5),
                Triple("ab", "", 1.0),
                Triple("ab", "abcdef", 2.0),
            )

        for ((expected, read, rate) in cases) {
            assertEquals(rate, HandwritingScore.errorRate(expected, read), 1e-9, "expected '$expected', read '$read'")
        }
    }

    @Test
    fun theReportGivesTheMeanPerLanguageAndKindOfSample() {
        val scores =
            listOf(
                score("de-01", Language.German, Source.AI, 0.5),
                score("en-01", Language.English, Source.REAL, 0.1),
                score("en-02", Language.English, Source.REAL, 0.3, "marked low quality"),
                score("en-03", Language.English, Source.AI, 1.0),
            )

        val tables = BaselineReport.markdown(scores).lines().filter { it.startsWith("|") && !it.startsWith("|---") }

        assertEquals(
            listOf(
                "| Language | Samples | Pages | Mean CER % |",
                "| English | real | 2 | 20.0 |",
                "| English | ai | 1 | 100.0 |",
                "| German | ai | 1 | 50.0 |",
                "| Page | Writing | Paper | CER % | Note |",
                "| de-01 | print | lined | 50.0 |  |",
                "| en-01 | print | lined | 10.0 |  |",
                "| en-02 | print | lined | 30.0 | marked low quality |",
                "| en-03 | print | lined | 100.0 |  |",
            ),
            tables,
        )
    }

    private fun score(name: String, language: Language, source: Source, rate: Double, note: String = ""): PageScore =
        PageScore(HandwritingPage(name, language, source, Writing.PRINT, Paper.LINED), rate, note)
}

package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextSegmenterTest {
    private val segmenter = TextSegmenter()

    @Test
    fun splitsIntoSentences() {
        val cases: List<Pair<String, List<String>>> =
            listOf(
                "Hello world. How are you?" to listOf("Hello world.", "How are you?"),
                "Привет. Как дела? Хорошо." to listOf("Привет.", "Как дела?", "Хорошо."),
                "שלום. מה שלומך?" to listOf("שלום.", "מה שלומך?"),
                "你好。今天天气很好！" to listOf("你好。", "今天天气很好！"),
                "こんにちは。元気ですか？" to listOf("こんにちは。", "元気ですか？"),
                "Is it 5? Yes. 5! Okay" to listOf("Is it 5?", "Yes.", "5!", "Okay"),
                "He said \"Go.\" Then he left." to listOf("He said \"Go.\"", "Then he left."),
                "One sentence without a terminator" to listOf("One sentence without a terminator"),
                "" to emptyList(),
                "  \n\n \t " to emptyList(),
            )

        assertSentences(cases)
    }

    @Test
    fun doesNotCutWhereACutWouldBeWrong() {
        val cases: List<Pair<String, List<String>>> =
            listOf(
                "Dr. Smith arrived. He left." to listOf("Dr. Smith arrived.", "He left."),
                "J. Smith went home. Then he slept." to listOf("J. Smith went home.", "Then he slept."),
                "It costs 3.5 euros. Fine." to listOf("It costs 3.5 euros.", "Fine."),
                "Wir treffen uns am 3. Mai im Park." to listOf("Wir treffen uns am 3. Mai im Park."),
                "Wait... what happened? Nothing!" to listOf("Wait... what happened?", "Nothing!"),
                "Ein Beispiel, z.B. dieses hier. Ende." to listOf("Ein Beispiel, z.B. dieses hier.", "Ende."),
                "Wait... What happened?" to listOf("Wait...", "What happened?"),
            )

        assertSentences(cases)
    }

    @Test
    fun joinsWrappedLinesIntoRunningText() {
        val cases: List<Pair<String, List<String>>> =
            listOf(
                "The line wraps\nin the middle of a sentence." to listOf("The line wraps in the middle of a sentence."),
                "A sentence\r\nwith Windows breaks." to listOf("A sentence with Windows breaks."),
                "  indented  \n  lines  " to listOf("indented lines"),
                "The infor-\nmation is here." to listOf("The information is here."),
                "A well-\nKnown name." to listOf("A well- Known name."),
                "日本語の\nテキスト。" to listOf("日本語のテキスト。"),
                "This ends.\nThis starts." to listOf("This ends.", "This starts."),
            )

        assertSentences(cases)
    }

    @Test
    fun paragraphsKeepTheirBlankLine() {
        val cases: List<Pair<String, String>> =
            listOf(
                "First one.\n\nSecond one." to "First one.\n\nSecond one.",
                "First one.\n\n\n\nSecond one." to "First one.\n\nSecond one.",
                "First\nparagraph.\n \nSecond paragraph." to "First paragraph.\n\nSecond paragraph.",
                "\n\nLeading and trailing.\n\n" to "Leading and trailing.",
            )

        for ((text, expected) in cases) {
            val segmented = segmenter.segment(text)
            val rebuilt = segmented.join(segmented.sentences, Language.English)

            assertEquals(Outcome.Success(expected), rebuilt, "text: $text")
        }
    }

    @Test
    fun cutsSentencesThatAreTooLong() {
        val cases: List<Triple<String, Int, List<String>>> =
            listOf(
                Triple(
                    "alpha beta gamma delta epsilon zeta eta theta iota kappa",
                    30,
                    listOf("alpha beta gamma delta", "epsilon zeta eta theta iota", "kappa"),
                ),
                Triple(
                    "aaaa bbbb cccc dddd, eeee ffff gggg hhhh",
                    30,
                    listOf("aaaa bbbb cccc dddd,", "eeee ffff gggg hhhh"),
                ),
                Triple(
                    "字".repeat(24) + "，" + "字".repeat(35),
                    30,
                    listOf("字".repeat(24) + "，", "字".repeat(30), "字".repeat(5)),
                ),
                Triple("😀".repeat(20), 31, listOf("😀".repeat(15), "😀".repeat(5))),
                Triple("short enough.", 30, listOf("short enough.")),
            )

        for ((text, limit, expected) in cases) {
            assertEquals(expected, TextSegmenter(limit).segment(text).sentences, "text: $text")
        }
    }

    @Test
    fun aLimitBelowTheMinimumStillMakesProgress() {
        val parts = TextSegmenter(1).segment("😀".repeat(40)).sentences

        assertEquals("😀".repeat(40), parts.joinToString(""))
        assertTrue(parts.all { it.length <= 20 })
    }

    private fun assertSentences(cases: List<Pair<String, List<String>>>) {
        for ((text, expected) in cases) {
            assertEquals(expected, segmenter.segment(text).sentences, "text: $text")
        }
    }
}

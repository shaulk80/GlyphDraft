package com.glyphdraft.engine.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class LineJoinerTest {
    @Test
    fun wrappedLinesBecomeRunningText() {
        val cases: List<Pair<List<String>, String>> =
            listOf(
                listOf("The line wraps", "in the middle of a sentence.") to "The line wraps in the middle of a sentence.",
                listOf("The infor-", "mation is here.") to "The information is here.",
                listOf("A well-", "Known name.") to "A well- Known name.",
                listOf("日本語の", "テキスト。") to "日本語のテキスト。",
                listOf("One line") to "One line",
                emptyList<String>() to "",
            )

        for ((lines, expected) in cases) {
            assertEquals(expected, LineJoiner.join(lines), "lines: $lines")
        }
    }
}

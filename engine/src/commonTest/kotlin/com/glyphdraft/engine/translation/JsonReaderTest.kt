package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsonReaderTest {
    @Test
    fun readsValues() {
        val cases: List<Pair<String, Any?>> =
            listOf(
                "null" to null,
                "true" to true,
                " false " to false,
                "12" to 12.0,
                "-1.5e2" to -150.0,
                "\"text\"" to "text",
                "\"a\\nb\\t\\\"q\\\" \\\\ \\/ \\u00e9\\u05d0\"" to "a\nb\t\"q\" \\ / éא",
                "[]" to emptyList<Any?>(),
                "{}" to emptyMap<String, Any?>(),
                "[1, [2, \"x\"], {\"k\": null}]" to listOf(1.0, listOf(2.0, "x"), mapOf("k" to null)),
                "{\"a\": {\"b\": [true]}, \"c\": 3}" to mapOf("a" to mapOf("b" to listOf(true)), "c" to 3.0),
                "\uFEFF{\"bom\": 1}" to mapOf("bom" to 1.0),
                "\"▁piece\"" to "▁piece",
            )

        for ((text, expected) in cases) {
            assertEquals(Outcome.Success(expected), JsonReader.parse(text), "json: $text")
        }
    }

    @Test
    fun badJsonIsAFailureWithAMessage() {
        val cases =
            listOf("", "   ", "{", "[1,", "[1,]", "{\"a\" 1}", "{a: 1}", "tru", "\"open", "\"\\x\"", "\"\\u12\"", "[1] x", "-", "{\"a\":1,}")

        for (text in cases) {
            val result = JsonReader.parse(text)

            assertIs<Outcome.Failure>(result, "json: $text")
        }
    }
}

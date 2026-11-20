package com.glyphdraft.engine.language

import com.glyphdraft.core.Script
import kotlin.test.Test
import kotlin.test.assertEquals

class TextScriptsTest {
    @Test
    fun dominantScriptOfText() {
        val cases: List<Pair<String, Script?>> =
            listOf(
                "Hello world" to Script.LATIN,
                "Grüße aus Zürich" to Script.LATIN,
                "Привет, мир" to Script.CYRILLIC,
                "שלום עולם" to Script.HEBREW,
                "你好，世界" to Script.HAN,
                "こんにちは世界" to Script.JAPANESE,
                "カタカナ" to Script.JAPANESE,
                "漢字とかな" to Script.JAPANESE,
                "Hello, мир! 12345 Добро пожаловать" to Script.CYRILLIC,
                "12345 !?" to null,
                "" to null,
                "ΑΒΓ" to null,
            )

        for ((text, expected) in cases) {
            assertEquals(expected, TextScripts.profile(text).dominant, "text: $text")
        }
    }

    @Test
    fun shareCountsLettersOnly() {
        val profile = TextScripts.profile("ab 12 !! вг")
        assertEquals(0.5, profile.share(Script.LATIN))
        assertEquals(0.5, profile.share(Script.CYRILLIC))
        assertEquals(0.0, profile.share(Script.HEBREW))
        assertEquals(0.0, TextScripts.profile("123").share(Script.LATIN))
    }
}

package com.glyphdraft.engine.language

import com.glyphdraft.core.Script

/**
 * How many letters of each script a text has. Only letters count: digits, punctuation and spaces say nothing about
 * the language. Characters outside the scripts of [Script] (Greek, Arabic, Hangul, ...) are counted in [other].
 */
class ScriptProfile(
    private val latin: Int,
    private val cyrillic: Int,
    private val hebrew: Int,
    private val han: Int,
    private val kana: Int,
    val other: Int,
) {
    /** Letters of every script, [other] included. */
    val letters: Int get() = latin + cyrillic + hebrew + han + kana + other

    /**
     * The script of the text, or null when it has no letters of ours. Kana make a text Japanese, and the Han characters
     * among them count with it; without kana the Han characters are Chinese. The script with most letters wins.
     */
    val dominant: Script? get() = counts().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    /** Share (0 to 1) of all letters that belong to [script]. */
    fun share(script: Script): Double {
        if (letters == 0) {
            return 0.0
        }

        return (counts()[script] ?: 0).toDouble() / letters
    }

    private fun counts(): Map<Script, Int> =
        mapOf(
            Script.LATIN to latin,
            Script.CYRILLIC to cyrillic,
            Script.HEBREW to hebrew,
            Script.HAN to if (kana == 0) han else 0,
            Script.JAPANESE to if (kana > 0) han + kana else 0,
        )
}

/** Reads the scripts of a text from the Unicode blocks of its letters. */
object TextScripts {
    fun profile(text: String): ScriptProfile {
        var latin = 0
        var cyrillic = 0
        var hebrew = 0
        var han = 0
        var kana = 0
        var other = 0
        for (character in text) {
            if (!character.isLetter()) {
                continue
            }

            when (character.code) {
                in LATIN_RANGES -> latin++
                in CYRILLIC_RANGE -> cyrillic++
                in HEBREW_RANGES -> hebrew++
                in HAN_RANGES -> han++
                in KANA_RANGES -> kana++
                else -> other++
            }
        }

        return ScriptProfile(latin, cyrillic, hebrew, han, kana, other)
    }

    // Latin-1 letters (without the multiplication and division signs, which are not letters anyway), Latin Extended
    // A and B, and Latin Extended Additional (Vietnamese and others).
    private val LATIN_RANGES = IntRangeSet(0x0041..0x005A, 0x0061..0x007A, 0x00C0..0x024F, 0x1E00..0x1EFF)
    private val CYRILLIC_RANGE = IntRangeSet(0x0400..0x052F)
    private val HEBREW_RANGES = IntRangeSet(0x0590..0x05FF, 0xFB1D..0xFB4F)

    // CJK ideographs: the main block, Extension A and the compatibility block.
    private val HAN_RANGES = IntRangeSet(0x4E00..0x9FFF, 0x3400..0x4DBF, 0xF900..0xFAFF)

    // Hiragana, katakana, katakana phonetic extensions and the half-width katakana.
    private val KANA_RANGES = IntRangeSet(0x3040..0x30FF, 0x31F0..0x31FF, 0xFF66..0xFF9F)
}

/** Several int ranges that work together as one `in` test. */
private class IntRangeSet(private vararg val ranges: IntRange) {
    operator fun contains(value: Int): Boolean = ranges.any { value in it }
}

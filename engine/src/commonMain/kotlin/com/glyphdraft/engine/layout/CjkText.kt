package com.glyphdraft.engine.layout

/** Chinese and Japanese text has no spaces between words, so joining and splitting it differs from the other scripts. */
internal object CjkText {
    // Han (main block, Extension A, compatibility), hiragana and katakana, CJK symbols and punctuation (U+3000 block),
    // half-width katakana and the full-width forms (full-width Latin letters, "，", "！", ...).
    private val RANGES = listOf(0x4E00..0x9FFF, 0x3400..0x4DBF, 0xF900..0xFAFF, 0x3040..0x30FF, 0x3000..0x303F, 0xFF00..0xFFEF)

    fun isCjk(character: Char): Boolean = RANGES.any { character.code in it }
}

package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.engine.ocr.Cer
import java.text.Normalizer

/**
 * The character error rate of a handwriting reading: [Cer] (whitespace ignored) after folding the differences that are
 * not reading mistakes (decision 0022): quote and dash shapes, the length of a blank drawn as underscores, the
 * full-width against the half-width form of a character, and letter case on a page that allows either.
 */
internal object HandwritingScore {
    private const val SINGLE_QUOTES = "‘’‚‛׳"
    private const val DOUBLE_QUOTES = "“”„‟«»״"
    private const val DASHES = "‐‑‒–—―−־"
    private val blanks = Regex("_+")

    fun errorRate(expected: String, actual: String, letterCase: LetterCase): Double =
        Cer.of(normalised(expected, letterCase), normalised(actual, letterCase))

    private fun normalised(text: String, letterCase: LetterCase): String {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val plain = String(CharArray(folded.length) { plainShape(folded[it]) }).replace(blanks, "_")

        return when (letterCase) {
            LetterCase.EXACT -> plain
            LetterCase.EITHER -> plain.lowercase()
        }
    }

    private fun plainShape(character: Char): Char =
        when (character) {
            in SINGLE_QUOTES -> '\''
            in DOUBLE_QUOTES -> '"'
            in DASHES -> '-'
            else -> character
        }
}

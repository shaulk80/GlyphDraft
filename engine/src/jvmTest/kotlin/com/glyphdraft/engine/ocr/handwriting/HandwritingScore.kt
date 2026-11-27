package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.engine.ocr.Cer
import java.text.Normalizer

/**
 * The character error rate of a handwriting reading: [Cer] (whitespace ignored) after folding the differences that are
 * not reading mistakes (decision 0022): quote and dash shapes, the length of a blank drawn as underscores, and the
 * full-width against the half-width form of a character.
 */
internal object HandwritingScore {
    private const val SINGLE_QUOTES = "‘’‚‛׳"
    private const val DOUBLE_QUOTES = "“”„‟«»״"
    private const val DASHES = "‐‑‒–—―−־"
    private val blanks = Regex("_+")

    fun errorRate(expected: String, actual: String): Double = Cer.of(normalised(expected), normalised(actual))

    private fun normalised(text: String): String {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val plain = String(CharArray(folded.length) { plainShape(folded[it]) })

        return plain.replace(blanks, "_")
    }

    private fun plainShape(character: Char): Char =
        when (character) {
            in SINGLE_QUOTES -> '\''
            in DOUBLE_QUOTES -> '"'
            in DASHES -> '-'
            else -> character
        }
}

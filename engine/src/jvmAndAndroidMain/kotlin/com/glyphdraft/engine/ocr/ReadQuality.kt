package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Script
import com.glyphdraft.engine.language.TextScripts

/**
 * Whether a reading looks wrong. The recognizers' own confidence cannot tell: the Latin recognizer reads a Russian page
 * as a few commas and dots with a mean confidence of 0.94, higher than some correct readings. What does tell is how
 * much text came out of the boxes the detector found: a line of real text of one height has about two characters per
 * line height in an alphabetic script (measured 2.0 to 2.7 on the fixtures and on a photographed page) and about one
 * in Chinese or Japanese (1.2 to 1.4); a wrong recognizer gives 0.02 to 0.84. Hebrew is read by Tesseract, whose mean
 * confidence does separate: 0.92 on Hebrew, 0.18 to 0.50 on everything else.
 */
internal object ReadQuality {
    /** Characters per line height under which an alphabetic reading counts as wrong. */
    const val MIN_ALPHABETIC_DENSITY = 1.2

    /** The same for Chinese and Japanese, whose characters are about twice as wide as a letter. */
    const val MIN_IDEOGRAPHIC_DENSITY = 0.8

    /** Mean Tesseract confidence under which a Hebrew reading counts as wrong. */
    const val MIN_HEBREW_CONFIDENCE = 0.6f

    /** Characters of [text] (spaces not counted) per line height of the detected [boxes]; 0 without boxes. */
    fun density(boxes: List<TextBox>, text: String): Double {
        val lineHeights = boxes.sumOf { it.length / it.thickness.coerceAtLeast(1.0) }
        if (lineHeights <= 0.0) {
            return 0.0
        }

        return text.count { !it.isWhitespace() } / lineHeights
    }

    fun isLowPaddleReading(boxes: List<TextBox>, text: String): Boolean {
        val ideographic = TextScripts.profile(text).dominant in IDEOGRAPHIC
        val minimum = if (ideographic) MIN_IDEOGRAPHIC_DENSITY else MIN_ALPHABETIC_DENSITY
        return density(boxes, text) < minimum
    }

    fun isLowHebrewReading(confidence: Float): Boolean = confidence < MIN_HEBREW_CONFIDENCE

    private val IDEOGRAPHIC = setOf(Script.HAN, Script.JAPANESE)
}

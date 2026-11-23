package com.glyphdraft.engine.translation

/**
 * Cuts a sentence that is too long for the model (OPUS-MT reads at most 512 tokens, and OCR text without punctuation
 * can be a whole page) into parts of at most [maxLength] characters. A part ends, in order of preference, after a comma,
 * semicolon or colon, at a space, or, for text without spaces, after a Chinese or Japanese comma; the last resort is a
 * hard cut that keeps a surrogate pair together.
 */
internal class SentenceLimiter(maxLength: Int) {
    // A limit below a few words would make parts of one or two characters (and no progress for a surrogate pair).
    private val maxLength: Int = maxLength.coerceAtLeast(MIN_LENGTH)

    fun limit(sentence: String): List<String> {
        val parts = ArrayList<String>()
        var rest = sentence
        while (rest.length > maxLength) {
            val cut = findCut(rest)
            parts.add(rest.substring(0, cut).trim())
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) {
            parts.add(rest)
        }

        return parts
    }

    /** Index at which to cut [text], which is longer than [maxLength]; always between 1 and [maxLength]. */
    private fun findCut(text: String): Int {
        val earliest = maxLength / 2
        val lastIndex = maxLength - 1
        for (index in lastIndex downTo earliest) {
            if (isBreakAfter(text, index)) {
                return index + 1
            }
        }
        for (index in lastIndex downTo earliest) {
            if (text[index].isWhitespace()) {
                return index
            }
        }

        return hardCut(text)
    }

    private fun isBreakAfter(text: String, index: Int): Boolean {
        val character = text[index]
        if (character in CJK_BREAKS) {
            return true
        }

        return character in WESTERN_BREAKS && text[index + 1].isWhitespace()
    }

    private fun hardCut(text: String): Int {
        if (text[maxLength - 1].isHighSurrogate()) {
            return maxLength - 1
        }

        return maxLength
    }

    private companion object {
        const val WESTERN_BREAKS = ",;:"
        const val CJK_BREAKS = "，、；："
        const val MIN_LENGTH = 20
    }
}

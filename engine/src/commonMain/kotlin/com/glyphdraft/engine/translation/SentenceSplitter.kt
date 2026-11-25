package com.glyphdraft.engine.translation

/**
 * Cuts a paragraph into sentences for Latin, Cyrillic and Hebrew text (a terminator followed by a space) and for Chinese
 * and Japanese text (a full-width terminator ends a sentence at once).
 *
 * A wrong cut costs more than a missed one: the model translates half a sentence badly (it may invent the rest, such as
 * a phone number after "tel."), while a long sentence is only slower. So in doubt there is no cut: after an abbreviation,
 * an initial or a number ("3. Mai"), and, after a period, before a lowercase letter or a plus sign. Sentences that stay
 * too long are cut by [SentenceLimiter].
 */
internal object SentenceSplitter {
    private const val TERMINATORS = ".!?…"
    private const val CJK_TERMINATORS = "。！？"
    private const val CLOSERS = "\"'”’»)]」』）"

    /** Words (lowercase, without the dot) after which a period does not end a sentence. */
    private val ABBREVIATIONS =
        setOf(
            "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "no", "nr", "fig", "approx", "dept", "inc", "ltd",
            "e.g", "i.e", "z.b", "d.h", "bzw", "ca", "tel", "тел",
        )

    /** [paragraph] is one line of running text; the result has no blank sentence. */
    fun split(paragraph: String): List<String> {
        val sentences = ArrayList<String>()
        var start = 0
        var position = 0
        while (position < paragraph.length) {
            if (!isTerminator(paragraph[position])) {
                position++
                continue
            }

            val end = endOfRun(paragraph, position)
            if (endsSentence(paragraph, position, end)) {
                addTrimmed(sentences, paragraph.substring(start, end))
                start = end
            }
            position = end
        }
        addTrimmed(sentences, paragraph.substring(start))

        return sentences
    }

    /** Whether a sentence ends with [line] when [next] comes after it. */
    fun isSentenceEnd(line: String, next: String): Boolean {
        val unclosed = line.trimEnd { it in CLOSERS }
        val terminators = unclosed.indexOfLast { !isTerminator(it) } + 1
        if (terminators == unclosed.length) {
            return false
        }

        return endsSentence("$line $next", terminators, line.length)
    }

    private fun isTerminator(character: Char): Boolean = character in TERMINATORS || character in CJK_TERMINATORS

    /** Index after the terminators that begin at [from] and the closing quotes and brackets that follow them. */
    private fun endOfRun(text: String, from: Int): Int {
        var end = from
        while (end < text.length && isTerminator(text[end])) {
            end++
        }
        while (end < text.length && text[end] in CLOSERS) {
            end++
        }

        return end
    }

    /** [from] is the first terminator, [end] the index after the run with its closers. */
    private fun endsSentence(text: String, from: Int, end: Int): Boolean {
        if (end >= text.length) {
            return true
        }
        if (text[from] in CJK_TERMINATORS) {
            return true
        }
        if (!text[end].isWhitespace()) {
            return false
        }
        if (!isWeakRun(text, from, end)) {
            return true
        }

        return !(isAbbreviation(text, from) || continuesTheSentence(text, end))
    }

    /** A run of periods or ellipses ends a sentence only under conditions; "?" and "!" always do. */
    private fun isWeakRun(text: String, from: Int, end: Int): Boolean {
        for (index in from until end) {
            if (text[index] == '?' || text[index] == '!') {
                return false
            }
        }

        return true
    }

    /** What follows [from] cannot begin a sentence: a lowercase letter, or the plus sign of a phone number. */
    private fun continuesTheSentence(text: String, from: Int): Boolean {
        val next = text.substring(from).firstOrNull { !it.isWhitespace() }

        return next != null && (next.isLowerCase() || next == '+')
    }

    /** The word in front of the period at [period] is an abbreviation, an initial ("J.") or a number ("3."). */
    private fun isAbbreviation(text: String, period: Int): Boolean {
        var wordStart = period
        while (wordStart > 0 && !text[wordStart - 1].isWhitespace()) {
            wordStart--
        }

        val word = text.substring(wordStart, period).trimStart('(', '[', '"', '\'', '“', '„', '«')
        if (word.isEmpty()) {
            return false
        }

        return word.all { it.isDigit() } ||
            (word.length == 1 && word[0].isUpperCase()) ||
            word.lowercase() in ABBREVIATIONS
    }

    private fun addTrimmed(into: MutableList<String>, sentence: String) {
        val trimmed = sentence.trim()
        if (trimmed.isNotEmpty()) {
            into.add(trimmed)
        }
    }
}

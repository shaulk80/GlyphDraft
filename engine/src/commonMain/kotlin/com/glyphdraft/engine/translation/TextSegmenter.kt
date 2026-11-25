package com.glyphdraft.engine.translation

/**
 * Prepares text for translation: a blank line separates paragraphs, every other line break is meant and kept (the
 * lines a page wrapped are joined before, decision 0021), and each line is cut into sentences of at most
 * [maxSentenceLength] characters (a model translates a sentence at a time and loses meaning, memory and time on a long
 * one). Several blank lines count as one.
 */
class TextSegmenter(maxSentenceLength: Int = DEFAULT_MAX_SENTENCE_LENGTH) {
    private val limiter = SentenceLimiter(maxSentenceLength)

    fun segment(text: String): SegmentedText = SegmentedText(paragraphLines(text).map { lines -> lines.map(::sentencesOf) })

    private fun sentencesOf(line: String): List<String> = SentenceSplitter.split(line).flatMap { limiter.limit(it) }

    /** Lines of each paragraph, trimmed; blank lines (any line break style) only separate. */
    private fun paragraphLines(text: String): List<List<String>> {
        val paragraphs = ArrayList<List<String>>()
        var current = ArrayList<String>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isNotEmpty()) {
                current.add(line)
                continue
            }

            if (current.isNotEmpty()) {
                paragraphs.add(current)
                current = ArrayList()
            }
        }
        if (current.isNotEmpty()) {
            paragraphs.add(current)
        }

        return paragraphs
    }

    companion object {
        /** About 100 to 130 tokens, far under the 512 the OPUS-MT models accept, and one sentence of real prose. */
        const val DEFAULT_MAX_SENTENCE_LENGTH = 400
    }
}

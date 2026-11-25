package com.glyphdraft.engine.layout

import com.glyphdraft.engine.translation.SentenceSplitter

/** Decides from the places of the lines which line breaks of a page are meant and which only wrap a paragraph. */
internal object PageLayout {
    /** [lines] are in reading order. */
    fun compose(lines: List<PlacedLine>): PageText {
        val breaks = breaksOf(lines)

        return PageText(asOnThePage(lines, breaks), running(lines, breaks))
    }

    /** What separates each line from the next one. */
    private fun breaksOf(lines: List<PlacedLine>): List<LineBreak> {
        if (lines.isEmpty()) {
            return emptyList()
        }

        val measures = PageMeasures(lines)
        val breaks = ArrayList<LineBreak>()
        for ((line, next) in lines.zipWithNext()) {
            breaks.add(measures.breakBetween(line, next, inParagraph = breaks.lastOrNull() == LineBreak.WRAP))
        }

        return breaks
    }

    private fun asOnThePage(lines: List<PlacedLine>, breaks: List<LineBreak>): String =
        buildString {
            lines.forEachIndexed { index, line ->
                append(line.text)
                append(breaks.getOrNull(index)?.onThePage.orEmpty())
            }
        }

    private fun running(lines: List<PlacedLine>, breaks: List<LineBreak>): String {
        val result = StringBuilder()
        val wrapped = ArrayList<String>()
        lines.forEachIndexed { index, line ->
            wrapped.add(line.text)
            val after = breaks.getOrNull(index)
            if (after != LineBreak.WRAP) {
                result.append(LineJoiner.join(wrapped)).append(after?.onThePage.orEmpty())
                wrapped.clear()
            }
        }

        return result.toString()
    }
}

/** What separates two lines; [onThePage] is how it is written in the text that keeps the lines of the page. */
private enum class LineBreak(val onThePage: String) {
    /** The paragraph goes on: joined in the running text. */
    WRAP("\n"),

    /** The next line was meant to start there: a heading, a list item, the end of a sentence. */
    LINE("\n"),

    /** A gap between blocks. */
    BLOCK("\n\n"),
}

/**
 * The rules of decision 0021, with the measures of the page they compare against. [lines] must not be empty.
 *
 * A line break is a wrap only on evidence: the next line goes on in lower case, or the line is long and ends so close to
 * the margin of the page that the next word had no room. A wrong join makes the translator drop text, and so does a
 * wrong cut. So a list mark or a colon keeps the break whatever the places say, and so does a sentence end, unless the
 * line is inside a wrapped paragraph and had no room for the next word.
 */
private class PageMeasures(lines: List<PlacedLine>) {
    /** The upper quartile, since a line without ascenders and descenders measures lower than the writing is. */
    private val lineHeight = quantile(lines.map { it.height }, UPPER_QUARTILE) ?: 0.0

    /** How far right (in the direction of the writing) the text of the page reaches. */
    private val margin = lines.maxOf { it.end }

    /** The distance from line to line inside a block; null for a single line. Rows that overlap do not count. */
    private val usualPitch =
        quantile(lines.zipWithNext { above, below -> below.center - above.center }.filter { it >= MIN_PITCH * lineHeight }, LOWER_QUARTILE)

    /** [inParagraph] says that [line] goes on from the line above it. */
    fun breakBetween(line: PlacedLine, next: PlacedLine, inParagraph: Boolean): LineBreak =
        when {
            isBlockGap(next.center - line.center) -> LineBreak.BLOCK
            LIST_MARK.containsMatchIn(next.text) -> LineBreak.LINE
            line.text.last() in COLONS -> LineBreak.LINE
            SentenceSplitter.isSentenceEnd(line.text, next.text) -> wrapIf(inParagraph && hadNoRoomFor(line, next))
            next.text.first().isLowerCase() -> LineBreak.WRAP
            else -> wrapIf(hadNoRoomFor(line, next))
        }

    private fun wrapIf(wrapped: Boolean): LineBreak = if (wrapped) LineBreak.WRAP else LineBreak.LINE

    /** A [pitch] of zero or less is the step to the next column. */
    private fun isBlockGap(pitch: Double): Boolean {
        if (pitch <= 0.0 || pitch > ALWAYS_A_GAP * lineHeight) {
            return true
        }

        val usual = usualPitch ?: return false
        return pitch > GAP_OVER_USUAL * usual && pitch > MIN_GAP * lineHeight
    }

    private fun hadNoRoomFor(line: PlacedLine, next: PlacedLine): Boolean {
        if (line.end - line.start < MIN_WRAPPED_LENGTH * lineHeight) {
            return false
        }

        val characterWidth = (next.end - next.start) / next.text.length
        return line.end + charactersNeededFor(next.text) * characterWidth > margin
    }

    /** The room the beginning of [text] takes after a line: its first word and a space; two characters without spaces. */
    private fun charactersNeededFor(text: String): Int {
        if (CjkText.isCjk(text.first())) {
            // Two, because punctuation rules move one more character to the next line.
            return CJK_CHARACTERS
        }

        return text.substringBefore(' ').length + 1
    }

    private fun quantile(values: List<Double>, fraction: Double): Double? {
        val sorted = values.sorted()
        return sorted.getOrNull((fraction * (sorted.size - 1)).toInt())
    }

    private companion object {
        const val LOWER_QUARTILE = 0.25
        const val UPPER_QUARTILE = 0.75

        // All in line heights, except GAP_OVER_USUAL, which is in usual pitches. Measured: wrapped lines are 1.0 to 1.7
        // line heights apart (double spacing included), blocks 1.9 to 3.4.
        const val MIN_PITCH = 0.6
        const val MIN_GAP = 1.7
        const val ALWAYS_A_GAP = 2.8
        const val GAP_OVER_USUAL = 1.5
        const val MIN_WRAPPED_LENGTH = 12

        const val CJK_CHARACTERS = 2
        const val COLONS = ":："

        /** A bullet, or a dash, asterisk or number of up to two digits that is followed by a space. */
        val LIST_MARK = Regex("""^(?:[•‣▪●・]|(?:[-–—·*]|\d{1,2}[.)])\s)""")
    }
}

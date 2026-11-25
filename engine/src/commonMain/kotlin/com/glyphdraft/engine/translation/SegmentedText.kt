package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Script

/**
 * A text cut into paragraphs, lines and sentences. The translator translates [sentences] one by one and [join] puts the
 * results back into the layout of the original.
 */
class SegmentedText internal constructor(private val paragraphs: List<List<List<String>>>) {
    /** Every sentence of every line of every paragraph, in reading order. */
    val sentences: List<String> = paragraphs.flatten().flatten()

    /**
     * Rebuilds the text from the translation of each of [sentences] (the same count, the same order). The sentences of a
     * line are separated by a space, or by nothing when [target] is written without spaces; lines by a line break and
     * paragraphs by one blank line. A sentence the model left empty is dropped, and so is a line or paragraph with
     * nothing left in it.
     */
    fun join(translated: List<String>, target: Language): Outcome<String> {
        if (translated.size != sentences.size) {
            return Outcome.Failure("Expected ${sentences.size} translated sentences but got ${translated.size}")
        }

        val separator = if (target.script == Script.HAN || target.script == Script.JAPANESE) "" else " "
        val remaining = translated.iterator()
        val result =
            paragraphs.map { lines ->
                val joinedLines = lines.map { line -> joinNonEmpty(line.map { remaining.next().trim() }, separator) }
                joinNonEmpty(joinedLines, LINE_SEPARATOR)
            }

        return Outcome.Success(joinNonEmpty(result, PARAGRAPH_SEPARATOR))
    }

    private fun joinNonEmpty(parts: List<String>, separator: String): String = parts.filter { it.isNotEmpty() }.joinToString(separator)

    private companion object {
        const val LINE_SEPARATOR = "\n"
        const val PARAGRAPH_SEPARATOR = "\n\n"
    }
}

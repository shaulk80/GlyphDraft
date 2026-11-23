package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Script

/**
 * A text cut into paragraphs and sentences. The translator translates [sentences] one by one and [join] puts the
 * results back into the paragraph layout of the original.
 */
class SegmentedText internal constructor(private val paragraphs: List<List<String>>) {
    /** Every sentence of every paragraph, in reading order. */
    val sentences: List<String> = paragraphs.flatten()

    /**
     * Rebuilds the text from the translation of each of [sentences] (the same count, the same order). A paragraph is the
     * translation of its sentences in one line, separated by a space, or by nothing when [target] is written without
     * spaces; paragraphs are separated by one blank line. A sentence the model left empty is dropped.
     */
    fun join(translated: List<String>, target: Language): Outcome<String> {
        if (translated.size != sentences.size) {
            return Outcome.Failure("Expected ${sentences.size} translated sentences but got ${translated.size}")
        }

        val separator = if (target.script == Script.HAN || target.script == Script.JAPANESE) "" else " "
        val result = ArrayList<String>()
        var next = 0
        for (paragraph in paragraphs) {
            val parts = translated.subList(next, next + paragraph.size).map { it.trim() }.filter { it.isNotEmpty() }
            next += paragraph.size
            if (parts.isNotEmpty()) {
                result.add(parts.joinToString(separator))
            }
        }

        return Outcome.Success(result.joinToString(PARAGRAPH_SEPARATOR))
    }

    private companion object {
        const val PARAGRAPH_SEPARATOR = "\n\n"
    }
}

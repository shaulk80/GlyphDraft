package com.glyphdraft.engine.translation

/**
 * Joins the lines of one OCR paragraph into running text. OCR cuts text where the page does, so a line break inside a
 * paragraph is not a sentence boundary and must not reach the translator.
 */
internal object LineJoiner {
    private const val SOFT_HYPHEN = '­'

    /** [lines] are trimmed and not blank. */
    fun join(lines: List<String>): String {
        val builder = StringBuilder()
        for (line in lines) {
            append(builder, line)
        }

        return builder.toString()
    }

    private fun append(builder: StringBuilder, line: String) {
        if (builder.isEmpty()) {
            builder.append(line)
            return
        }

        if (endsWithHyphenatedWord(builder) && line.first().isLowerCase()) {
            // "informa-" + "tion": the hyphen only marks the break. A real compound ("well-" + "known") loses its
            // hyphen too; a wrong repair of that kind reads better than a stray "informa- tion".
            builder.setLength(builder.length - 1)
            builder.append(line)
            return
        }

        if (!needsSpace(builder.last(), line.first())) {
            builder.append(line)
            return
        }

        builder.append(' ').append(line)
    }

    private fun endsWithHyphenatedWord(builder: StringBuilder): Boolean {
        val last = builder.last()
        if (last == SOFT_HYPHEN) {
            return true
        }

        return last == '-' && builder.length > 1 && builder[builder.length - 2].isLetter()
    }

    private fun needsSpace(before: Char, after: Char): Boolean = !CjkText.isCjk(before) && !CjkText.isCjk(after)
}

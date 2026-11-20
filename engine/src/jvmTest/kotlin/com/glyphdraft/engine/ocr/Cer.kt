package com.glyphdraft.engine.ocr

/** Character error rate: edit distance over the length of the expected text, with all whitespace ignored. */
internal object Cer {
    fun of(expected: String, actual: String): Double {
        val a = expected.filterNot { it.isWhitespace() }
        val b = actual.filterNot { it.isWhitespace() }
        if (a.isEmpty()) {
            return if (b.isEmpty()) 0.0 else 1.0
        }

        return editDistance(a, b).toDouble() / a.length
    }

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }

            previous = current
        }

        return previous[b.length]
    }
}

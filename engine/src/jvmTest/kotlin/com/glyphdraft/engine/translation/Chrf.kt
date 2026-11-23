package com.glyphdraft.engine.translation

import kotlin.math.max

/** Corpus-level chrF (character n-grams 1 to 6, whitespace ignored, beta 2), 0 to 100; the same figure the spike reported (docs/spikes/m3-translation.md). */
internal object Chrf {
    private const val MAX_ORDER = 6
    private const val BETA = 2.0

    fun score(hypotheses: List<String>, references: List<String>): Double {
        require(hypotheses.size == references.size) { "${hypotheses.size} hypotheses against ${references.size} references" }
        val match = DoubleArray(MAX_ORDER)
        val hypothesisTotal = DoubleArray(MAX_ORDER)
        val referenceTotal = DoubleArray(MAX_ORDER)
        for (index in hypotheses.indices) {
            val hypothesis = hypotheses[index].filter { !it.isWhitespace() }
            val reference = references[index].filter { !it.isWhitespace() }
            for (order in 1..MAX_ORDER) {
                val hypothesisGrams = grams(hypothesis, order)
                val referenceGrams = grams(reference, order)
                hypothesisTotal[order - 1] += hypothesisGrams.values.sum().toDouble()
                referenceTotal[order - 1] += referenceGrams.values.sum().toDouble()
                for ((gram, count) in hypothesisGrams) {
                    match[order - 1] += minOf(count, referenceGrams[gram] ?: 0).toDouble()
                }
            }
        }

        var precision = 0.0
        var recall = 0.0
        for (order in 0 until MAX_ORDER) {
            precision += if (hypothesisTotal[order] > 0) match[order] / hypothesisTotal[order] else 0.0
            recall += if (referenceTotal[order] > 0) match[order] / referenceTotal[order] else 0.0
        }
        precision /= MAX_ORDER
        recall /= MAX_ORDER

        val denominator = max(BETA * BETA * precision + recall, 1e-12)
        return 100.0 * (1 + BETA * BETA) * precision * recall / denominator
    }

    private fun grams(text: String, order: Int): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (start in 0..text.length - order) {
            counts.merge(text.substring(start, start + order), 1, Int::plus)
        }

        return counts
    }
}

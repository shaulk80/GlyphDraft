package spike

import kotlin.math.max

/** Corpus-level chrF (character n-grams 1..6, whitespace ignored, beta 2), n-gram counts summed over all sentences. */
object Metrics {
    private const val MAX_ORDER = 6
    private const val BETA = 2.0

    fun chrF(hypotheses: List<String>, references: List<String>): Double {
        val match = DoubleArray(MAX_ORDER)
        val hypTotal = DoubleArray(MAX_ORDER)
        val refTotal = DoubleArray(MAX_ORDER)
        for (i in hypotheses.indices) {
            val h = hypotheses[i].filter { !it.isWhitespace() }
            val r = references[i].filter { !it.isWhitespace() }
            for (order in 1..MAX_ORDER) {
                val hg = grams(h, order)
                val rg = grams(r, order)
                hypTotal[order - 1] += hg.values.sum().toDouble()
                refTotal[order - 1] += rg.values.sum().toDouble()
                for ((gram, count) in hg) {
                    match[order - 1] += minOf(count, rg[gram] ?: 0).toDouble()
                }
            }
        }
        var precision = 0.0
        var recall = 0.0
        for (k in 0 until MAX_ORDER) {
            precision += if (hypTotal[k] > 0) match[k] / hypTotal[k] else 0.0
            recall += if (refTotal[k] > 0) match[k] / refTotal[k] else 0.0
        }
        precision /= MAX_ORDER
        recall /= MAX_ORDER
        val denominator = max(BETA * BETA * precision + recall, 1e-12)
        return 100.0 * (1 + BETA * BETA) * precision * recall / denominator
    }

    private fun grams(text: String, order: Int): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (i in 0..text.length - order) {
            counts.merge(text.substring(i, i + order), 1, Int::plus)
        }
        return counts
    }
}

package com.glyphdraft.engine.ocr

/** Text of one line and how sure the recognizer is: the mean probability of the characters that were kept. */
data class LineText(val text: String, val confidence: Float)

/** Greedy CTC decoding: best class per step, repeats collapsed, blanks dropped. */
object CtcDecoder {
    fun decode(steps: Array<FloatArray>, dictionary: CharacterDictionary): LineText {
        val text = StringBuilder()
        var confidenceSum = 0f
        var kept = 0
        var previous = -1
        for (step in steps) {
            val best = indexOfMax(step)
            if (best != BLANK && best != previous) {
                text.append(dictionary.symbol(best))
                confidenceSum += step[best]
                kept++
            }

            previous = best
        }

        val confidence = if (kept == 0) 0f else confidenceSum / kept
        return LineText(text.toString(), confidence)
    }

    private fun indexOfMax(values: FloatArray): Int {
        var best = 0
        for (i in values.indices) {
            if (values[i] > values[best]) {
                best = i
            }
        }

        return best
    }

    private const val BLANK = 0
}

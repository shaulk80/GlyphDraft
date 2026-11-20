package spike

object Metrics {
    /** Character error rate: edit distance over expected length, ignoring all whitespace. */
    fun cer(expected: String, actual: String): Double {
        val a = expected.filterNot { it.isWhitespace() }
        val b = actual.filterNot { it.isWhitespace() }
        if (a.isEmpty()) {
            return if (b.isEmpty()) 0.0 else 1.0
        }
        return levenshtein(a, b).toDouble() / a.length
    }

    private fun levenshtein(a: String, b: String): Int {
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

/** Script of a text by Unicode block; the first step of language detection and of recogniser choice. */
object Scripts {
    fun dominant(text: String): String {
        val counts = HashMap<String, Int>()
        for (ch in text) {
            val script = when (Character.UnicodeScript.of(ch.code)) {
                Character.UnicodeScript.LATIN -> "Latin"
                Character.UnicodeScript.CYRILLIC -> "Cyrillic"
                Character.UnicodeScript.HEBREW -> "Hebrew"
                Character.UnicodeScript.HAN -> "Han"
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> "Kana"
                else -> continue
            }
            counts.merge(script, 1, Int::plus)
        }
        return counts.maxByOrNull { it.value }?.key ?: "Latin"
    }

    fun recognizerFor(text: String): String = when (dominant(text)) {
        "Cyrillic" -> "eslav_PP-OCRv5_mobile_rec_onnx"
        "Latin" -> "latin_PP-OCRv5_mobile_rec_onnx"
        else -> "PP-OCRv5_mobile_rec_onnx"
    }
}

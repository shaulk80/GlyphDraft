package com.glyphdraft.engine.language

import com.glyphdraft.core.Language
import com.glyphdraft.core.LanguageGuess

/**
 * Chooses among the Latin-script languages by counting their markers ([LatinMarkers]) in a text: one point for each
 * common word, each marker letter and each marker apostrophe form. It needs a sentence, not a single word: with no
 * marker at all, or a tie, the answer is null ("cannot tell").
 */
class LatinLanguageScorer(private val markers: List<LatinMarkers> = DEFAULT_MARKERS) {
    /** The best of [candidates] for [text], null when no candidate has a marker or the first two are tied. */
    fun best(text: String, candidates: Set<Language>): LanguageGuess? {
        val scores = score(text, candidates)
        val ranked = scores.entries.sortedByDescending { it.value }
        val best = ranked.firstOrNull()
        if (best == null || best.value < MIN_SCORE) {
            return null
        }

        if (ranked.size > 1 && ranked[1].value == best.value) {
            return null
        }

        // The share of the winner among all points, scaled down while there is little evidence.
        val total = scores.values.sum()
        val evidence = minOf(1.0, best.value / CONFIDENT_SCORE)
        return LanguageGuess(best.key, best.value / total * evidence)
    }

    /** Points per candidate that the scorer knows; candidates without markers are left out. */
    fun score(text: String, candidates: Set<Language>): Map<Language, Double> {
        val relevant = markers.filter { it.language in candidates }
        val tokens = tokensOf(text)
        val lower = text.lowercase()
        return relevant.associate { marker -> marker.language to pointsOf(marker, tokens, lower) }
    }

    private fun pointsOf(marker: LatinMarkers, tokens: List<String>, lowerText: String): Double {
        var points = 0.0
        for (token in tokens) {
            points += pointsOfToken(marker, token)
        }

        points += lowerText.count { it in marker.letters }
        return points
    }

    private fun pointsOfToken(marker: LatinMarkers, token: String): Double {
        val apostrophe = token.indexOf(APOSTROPHE)
        if (apostrophe < 0) {
            return if (token in marker.words) 1.0 else 0.0
        }

        // "l'homme" is the elision l' plus a word; "don't" is a word plus a contraction.
        val head = token.substring(0, apostrophe + 1)
        val tail = token.substring(apostrophe + 1)
        var points = 0.0
        if (head in marker.elisions) {
            points += 1.0
        }

        if (tail in marker.contractions) {
            points += 1.0
        }

        if (tail in marker.words) {
            points += 1.0
        }

        return points
    }

    /** Lower-case words: runs of letters, with apostrophes (also the typographic one) kept inside a word. */
    private fun tokensOf(text: String): List<String> {
        val tokens = ArrayList<String>()
        val current = StringBuilder()
        for (character in text.lowercase()) {
            if (character.isLetter() || (current.isNotEmpty() && isApostrophe(character))) {
                current.append(if (isApostrophe(character)) APOSTROPHE else character)
                continue
            }

            if (current.isNotEmpty()) {
                tokens.add(current.toString().trimEnd(APOSTROPHE))
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString().trimEnd(APOSTROPHE))
        }

        return tokens.filter { it.isNotEmpty() }
    }

    private fun isApostrophe(character: Char): Boolean = character == APOSTROPHE || character == TYPOGRAPHIC_APOSTROPHE

    companion object {
        private const val APOSTROPHE = '\''
        private const val TYPOGRAPHIC_APOSTROPHE = '’'

        /** Fewest points the winner needs. */
        private const val MIN_SCORE = 1.0

        /** Points at which the evidence counts as full for the confidence. */
        private const val CONFIDENT_SCORE = 4.0

        val DEFAULT_MARKERS: List<LatinMarkers> = listOf(LatinMarkers.English, LatinMarkers.German, LatinMarkers.French, LatinMarkers.Spanish)
    }
}

package com.glyphdraft.engine.language

import com.glyphdraft.core.Language
import com.glyphdraft.core.LanguageDetector
import com.glyphdraft.core.LanguageGuess
import com.glyphdraft.core.Script

/**
 * Tells the language of a text in two steps, with no model file: the script of its letters ([TextScripts]) narrows the
 * candidates to the languages written in it, and when several are left (Latin script: English, German, French,
 * Spanish) [LatinLanguageScorer] chooses among them. This is what the languages of the picker need: Cyrillic, Hebrew
 * and Japanese each have one candidate, and Han characters without kana are Chinese.
 *
 * Why not Lingua (decision 0005 named it): its only release on Maven Central is a single 80 MB jar with the models of
 * 75 languages, of which this app can read four, and it is unmaintained since 2022 (decision 0009).
 */
class TextLanguageDetector(private val latin: LatinLanguageScorer = LatinLanguageScorer()) : LanguageDetector {
    override suspend fun detect(text: String, candidates: Set<Language>): LanguageGuess? {
        val profile = TextScripts.profile(text)
        val script = profile.dominant
        if (script == null) {
            return null
        }

        val sameScript = candidatesWritten(script, candidates)
        if (sameScript.isEmpty()) {
            return null
        }

        val share = profile.share(script)
        if (sameScript.size == 1) {
            return LanguageGuess(sameScript.first(), share)
        }

        val guess = latin.best(text, sameScript.toSet())
        if (guess == null) {
            return null
        }

        return LanguageGuess(guess.language, guess.confidence * share)
    }

    /**
     * The candidates written in [script]. Han characters without kana are Chinese, but a Japanese text can be kanji
     * only (a sign, a title), so when no Chinese can be read the Japanese candidates fit.
     */
    private fun candidatesWritten(script: Script, candidates: Set<Language>): List<Language> {
        val exact = candidates.filter { it.script == script }
        if (exact.isNotEmpty() || script != Script.HAN) {
            return exact
        }

        return candidates.filter { it.script == Script.JAPANESE }
    }
}

package com.glyphdraft.core.stub

import com.glyphdraft.core.Language
import com.glyphdraft.core.LanguageDetector
import com.glyphdraft.core.LanguageGuess

/** Placeholder detector: blank text is unknown, anything else is [language] if it is among the candidates. */
class StubLanguageDetector(private val language: Language = Language.English) : LanguageDetector {
    override suspend fun detect(text: String, candidates: Set<Language>): LanguageGuess? {
        if (text.isBlank()) {
            return null
        }

        if (language !in candidates) {
            return null
        }

        return LanguageGuess(language, confidence = 1.0)
    }
}

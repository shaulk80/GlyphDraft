package com.glyphdraft.core

/** A language the detector believes a text is written in. [confidence] is between 0 and 1. */
data class LanguageGuess(val language: Language, val confidence: Double)

/** Tells which language a text is written in. Implementations are chosen in the composition root. */
interface LanguageDetector {
    /**
     * Most likely language of [text], chosen only among [candidates] (the languages whose models are installed).
     * Null means "cannot tell": the text is blank, too short, or none of the candidates fits.
     * The call is cancellable; implementations move heavy work off the calling thread.
     */
    suspend fun detect(text: String, candidates: Set<Language>): LanguageGuess?
}

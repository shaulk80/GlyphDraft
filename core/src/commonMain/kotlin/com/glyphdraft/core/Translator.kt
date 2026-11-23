package com.glyphdraft.core

/** One translation model: text of [from] in, text of [to] out. A model works in one direction only (decision 0012). */
data class TranslationDirection(val from: Language, val to: Language)

/**
 * Translated text. [route] lists the languages the text went through, source first and target last; a pair without a
 * direct model has the pivot languages in between (de, en, ru, he), a direct pair has just two, and a text that needed
 * no translation (source equals target) has one.
 */
data class Translation(
    val text: String,
    val from: Language,
    val to: Language,
    val route: List<Language>,
) {
    /** The pivot languages between source and target; empty for a direct pair. The UI shows them as "via English". */
    val via: List<Language> get() = route.drop(1).dropLast(1)
}

/** Translates text between languages. Implementations are chosen in the composition root; the UI only sees this interface. */
interface Translator {
    /**
     * Translates [text] from [from] to [to].
     * A text that is blank, a pair without a route, a model that is not installed and similar expected problems come
     * back as [Outcome.Failure] with a message for the user; callers check the catalog first to offer the download.
     * When [from] equals [to] the text comes back unchanged. The call is cancellable and does its heavy work off the
     * calling thread.
     */
    suspend fun translate(text: String, from: Language, to: Language): Outcome<Translation>
}

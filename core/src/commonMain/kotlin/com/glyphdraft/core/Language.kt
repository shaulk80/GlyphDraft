package com.glyphdraft.core

/**
 * The writing system a language is set in, as far as choosing a recognizer and telling languages apart needs it.
 * [HAN] is Chinese writing; [JAPANESE] is Han characters mixed with kana, so a text with kana is Japanese.
 */
enum class Script { LATIN, CYRILLIC, HEBREW, HAN, JAPANESE }

/** A language the user can pick or that an engine detects. [code] is an ISO 639-1 tag, e.g. "en". */
data class Language(val code: String, val displayName: String, val script: Script) {
    companion object {
        val English = Language("en", "English", Script.LATIN)
        val Russian = Language("ru", "Russian", Script.CYRILLIC)
        val Hebrew = Language("he", "Hebrew", Script.HEBREW)
        val German = Language("de", "German", Script.LATIN)
        val French = Language("fr", "French", Script.LATIN)
        val Spanish = Language("es", "Spanish", Script.LATIN)
        val Chinese = Language("zh", "Chinese", Script.HAN)
        val Japanese = Language("ja", "Japanese", Script.JAPANESE)

        /** Languages offered in the pickers. M2 task 8 replaces this with [ModelCatalog.knownLanguages]. */
        val Supported: List<Language> =
            listOf(English, Russian, Hebrew, German, French, Spanish, Chinese, Japanese)
    }
}

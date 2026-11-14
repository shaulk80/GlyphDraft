package com.glyphdraft.core

/** A language the user can pick or that an engine detects. [code] is an ISO 639-1 tag, e.g. "en". */
data class Language(val code: String, val displayName: String) {
    companion object {
        val English = Language("en", "English")
        val Russian = Language("ru", "Russian")
        val Hebrew = Language("he", "Hebrew")
        val German = Language("de", "German")
        val French = Language("fr", "French")
        val Spanish = Language("es", "Spanish")
        val Chinese = Language("zh", "Chinese")
        val Japanese = Language("ja", "Japanese")

        /** Languages offered in the pickers. M2 replaces this with what the installed models support. */
        val Supported: List<Language> =
            listOf(English, Russian, Hebrew, German, French, Spanish, Chinese, Japanese)
    }
}

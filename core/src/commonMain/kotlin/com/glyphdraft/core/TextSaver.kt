package com.glyphdraft.core

/** Saves text to a file the user chooses through the platform's own dialog. Implemented by the app modules. */
interface TextSaver {
    /** Success with false means the user cancelled; failure means the file could not be written. */
    suspend fun save(suggestedName: String, text: String): Outcome<Boolean>
}

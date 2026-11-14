package com.glyphdraft.core

/** Asks the user for an image through the platform's own picker. Implemented by the app modules. */
interface ImagePicker {
    /** Success with null means the user cancelled; failure means the file could not be read. */
    suspend fun pick(): Outcome<ImageInput?>
}

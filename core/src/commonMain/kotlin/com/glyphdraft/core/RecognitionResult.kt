package com.glyphdraft.core

/**
 * Text read from an image. [language] is the language of the text: the hint when one was given, else the detected one.
 * [lowQuality] is true when the engine doubts the text, typically because the models of the page's language are not
 * installed or another language was chosen: the screen then tells the user so instead of showing the text as a result.
 */
data class RecognitionResult(
    val text: String,
    val language: Language,
    val detected: Boolean,
    val lowQuality: Boolean = false,
)

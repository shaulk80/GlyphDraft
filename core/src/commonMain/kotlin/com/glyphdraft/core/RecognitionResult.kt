package com.glyphdraft.core

/**
 * Text read from an image. [text] keeps the lines of the image, with an empty line where the image has a gap between
 * blocks. [runningText] is the same text with the lines that the page wrapped joined, so that every line break left in
 * it is meant: it is what gets translated (decision 0021). [language] is the language of the text: the hint when one was
 * given, else the detected one. [lowQuality] is true when the engine doubts the text, typically because the models of the
 * page's language are not installed or another language was chosen: the screen then tells the user so instead of showing
 * the text as a result.
 */
data class RecognitionResult(
    val text: String,
    val language: Language,
    val detected: Boolean,
    val lowQuality: Boolean = false,
    val runningText: String = text,
)

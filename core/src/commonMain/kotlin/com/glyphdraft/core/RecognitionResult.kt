package com.glyphdraft.core

/** Text read from an image. [language] is the language of the text: the hint when one was given, else the detected one. */
data class RecognitionResult(val text: String, val language: Language, val detected: Boolean)

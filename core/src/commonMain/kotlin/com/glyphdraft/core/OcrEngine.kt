package com.glyphdraft.core

/** Reads text from an image. Implementations are chosen in the composition root; the UI only sees this interface. */
interface OcrEngine {
    /** [languageHint] null means "detect the language". Failure is returned as a value; the call is cancellable. */
    suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult>
}

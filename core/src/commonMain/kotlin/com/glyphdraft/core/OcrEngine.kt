package com.glyphdraft.core

/** Reads text from an image. Implementations are chosen in the composition root; the UI only sees this interface. */
interface OcrEngine {
    /**
     * Reads the text of [image]. [languageHint] null means "detect the language" among the installed ones
     * ([ModelCatalog.usableLanguages]); the result then has `detected = true`.
     * A hint whose model is not installed, an unreadable image and similar expected problems come back as
     * [Outcome.Failure] with a message for the user; callers check the catalog first to offer the download.
     * The call is cancellable and does its heavy work off the calling thread.
     */
    suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult>
}

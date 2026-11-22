package com.glyphdraft.core

import kotlin.time.TimeSource

/**
 * Passes every call to [inner] and reports how it went to [log]: one line per recognition, with the file name, the
 * language, the number of characters and the time, and the message and cause of a failure. The text itself is never
 * logged. The app composition roots wrap their engine in this, so a failure that the user sees as a short notice is
 * also in the log file with its exception.
 */
class LoggingOcrEngine(
    private val inner: OcrEngine,
    private val log: (message: String, cause: Throwable?) -> Unit,
) : OcrEngine {
    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> {
        val started = TimeSource.Monotonic.markNow()
        val outcome = inner.recognize(image, languageHint)
        val millis = started.elapsedNow().inWholeMilliseconds
        val request = "'${image.name}' (language ${languageHint?.code ?: "not given"})"
        when (outcome) {
            is Outcome.Success -> {
                val result = outcome.value
                log("Recognized $request: ${result.language.code}, detected=${result.detected}, ${result.text.length} characters, lowQuality=${result.lowQuality}, $millis ms", null)
            }
            is Outcome.Failure -> log("Recognition of $request failed after $millis ms: ${outcome.message}", outcome.cause)
        }

        return outcome
    }
}

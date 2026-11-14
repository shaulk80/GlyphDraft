package com.glyphdraft.core.stub

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import kotlinx.coroutines.delay

/** Placeholder engine for the clickable skeleton (M1): ignores the pixels and returns fixed text after a short delay. */
class StubOcrEngine(private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS) : OcrEngine {
    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> {
        delay(latencyMillis)
        if (image.bytes.isEmpty()) {
            return Outcome.Failure("The image '${image.name}' is empty.")
        }

        val language = languageHint ?: DETECTED_LANGUAGE
        val result = RecognitionResult(text = sampleText(image.name), language = language, detected = languageHint == null)
        return Outcome.Success(result)
    }

    private fun sampleText(imageName: String): String =
        "Sample text from $imageName.\nThis is fixed output of the stub engine; real recognition arrives in M2."

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 800L
        val DETECTED_LANGUAGE: Language = Language.English
    }
}

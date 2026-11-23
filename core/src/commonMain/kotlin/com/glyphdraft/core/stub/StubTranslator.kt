package com.glyphdraft.core.stub

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Translation
import com.glyphdraft.core.Translator
import kotlinx.coroutines.delay

/** Placeholder translator for the UI work and tests: marks the text with the target code instead of translating it. */
class StubTranslator(private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS) : Translator {
    override suspend fun translate(text: String, from: Language, to: Language): Outcome<Translation> {
        delay(latencyMillis)
        if (text.isBlank()) {
            return Outcome.Failure("There is no text to translate.")
        }

        if (from == to) {
            return Outcome.Success(Translation(text, from, to, listOf(from)))
        }

        return Outcome.Success(Translation("[${to.code}] $text", from, to, listOf(from, to)))
    }

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 400L
    }
}

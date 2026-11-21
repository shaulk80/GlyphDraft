package com.glyphdraft.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LoggingOcrEngineTest {
    private val image = ImageInput("page.png", byteArrayOf(1))
    private val cause = IllegalStateException("boom")

    @Test
    fun returnsTheOutcomeOfTheInnerEngineUnchanged() =
        runTest {
            val success = Outcome.Success(RecognitionResult("text", Language.English, detected = true))
            val failure = Outcome.Failure("no model", cause)

            for (expected in listOf(success, failure)) {
                val engine = LoggingOcrEngine(FixedEngine(expected)) { _, _ -> }

                assertSame(expected, engine.recognize(image, null))
            }
        }

    @Test
    fun handsTheCauseOfAFailureToTheLog() =
        runTest {
            val causes = ArrayList<Throwable?>()
            val engine = LoggingOcrEngine(FixedEngine(Outcome.Failure("no model", cause))) { _, logged -> causes.add(logged) }

            engine.recognize(image, Language.German)

            assertEquals(listOf<Throwable?>(cause), causes)
        }

    private class FixedEngine(private val outcome: Outcome<RecognitionResult>) : OcrEngine {
        override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> = outcome
    }
}

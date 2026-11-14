package com.glyphdraft.core.stub

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StubOcrEngineTest {
    private val engine = StubOcrEngine(latencyMillis = 0)
    private val image = ImageInput("page.png", byteArrayOf(1, 2, 3))

    @Test
    fun usesHintOrDetectedLanguage() =
        runTest {
            val cases: List<Triple<Language?, Language, Boolean>> =
                listOf(
                    Triple(null, StubOcrEngine.DETECTED_LANGUAGE, true),
                    Triple(Language.Russian, Language.Russian, false),
                )

            for ((hint, expectedLanguage, expectedDetected) in cases) {
                val outcome = engine.recognize(image, hint)

                val result = assertIs<Outcome.Success<RecognitionResult>>(outcome).value
                assertEquals(expectedLanguage, result.language)
                assertEquals(expectedDetected, result.detected)
                assertTrue(result.text.contains("page.png"))
            }
        }

    @Test
    fun emptyImageIsAFailureNotAnException() =
        runTest {
            val outcome = engine.recognize(ImageInput("empty.png", ByteArray(0)), null)

            assertIs<Outcome.Failure>(outcome)
        }
}

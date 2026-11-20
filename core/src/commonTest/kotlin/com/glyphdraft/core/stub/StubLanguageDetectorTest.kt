package com.glyphdraft.core.stub

import com.glyphdraft.core.Language
import com.glyphdraft.core.LanguageGuess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StubLanguageDetectorTest {
    private val detector = StubLanguageDetector(Language.English)

    @Test
    fun answersOnlyForNonBlankTextAndKnownCandidates() =
        runTest {
            val english = setOf(Language.English, Language.German)
            val cases: List<Triple<String, Set<Language>, LanguageGuess?>> =
                listOf(
                    Triple("hello", english, LanguageGuess(Language.English, 1.0)),
                    Triple("   ", english, null),
                    Triple("", english, null),
                    Triple("hello", setOf(Language.Russian), null),
                    Triple("hello", emptySet(), null),
                )

            for ((text, candidates, expected) in cases) {
                assertEquals(expected, detector.detect(text, candidates), "text='$text', candidates=$candidates")
            }
        }
}

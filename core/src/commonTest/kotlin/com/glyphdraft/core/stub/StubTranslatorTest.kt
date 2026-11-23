package com.glyphdraft.core.stub

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Translation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class StubTranslatorTest {
    private val translator = StubTranslator(latencyMillis = 0)

    @Test
    fun translatesToTheTargetWithAMarkAndADirectRoute() =
        runTest {
            val outcome = translator.translate("Guten Tag", Language.German, Language.English)

            val expected = Translation("[en] Guten Tag", Language.German, Language.English, listOf(Language.German, Language.English))
            assertEquals(Outcome.Success(expected), outcome)
        }

    @Test
    fun sameLanguageComesBackUnchangedWithASingleStepRoute() =
        runTest {
            val outcome = translator.translate("Hello", Language.English, Language.English)

            assertEquals(Outcome.Success(Translation("Hello", Language.English, Language.English, listOf(Language.English))), outcome)
        }

    @Test
    fun blankTextIsAFailure() =
        runTest {
            for (text in listOf("", "   ", "\n\t")) {
                assertTrue(translator.translate(text, Language.German, Language.English) is Outcome.Failure, "text '$text'")
            }
        }

    @Test
    fun viaListsOnlyThePivotLanguages() {
        val cases =
            listOf(
                listOf(Language.English) to emptyList(),
                listOf(Language.German, Language.English) to emptyList(),
                listOf(Language.German, Language.English, Language.Spanish) to listOf(Language.English),
                listOf(Language.German, Language.English, Language.Russian, Language.Hebrew) to listOf(Language.English, Language.Russian),
            )

        for ((route, expected) in cases) {
            val translation = Translation("x", route.first(), route.last(), route)
            assertEquals(expected, translation.via, "route=$route")
        }
    }
}

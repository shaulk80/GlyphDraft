package com.glyphdraft.ui.state

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.TranslationDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun modelOf(from: Language, to: Language, experimental: Boolean = false): ModelPack =
    ModelPack("mt-${from.code}-${to.code}", "${from.displayName} to ${to.displayName}", emptySet(), 1L, TranslationDirection(from, to), experimental)

private val englishToGerman = modelOf(Language.English, Language.German)
private val germanToEnglish = modelOf(Language.German, Language.English)
private val englishToRussian = modelOf(Language.English, Language.Russian)
private val russianToHebrew = modelOf(Language.Russian, Language.Hebrew, experimental = true)

class RouteInfoTest {
    private val state =
        AppState(
            packs = listOf(englishToGerman, germanToEnglish, englishToRussian, russianToHebrew),
            installedPackIds = setOf(englishToGerman.id, germanToEnglish.id),
        )

    @Test
    fun theLabelOfATargetTellsHowThePairIsTranslated() {
        val cases =
            listOf(
                Triple(Language.English, Language.German, "German"),
                Triple(Language.English, Language.Russian, "Russian (download needed)"),
                Triple(Language.English, Language.Hebrew, "Hebrew (via Russian, download needed, experimental)"),
                Triple(Language.German, Language.Russian, "Russian (via English, download needed)"),
                Triple(Language.German, Language.Hebrew, "Hebrew (via English and Russian, download needed, experimental)"),
                Triple(Language.Hebrew, Language.German, "German (not available)"),
                Triple(Language.German, Language.German, "German"),
            )

        for ((source, target, label) in cases) {
            assertEquals(label, state.targetLabel(target, source), "${source.code} to ${target.code}")
        }
    }

    @Test
    fun withoutAKnownSourceOnlyTheNameShows() {
        assertEquals("Hebrew", state.targetLabel(Language.Hebrew, source = null))
    }

    @Test
    fun theRouteInfoListsTheMissingModelsInOrderOfUse() {
        val info = state.routeInfo(Language.German, Language.Hebrew)

        assertEquals(listOf(Language.English, Language.Russian), info?.via)
        assertEquals(listOf(englishToRussian, russianToHebrew), info?.missing)
        assertEquals(true, info?.experimental)
    }

    @Test
    fun aPairWithoutARouteHasNoInfo() {
        assertNull(state.routeInfo(Language.Hebrew, Language.German))
    }
}

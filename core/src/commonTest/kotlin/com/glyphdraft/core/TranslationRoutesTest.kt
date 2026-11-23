package com.glyphdraft.core

import com.glyphdraft.core.stub.StubModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationRoutesTest {
    private val en = Language.English
    private val de = Language.German
    private val fr = Language.French
    private val ru = Language.Russian
    private val he = Language.Hebrew
    private val ja = Language.Japanese
    private val zh = Language.Chinese

    /** The shape of the shipping set (decision 0012): English is the hub, Hebrew is reached through Russian. */
    private val hubs =
        listOf(en to de, de to en, en to fr, fr to en, en to ru, ru to en, en to zh, zh to en, ja to en, he to en, ru to he)
            .map { (from, to) -> TranslationDirection(from, to) }

    private val routes = TranslationRoutes(hubs)

    @Test
    fun aPairRunsThroughTheHubsByTheShortestChain() {
        val cases =
            listOf(
                Triple(en, de, listOf(en, de)),
                Triple(ru, he, listOf(ru, he)),
                Triple(de, fr, listOf(de, en, fr)),
                Triple(ja, zh, listOf(ja, en, zh)),
                Triple(he, de, listOf(he, en, de)),
                Triple(he, ru, listOf(he, en, ru)),
                Triple(en, he, listOf(en, ru, he)),
                Triple(de, he, listOf(de, en, ru, he)),
                Triple(ja, he, listOf(ja, en, ru, he)),
                Triple(de, de, listOf(de)),
                Triple(he, he, listOf(he)),
            )

        for ((from, to, expected) in cases) {
            assertEquals(expected, routes.route(from, to), "${from.code} to ${to.code}")
        }
    }

    @Test
    fun aPairNoChainReachesHasNoRoute() {
        val cases = listOf(en to ja, de to ja, he to ja, en to Language.Spanish, Language.Spanish to en)

        for ((from, to) in cases) {
            assertNull(routes.route(from, to), "${from.code} to ${to.code}")
            assertNull(routes.steps(from, to), "${from.code} to ${to.code}")
        }
    }

    @Test
    fun stepsAreTheDirectionsToRunInOrder() {
        assertEquals(
            listOf(TranslationDirection(de, en), TranslationDirection(en, ru), TranslationDirection(ru, he)),
            routes.steps(de, he),
        )
        assertEquals(emptyList(), routes.steps(de, de))
    }

    @Test
    fun sourcesAndTargetsFollowTheDirections() {
        assertEquals(setOf(en, de, fr, ru, zh, ja, he), routes.sources)
        assertEquals(setOf(en, de, fr, ru, zh, he), routes.targets)
        assertEquals(setOf(en, fr, ru, zh, he), routes.targetsFrom(de))
        assertEquals(setOf(en, de, fr, ru, zh, he), routes.targetsFrom(ja))
        assertEquals(emptySet(), routes.targetsFrom(Language.Spanish))
    }

    @Test
    fun ofTwoChainsOfTheSameLengthTheFirstListedWins() {
        val a = Language("a", "A", Script.LATIN)
        val b = Language("b", "B", Script.LATIN)
        val c = Language("c", "C", Script.LATIN)
        val d = Language("d", "D", Script.LATIN)
        val ab = TranslationDirection(a, b)
        val ac = TranslationDirection(a, c)
        val bd = TranslationDirection(b, d)
        val cd = TranslationDirection(c, d)

        assertEquals(listOf(a, b, d), TranslationRoutes(listOf(ab, ac, bd, cd)).route(a, d))
        assertEquals(listOf(a, c, d), TranslationRoutes(listOf(ac, ab, cd, bd)).route(a, d))
    }

    @Test
    fun aCycleDoesNotLoop() {
        val cycle = listOf(TranslationDirection(en, de), TranslationDirection(de, en), TranslationDirection(de, fr))

        assertEquals(listOf(en, de, fr), TranslationRoutes(cycle).route(en, fr))
        assertNull(TranslationRoutes(cycle).route(en, ru))
    }

    @Test
    fun theCatalogNamesTheModelsOfARoute() {
        val packs = hubs.map { ModelPack("${it.from.code}-${it.to.code}", "${it.from.code} to ${it.to.code}", emptySet(), 100, it) }
        val catalog = StubModelCatalog(packs = packs, installed = setOf("de-en", "en-ru"))

        val route = catalog.packsForRoute(de, he)

        assertEquals(listOf("de-en", "en-ru", "ru-he"), route?.map { it.id })
        assertEquals(listOf("ru-he"), route?.filterNot { catalog.isInstalled(it) }?.map { it.id })
        assertEquals(emptyList(), catalog.packsForRoute(de, de))
        assertNull(catalog.packsForRoute(en, ja))
        assertTrue(catalog.translationRoutes().targets.contains(he))
    }
}

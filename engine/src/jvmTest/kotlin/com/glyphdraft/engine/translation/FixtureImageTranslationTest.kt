package com.glyphdraft.engine.translation

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.packsForRoute
import com.glyphdraft.core.translationRoutes
import com.glyphdraft.engine.OnDeviceEngine
import com.glyphdraft.engine.models.ModelManifest
import com.glyphdraft.engine.ocr.AwtImageDecoder
import com.glyphdraft.engine.ocr.TestModels
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * An image to a translation with the real models, through the factory the apps use ([OnDeviceEngine]): the engine reads the
 * fixture without being told the language, and the language it found goes to the translator with the target. Latin and
 * non-Latin sources, routes of one, two and three models, Hebrew in and out.
 *
 * There is no hand translation of the fixture texts, so the figure is a consistency one: chrF of the translation of what was
 * read against the translation of the exact text of the fixture (`.txt`) by the same models. It measures what a reading
 * mistake costs the translation, not the quality of the translation (that is [FixtureTranslationTest]). A case needs the
 * reading pack and every model of its route; one that is not installed is left out and says so.
 *
 * On CI the cases of [REQUIRED] run (English, German and French, the directions [FixtureTranslationTest] downloads); with more
 * models in the models folder (see [TestModels]) the others run as well.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureImageTranslationTest*"`
 */
class FixtureImageTranslationTest {
    /** Fixture, the language it is in, the target, the lowest chrF against the translation of the exact text. */
    private val cases =
        listOf(
            Case("en-clean", Language.English, Language.German),
            Case("en-skew", Language.English, Language.German),
            Case("de-clean", Language.German, Language.English),
            Case("de-blur", Language.German, Language.English),
            Case("de-clean", Language.German, Language.French),
            Case("ru-clean", Language.Russian, Language.English),
            Case("ru-noise", Language.Russian, Language.German),
            Case("zh-clean", Language.Chinese, Language.English),
            Case("zh-noise", Language.Chinese, Language.English),
            Case("ja-clean", Language.Japanese, Language.English),
            Case("ja-clean", Language.Japanese, Language.German),
            Case("he-clean", Language.Hebrew, Language.English),
            Case("he-clean", Language.Hebrew, Language.German),
            Case("ru-clean", Language.Russian, Language.Hebrew),
            Case("en-clean", Language.English, Language.Hebrew),
            Case("de-clean", Language.German, Language.Hebrew),
        )

    @Test
    fun anImageIsReadAndTranslatedAsTheExactTextWouldBe() {
        val readingPacks = setOf(ModelManifest.LATIN, ModelManifest.ESLAV, ModelManifest.CJK, ModelManifest.HEBREW)
        val catalog = TestModels.catalogWith("FixtureImageTranslationTest", readingPacks + REQUIRED) ?: return

        val failures = ArrayList<String>()
        var run = 0
        OnDeviceEngine(TestModels.modelsFolder(), AwtImageDecoder()).use { engine ->
            for (case in cases) {
                val missing = missingModels(catalog, case)
                if (missing.isNotEmpty()) {
                    println("${case.label} not run: $missing not installed.")
                    continue
                }

                run++
                failures.addAll(check(engine, catalog, case))
            }
        }

        println("$run of ${cases.size} cases run.")
        assertTrue(run > 0, "No case could run.")
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun check(engine: OnDeviceEngine, catalog: ModelCatalog, case: Case): List<String> {
        val image = ImageInput("${case.fixture}.png", File(TestModels.fixtures, "${case.fixture}.png").readBytes())
        val started = System.nanoTime()
        val read = runBlocking { engine.engine.recognize(image, null) }
        if (read !is Outcome.Success) {
            return listOf("${case.label}: reading failed: ${(read as Outcome.Failure).message}")
        }

        val recognition = read.value
        if (recognition.language != case.from) {
            return listOf("${case.label}: the engine found ${recognition.language.code}, not ${case.from.code}")
        }

        val translated = runBlocking { engine.translator.translate(recognition.text, recognition.language, case.to) }
        val expected = runBlocking { engine.translator.translate(TestModels.expectedText(case.fixture).trim(), case.from, case.to) }
        if (translated !is Outcome.Success || expected !is Outcome.Success) {
            return listOf("${case.label}: translation failed: $translated / $expected")
        }

        val seconds = (System.nanoTime() - started) / NANOS_PER_SECOND
        val score = Chrf.score(listOf(translated.value.text), listOf(expected.value.text))
        println("${case.label} chrF %.1f against the exact text (route %s, %.1f s)".format(score, routeOf(translated.value.route), seconds))
        println("  read:       ${recognition.text.replace("\n", " / ")}")
        println("  translated: ${translated.value.text.replace("\n", " / ")}")

        val problems = ArrayList<String>()
        val route = catalog.translationRoutes().route(case.from, case.to)
        if (translated.value.route != route) {
            problems.add("${case.label}: route ${routeOf(translated.value.route)}, expected ${route?.let(::routeOf)}")
        }

        if (translated.value.text.isBlank()) {
            problems.add("${case.label}: the translation is empty")
        }

        if (score < MINIMUM_CHRF) {
            problems.add("${case.label}: chrF %.1f is under %.1f. Translated: ${translated.value.text}".format(score, MINIMUM_CHRF))
        }

        return problems
    }

    /** The reading pack of the source is not asked of the catalog here: [TestModels] installs all four or the test does not run. */
    private fun missingModels(catalog: ModelCatalog, case: Case): List<String> {
        val packs = catalog.packsForRoute(case.from, case.to) ?: return listOf("a route")
        return packs.map { it.id }.filter { it !in catalog.installedPackIds.value }
    }

    private fun routeOf(route: List<Language>): String = route.joinToString(">") { it.code }

    private class Case(val fixture: String, val from: Language, val to: Language) {
        val label: String get() = "$fixture ${from.code} to ${to.code}"
    }

    private companion object {
        const val MINIMUM_CHRF = 85.0
        const val NANOS_PER_SECOND = 1_000_000_000.0

        /** What CI downloads for the translation tests; the other directions are measured when they are in the models folder. */
        val REQUIRED = setOf("mt-en-de", "mt-de-en", "mt-en-fr")
    }
}

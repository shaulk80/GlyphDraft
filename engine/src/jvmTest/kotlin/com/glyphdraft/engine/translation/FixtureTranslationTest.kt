package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.packsForRoute
import com.glyphdraft.engine.ocr.TestModels
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Translates the 30 reference sentences (`src/test/resources/translation`) with the real OPUS-MT models through the whole
 * translator (segmenter, tokenizer, route, decode loop) and compares them with the hand translations by chrF.
 *
 * Every direction that is installed is measured and its figure printed next to the one of the spike; a figure more than
 * [WARNING_MARGIN] under it prints a WARNING (nothing fails). A figure under its floor (the spike figure minus
 * [FLOOR_MARGIN]) fails the test: that is a regression of the decode loop or the tokenizer, not a matter of taste.
 *
 * On CI only [REQUIRED] is downloaded (about 350 MB in the cache); with more directions in the models folder (see
 * [TestModels]) they are measured as well, which is how the Hebrew and the other directions were checked.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureTranslationTest*"`
 */
class FixtureTranslationTest {
    /** Directions and pairs with the chrF of the spike (docs/spikes/m3-translation.md); a pair of two languages goes through English (or Russian). */
    private val cases =
        listOf(
            Case(Language.English, Language.German, 81.5),
            Case(Language.German, Language.English, 85.9),
            Case(Language.English, Language.French, 82.4),
            Case(Language.French, Language.English, 79.0),
            Case(Language.English, Language.Spanish, 83.4),
            Case(Language.Spanish, Language.English, 81.9),
            Case(Language.English, Language.Russian, 70.3),
            Case(Language.Russian, Language.English, 72.6),
            Case(Language.English, Language.Chinese, 37.1),
            Case(Language.Chinese, Language.English, 72.9),
            Case(Language.Japanese, Language.English, 72.3),
            // The spike said 82.5 for this one: its tokenizer ended sentences with id 0 instead of the end id 25568 of this model.
            Case(Language.Hebrew, Language.English, 78.9),
            Case(Language.Russian, Language.Hebrew, 68.2),
            Case(Language.German, Language.French, 78.4),
            Case(Language.Russian, Language.German, 74.6),
            Case(Language.Japanese, Language.German, 67.7),
            Case(Language.English, Language.Hebrew, 67.3),
            // Hebrew pairs through the hubs (task 6, decision 0016): measured with the shipped routes, not in the spike.
            Case(Language.Hebrew, Language.German, 75.6),
            Case(Language.Hebrew, Language.Russian, 64.6),
            Case(Language.German, Language.Hebrew, 66.0),
        )

    @Test
    fun everyInstalledDirectionScoresAsInTheSpike() {
        val catalog = TestModels.catalogWith("FixtureTranslationTest", REQUIRED)
        if (catalog == null) {
            return
        }

        val translator = OnDeviceTranslator(catalog, OnnxDirectionSource(catalog))
        val failures = ArrayList<String>()
        translator.use {
            for (case in cases) {
                val needed = catalog.packsForRoute(case.from, case.to).orEmpty().map { it.id }
                val missing = needed.filter { it !in catalog.installedPackIds.value }
                if (missing.isNotEmpty()) {
                    println("${case.label} not measured: $missing not installed.")
                    continue
                }

                failures.addAll(measure(case, translator))
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun measure(case: Case, translator: OnDeviceTranslator): List<String> {
        val sources = lines(case.from.code)
        val references = lines(case.to.code)
        val outcome = runBlocking { translator.translate(sources.joinToString(PARAGRAPH), case.from, case.to) }
        if (outcome is Outcome.Failure) {
            return listOf("${case.label}: ${outcome.message}")
        }

        val translation = (outcome as Outcome.Success).value
        val hypotheses = translation.text.split(PARAGRAPH)
        if (hypotheses.size != references.size) {
            return listOf("${case.label}: ${hypotheses.size} translated paragraphs for ${references.size} sentences")
        }

        writeOutput(case, sources, hypotheses, references)
        val score = Chrf.score(hypotheses, references)
        val note = if (score < case.spike - WARNING_MARGIN) "  WARNING: more than $WARNING_MARGIN under the spike" else ""
        println("${case.label} chrF %.1f (spike %.1f, route %s)%s".format(score, case.spike, translation.route.joinToString(">") { it.code }, note))
        if (note.isNotEmpty()) {
            for (index in hypotheses.indices) {
                println("  [${index + 1}] ${sources[index]}")
                println("      got: ${hypotheses[index]}")
                println("      ref: ${references[index]}")
            }
        }

        if (score < case.spike - FLOOR_MARGIN) {
            return listOf("${case.label}: chrF %.1f is under the floor of %.1f".format(score, case.spike - FLOOR_MARGIN))
        }

        return emptyList()
    }

    /** The sentences of one direction as source, output and reference, in `build/translation-output/<from>-<to>.txt`, for a human read. */
    private fun writeOutput(case: Case, sources: List<String>, hypotheses: List<String>, references: List<String>) {
        val text =
            sources.indices.joinToString("\n\n") { index ->
                "[${index + 1}] ${sources[index]}\nOutput:    ${hypotheses[index]}\nReference: ${references[index]}"
            }
        val file = File(OUTPUT_DIRECTORY, "${case.from.code}-${case.to.code}.txt")
        file.parentFile.mkdirs()
        file.writeText(text + "\n", Charsets.UTF_8)
    }

    private fun lines(code: String): List<String> =
        File(DIRECTORY, "$code.txt").readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }

    private class Case(val from: Language, val to: Language, val spike: Double) {
        val label: String get() = "${from.code} to ${to.code}"
    }

    private companion object {
        val DIRECTORY = File("src/test/resources/translation")
        val OUTPUT_DIRECTORY = File("build/translation-output")
        const val PARAGRAPH = "\n\n"
        const val WARNING_MARGIN = 2.0
        const val FLOOR_MARGIN = 5.0

        /** What CI downloads: both ways of German and English, and French, so that German to French goes through English. */
        val REQUIRED = setOf("mt-en-de", "mt-de-en", "mt-en-fr")
    }
}

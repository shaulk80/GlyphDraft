package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.ModelManifest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Reads the fixture images (`src/test/resources/fixtures`, expected text next to each) with the real PP-OCRv5 models
 * and checks the character error rate against the figures of the spike (docs/spikes/m2-ocr-engines.md).
 *
 * The models are about 45 MB, so the test needs them on disk (see [TestModels]); without them it does nothing, and
 * says so on stdout.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureOcrTest*"`
 */
class FixtureOcrTest {
    /** Fixture name, pack that reads it, largest accepted error rate. Hebrew is Tesseract's (task 7). */
    private val cases =
        listOf(
            Case("en-clean", ModelManifest.LATIN, 0.02),
            Case("en-skew", ModelManifest.LATIN, 0.02),
            Case("de-clean", ModelManifest.LATIN, 0.02),
            Case("de-blur", ModelManifest.LATIN, 0.02),
            Case("ru-clean", ModelManifest.ESLAV, 0.02),
            Case("ru-noise", ModelManifest.ESLAV, 0.02),
            Case("zh-clean", ModelManifest.CJK, 0.02),
            Case("zh-noise", ModelManifest.CJK, 0.02),
            Case("ja-clean", ModelManifest.CJK, 0.02),
            Case("mixed-clean", ModelManifest.ESLAV, 0.05),
        )

    @Test
    fun fixturesAreReadWithinTheErrorRatesOfTheSpike() {
        val packs = cases.map { it.pack }.toSet()
        val catalog = TestModels.catalogWith("FixtureOcrTest", packs)
        if (catalog == null) {
            return
        }

        val models = PaddleModels(catalog)
        val detector = (models.openDetector() as Outcome.Success).value
        val failures = ArrayList<String>()
        detector.use {
            val ocr = PaddleOcr(detector)
            for (pack in packs) {
                val recognizer = (models.openRecognizer(pack) as Outcome.Success).value
                recognizer.use {
                    for (case in cases.filter { it.pack == pack }) {
                        failures.addAll(check(case, ocr, recognizer))
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun check(case: Case, ocr: PaddleOcr, recognizer: TextRecognizer): List<String> {
        val image = TestModels.decodeFixture(case.name)
        val expected = TestModels.expectedText(case.name)

        val outcome = runBlocking { ocr.read(image, recognizer) }

        if (outcome is Outcome.Failure) {
            return listOf("${case.name}: ${outcome.message}")
        }

        val text = (outcome as Outcome.Success).value.text
        val rate = Cer.of(expected, text)
        println("${case.name}: CER ${"%.1f".format(rate * 100)} %")
        if (rate > case.maxRate) {
            return listOf("${case.name}: CER ${rate * 100} % is above ${case.maxRate * 100} %. Read: $text")
        }

        return emptyList()
    }

    private class Case(val name: String, val pack: String, val maxRate: Double)
}

package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.HttpModelInstaller
import com.glyphdraft.engine.models.ModelManifest
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Reads the fixture images (`src/test/resources/fixtures`, expected text next to each) with the real PP-OCRv5 models
 * and checks the character error rate against the figures of the spike (docs/spikes/m2-ocr-engines.md).
 *
 * The models are about 45 MB, so the test needs them on disk: in `GLYPHDRAFT_MODELS_DIR`, or by default in
 * `engine/build/test-models`. With `GLYPHDRAFT_NETWORK_TESTS=1` the missing packs are downloaded there first (checked
 * by the pinned checksums); without models and without that variable the test does nothing, and says so on stdout.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureOcrTest*"`
 */
class FixtureOcrTest {
    private val fixtures = File("src/test/resources/fixtures")

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
        val catalog = FileModelCatalog(ModelManifest.packs, modelsFolder())
        val packs = cases.map { it.pack }.toSet()
        if (!prepare(catalog, packs)) {
            println("FixtureOcrTest skipped: models are not installed (set GLYPHDRAFT_NETWORK_TESTS=1 to download them).")
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
        val image = (AwtImageDecoder().decode(File(fixtures, "${case.name}.png").readBytes()) as Outcome.Success).value
        val expected = File(fixtures, "${case.name}.txt").readText(Charsets.UTF_8)

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

    /** True when every pack in [packs] is installed (downloading them first when the environment allows). */
    private fun prepare(catalog: FileModelCatalog, packs: Set<String>): Boolean {
        val missing = packs.filter { it !in catalog.installedPackIds.value }
        if (missing.isNotEmpty() && System.getenv(NETWORK_VARIABLE).isNullOrBlank()) {
            return false
        }

        val installer = HttpModelInstaller(catalog)
        for (id in missing) {
            val pack = ModelManifest.packs.first { it.pack.id == id }.pack
            val outcome = runBlocking { installer.install(pack) {} }
            assertTrue(outcome is Outcome.Success, "Installing $id failed: $outcome")
        }

        return true
    }

    private fun modelsFolder(): Path {
        val configured = System.getenv(FOLDER_VARIABLE)
        if (!configured.isNullOrBlank()) {
            return File(configured).toPath()
        }

        return File("build/test-models").toPath()
    }

    private class Case(val name: String, val pack: String, val maxRate: Double)

    private companion object {
        const val NETWORK_VARIABLE = "GLYPHDRAFT_NETWORK_TESTS"
        const val FOLDER_VARIABLE = "GLYPHDRAFT_MODELS_DIR"
    }
}

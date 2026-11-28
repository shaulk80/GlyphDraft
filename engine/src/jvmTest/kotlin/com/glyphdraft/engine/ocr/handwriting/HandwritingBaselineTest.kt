package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.ModelManifest
import com.glyphdraft.engine.ocr.AwtImageDecoder
import com.glyphdraft.engine.ocr.OnDeviceOcrEngine
import com.glyphdraft.engine.ocr.PaddleModels
import com.glyphdraft.engine.ocr.PaddleReader
import com.glyphdraft.engine.ocr.TesseractHebrewReader
import com.glyphdraft.engine.ocr.TestModels
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/**
 * Reads the handwriting fixtures with the shipped models, each page in its own language as a user who picks it would,
 * and writes the error rates to `build/reports/handwriting/baseline.md` (scripts/build.ps1 prints it). Accuracy never
 * fails this test (docs/plan.md, M4 task 1); a page or a model that cannot be used does. Needs the models (see
 * [TestModels]); without them it does nothing and removes an older report.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*HandwritingBaselineTest*"`
 */
class HandwritingBaselineTest {
    private val report = File("build/reports/handwriting/baseline.md")

    @Test
    fun theErrorRatesOfTheShippedModelsAreReported() {
        val catalog = TestModels.catalogWith("HandwritingBaselineTest", ModelManifest.packs.map { it.pack.id }.toSet())
        if (catalog == null) {
            report.delete()
            return
        }

        val paddle = PaddleReader(PaddleModels(catalog))
        val engine = OnDeviceOcrEngine(AwtImageDecoder(), catalog, paddle, TextLanguageDetector(), TesseractHebrewReader(catalog))
        val scores = engine.use { open -> HandwritingPages.load().map { score(open, it) } }

        val markdown = BaselineReport.markdown(scores)
        report.parentFile.mkdirs()
        report.writeText(markdown, Charsets.UTF_8)
        println(markdown)
    }

    private fun score(engine: OnDeviceOcrEngine, page: HandwritingPage): PageScore {
        val image = ImageInput(page.image.name, page.image.readBytes())
        val expected = page.expected.readText(Charsets.UTF_8)

        fun scored(read: String, note: String): PageScore =
            PageScore(page, HandwritingScore.errorRate(expected, read, page.letterCase), note)

        return when (val outcome = runBlocking { engine.recognize(image, page.language) }) {
            is Outcome.Success -> {
                val note = if (outcome.value.lowQuality) "marked low quality" else ""
                scored(outcome.value.text, note)
            }
            is Outcome.Failure -> {
                // Finding no text is a reading result (everything is missed); any other failure is a broken run.
                assertEquals(OnDeviceOcrEngine.NO_TEXT, outcome.message, "${page.name} could not be read")
                scored("", outcome.message)
            }
        }
    }
}

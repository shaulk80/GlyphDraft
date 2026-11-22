package com.glyphdraft.engine.ocr

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.stub.StubModelCatalog
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.ModelManifest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * The whole engine on the fixtures with the real models: no language given (the engine chooses the recognizer from the
 * image and tells the language from the text it read) and the right language given. Hebrew goes through Tesseract, the
 * rest through PP-OCRv5. Needs the models on disk like [FixtureOcrTest] (see [TestModels]) and does nothing without.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureEngineTest*"`
 */
class FixtureEngineTest {
    /** Fixture name, its language, largest accepted error rate. The mixed page is mostly Russian. */
    private val cases =
        listOf(
            Case("en-clean", Language.English, 0.02),
            Case("en-skew", Language.English, 0.02),
            Case("de-clean", Language.German, 0.02),
            Case("de-blur", Language.German, 0.02),
            Case("ru-clean", Language.Russian, 0.02),
            Case("ru-noise", Language.Russian, 0.02),
            Case("zh-clean", Language.Chinese, 0.02),
            Case("zh-noise", Language.Chinese, 0.02),
            Case("ja-clean", Language.Japanese, 0.02),
            Case("mixed-clean", Language.Russian, 0.05),
            Case("he-clean", Language.Hebrew, 0.02),
        )

    @Test
    fun theEngineReadsEveryFixtureWithAndWithoutALanguage() {
        val packs = setOf(ModelManifest.LATIN, ModelManifest.ESLAV, ModelManifest.CJK, ModelManifest.HEBREW)
        val catalog = TestModels.catalogWith("FixtureEngineTest", packs)
        if (catalog == null) {
            return
        }

        val problems = ArrayList<String>()
        val paddle = PaddleReader(PaddleModels(catalog))
        OnDeviceOcrEngine(AwtImageDecoder(), catalog, paddle, TextLanguageDetector(), TesseractHebrewReader(catalog)).use { engine ->
            for (case in cases) {
                problems.addAll(check(engine, case, hint = null))
                problems.addAll(check(engine, case, hint = case.language))
            }
        }

        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    /**
     * The mistakes of the M2 hand test (docs/manual/m2.md, findings 5, 6 and 8): a page whose models are missing or a
     * language that does not fit the page. Every one of them has to come back marked as low quality; right readings
     * (above) and a different but readable language (English text read with the Russian models) must not.
     */
    @Test
    fun aWrongPackOrLanguageIsMarkedLowQuality() {
        val catalog = TestModels.catalogWith("FixtureEngineTest", setOf(ModelManifest.LATIN, ModelManifest.ESLAV, ModelManifest.CJK, ModelManifest.HEBREW))
        if (catalog == null) {
            return
        }

        // The models are all on disk, but the engine is told that only the Latin pack is installed.
        val latinOnly = StubModelCatalog(installed = setOf(ModelManifest.LATIN))
        // fixture, language chosen (null: auto-detect), expected low quality, installed packs: everything or Latin only
        val cases =
            listOf(
                Wrong("ru-clean", null, expectLow = true, latinOnly = true),
                Wrong("ru-noise", null, expectLow = true, latinOnly = true),
                Wrong("zh-clean", null, expectLow = true, latinOnly = true),
                Wrong("ja-clean", null, expectLow = true, latinOnly = true),
                Wrong("ru-clean", Language.Hebrew, expectLow = true),
                Wrong("en-clean", Language.Hebrew, expectLow = true),
                Wrong("he-clean", Language.English, expectLow = true),
                Wrong("mixed-clean", Language.English, expectLow = true),
                Wrong("ru-clean", Language.English, expectLow = true),
                Wrong("en-clean", Language.Russian, expectLow = false),
                Wrong("de-clean", Language.English, expectLow = false),
            )

        val problems = ArrayList<String>()
        for (case in cases) {
            val installed: ModelCatalog = if (case.latinOnly) latinOnly else catalog
            val paddle = PaddleReader(PaddleModels(catalog))
            OnDeviceOcrEngine(AwtImageDecoder(), installed, paddle, TextLanguageDetector(), TesseractHebrewReader(catalog)).use { engine ->
                val image = ImageInput("${case.name}.png", File(TestModels.fixtures, "${case.name}.png").readBytes())
                val outcome = runBlocking { engine.recognize(image, case.language) }
                val label = "${case.name} (${case.language?.code ?: "auto"}${if (case.latinOnly) ", Latin only" else ""})"
                println("$label: $outcome")
                if (outcome is Outcome.Success && outcome.value.lowQuality != case.expectLow) {
                    problems.add("$label: lowQuality is ${outcome.value.lowQuality}, expected ${case.expectLow}")
                }
            }
        }

        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    private class Wrong(val name: String, val language: Language?, val expectLow: Boolean, val latinOnly: Boolean = false)

    private fun check(engine: OnDeviceOcrEngine, case: Case, hint: Language?): List<String> {
        val label = "${case.name} (${hint?.code ?: "no language"})"
        val image = ImageInput("${case.name}.png", File(TestModels.fixtures, "${case.name}.png").readBytes())

        val started = System.nanoTime()
        val outcome = runBlocking { engine.recognize(image, hint) }
        val millis = (System.nanoTime() - started) / NANOS_PER_MILLI

        if (outcome is Outcome.Failure) {
            return listOf("$label: ${outcome.message}")
        }

        val result = (outcome as Outcome.Success).value
        val rate = Cer.of(TestModels.expectedText(case.name), result.text)
        println("$label: ${result.language.code}, detected=${result.detected}, CER ${"%.1f".format(rate * 100)} %, $millis ms")

        val problems = ArrayList<String>()
        if (result.language != case.language) {
            problems.add("$label: language ${result.language.code}, expected ${case.language.code}")
        }

        if (result.lowQuality) {
            problems.add("$label: marked as low quality")
        }

        // The error rate ignores whitespace, so the line breaks are compared on their own: one text line per line, no empty ones.
        if (result.text.lines().any { it.isBlank() }) {
            problems.add("$label: the text has an empty line: ${result.text.replace("\n", "<LF>")}")
        }

        if (result.detected != (hint == null)) {
            problems.add("$label: detected flag is ${result.detected}")
        }

        if (rate > case.maxRate) {
            problems.add("$label: CER ${rate * 100} % is above ${case.maxRate * 100} %. Read: ${result.text}")
        }

        return problems
    }

    private class Case(val name: String, val language: Language, val maxRate: Double)

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}

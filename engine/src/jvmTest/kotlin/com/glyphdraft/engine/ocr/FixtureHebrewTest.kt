package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.ModelManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Tesseract `heb` on the fixtures with the real data file (1 MB, see [TestModels]; does nothing without it): the Hebrew
 * page is read at 0.0 % CER and probes above the threshold of the engine, every other page probes below it.
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*FixtureHebrewTest*"`
 */
class FixtureHebrewTest {
    @Test
    fun theHebrewPageIsReadAndProbedAsHebrewAndNoOtherPageIs() {
        val catalog = TestModels.catalogWith("FixtureHebrewTest", setOf(ModelManifest.HEBREW))
        if (catalog == null) {
            return
        }

        val problems = ArrayList<String>()
        TesseractHebrewReader(catalog).use { reader ->
            problems.addAll(checkHebrew(reader))
            problems.addAll(checkOthers(reader))
        }

        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    private fun checkHebrew(reader: HebrewReader): List<String> {
        val page = TestModels.decodeFixture("he-clean")
        val confidence = (runBlocking { reader.probe(page) } as? Outcome.Success)?.value
        val text = (runBlocking { reader.read(page) } as? Outcome.Success)?.value
        println("he-clean: confidence $confidence, read: $text")

        val problems = ArrayList<String>()
        if (confidence == null || confidence < OnDeviceOcrEngine.HEBREW_PROBE_THRESHOLD) {
            problems.add("he-clean: probe $confidence is below ${OnDeviceOcrEngine.HEBREW_PROBE_THRESHOLD}")
        }

        val expected = TestModels.expectedText("he-clean")
        val rate = Cer.of(expected, text.orEmpty())
        assertEquals(0.0, rate, 0.0, "he-clean was read as: $text")
        // The error rate ignores whitespace; the line breaks are checked here (Tesseract once added an empty line).
        assertEquals(expected.trim().lines(), text.orEmpty().lines(), "he-clean lines")
        return problems
    }

    private fun checkOthers(reader: HebrewReader): List<String> =
        OTHERS.mapNotNull { name ->
            val confidence = (runBlocking { reader.probe(TestModels.decodeFixture(name)) } as? Outcome.Success)?.value
            println("$name: confidence $confidence")
            if (confidence == null || confidence >= OnDeviceOcrEngine.HEBREW_PROBE_THRESHOLD) {
                "$name: probe $confidence is not below ${OnDeviceOcrEngine.HEBREW_PROBE_THRESHOLD}"
            } else {
                null
            }
        }

    private companion object {
        val OTHERS =
            listOf(
                "en-clean", "en-skew", "de-clean", "de-blur", "ru-clean", "ru-noise", "zh-clean", "zh-noise", "ja-clean", "mixed-clean",
            )
    }
}

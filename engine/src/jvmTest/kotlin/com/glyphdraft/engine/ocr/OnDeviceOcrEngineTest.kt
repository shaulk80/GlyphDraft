package com.glyphdraft.engine.ocr

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.stub.StubModelCatalog
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.ModelManifest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OnDeviceOcrEngineTest {
    private val input = ImageInput("page.png", byteArrayOf(1, 2, 3))
    private val boxes = listOf(lineBox(20.0, 100.0), lineBox(50.0, 300.0), lineBox(80.0, 200.0))
    private val allPacks = setOf(ModelManifest.LATIN, ModelManifest.ESLAV, ModelManifest.CJK, ModelManifest.HEBREW)

    private class Setup(
        val engine: OnDeviceOcrEngine,
        val detector: FakeDetector,
        val source: FakeModelSource,
    )

    private fun setup(
        installed: Set<String>,
        recognizers: Map<String, FakeRecognizer>,
        boxes: List<TextBox> = this.boxes,
        hebrew: HebrewReader? = null,
        decoder: ImageDecoder = FakeDecoder(),
    ): Setup {
        val detector = FakeDetector(boxes)
        val source = FakeModelSource(detector, recognizers)
        val engine = OnDeviceOcrEngine(decoder, StubModelCatalog(installed = installed), PaddleReader(source), TextLanguageDetector(), hebrew)
        return Setup(engine, detector, source)
    }

    /** Every PP-OCRv5 pack has a recognizer; the Latin one reads [text] with confidence, the others read junk. */
    private fun latinWins(text: String): Map<String, FakeRecognizer> =
        mapOf(ModelManifest.LATIN to FakeRecognizer(text, 0.9f), ModelManifest.ESLAV to FakeRecognizer("junk", 0.2f), ModelManifest.CJK to FakeRecognizer("junk", 0.2f))

    private fun success(outcome: Outcome<RecognitionResult>): RecognitionResult {
        assertIs<Outcome.Success<RecognitionResult>>(outcome, "outcome: $outcome")
        return outcome.value
    }

    @Test
    fun withALanguageTheInstalledPackThatServesItReadsAndNothingElseIsOpened() =
        runTest {
            val recognizers = mapOf(ModelManifest.LATIN to FakeRecognizer("Hello", 0.9f), ModelManifest.ESLAV to FakeRecognizer("Привет", 0.9f))
            val s = setup(allPacks, recognizers)

            val result = success(s.engine.recognize(input, Language.Russian))

            assertEquals(RecognitionResult("Привет\nПривет\nПривет", Language.Russian, detected = false), result)
            assertEquals(listOf(ModelManifest.ESLAV), s.source.opened)
        }

    @Test
    fun aLanguageWhoseModelIsNotInstalledIsAFailureForTheUser() =
        runTest {
            val s = setup(setOf(ModelManifest.LATIN), mapOf(ModelManifest.LATIN to FakeRecognizer("Hello", 0.9f)))

            val outcome = s.engine.recognize(input, Language.Russian)

            assertEquals(Outcome.Failure("The Russian models are not installed."), outcome)
        }

    @Test
    fun withoutALanguageTheMostConfidentPackReadsAndTheLanguageIsToldFromTheText() =
        runTest {
            class Case(val name: String, val text: String, val expected: Language)

            val cases =
                listOf(
                    Case("russian", "Собрание начинается в десять часов", Language.Russian),
                    Case("german", "Die Besprechung beginnt am Donnerstag", Language.German),
                    Case("english", "The meeting starts at ten and all are welcome", Language.English),
                    Case("french", "La réunion commence à dix heures et tout le monde est invité", Language.French),
                    Case("chinese", "会议将于上午十点开始", Language.Chinese),
                    Case("japanese", "会議は午前十時に始まります", Language.Japanese),
                    Case("no marker words: the first installed language of the script", "Invoice 2026-1187 totals 1,249.50", Language.English),
                )
            val packOfText = mapOf(Language.Russian to ModelManifest.ESLAV, Language.Chinese to ModelManifest.CJK, Language.Japanese to ModelManifest.CJK)

            for (case in cases) {
                val winner = packOfText[case.expected] ?: ModelManifest.LATIN
                val recognizers =
                    listOf(ModelManifest.LATIN, ModelManifest.ESLAV, ModelManifest.CJK).associateWith { pack ->
                        if (pack == winner) FakeRecognizer(case.text, 0.95f) else FakeRecognizer("junk", 0.2f)
                    }
                val s = setup(allPacks - ModelManifest.HEBREW, recognizers)

                val result = success(s.engine.recognize(input, null))

                assertEquals(case.expected, result.language, case.name)
                assertTrue(result.detected, case.name)
                assertEquals(List(3) { case.text }.joinToString("\n"), result.text, case.name)
                assertEquals(1, s.detector.calls, "${case.name}: the page is detected once for all packs")
            }
        }

    @Test
    fun theLanguageComesFromTheTextEvenWhenAnotherScriptsRecognizerWonTheProbe() =
        runTest {
            // The Russian recognizer reads English as well and may be a hair more confident than the Latin one.
            val recognizers =
                mapOf(
                    ModelManifest.LATIN to FakeRecognizer("Die Besprechung beginnt am Donnerstag", 0.90f),
                    ModelManifest.ESLAV to FakeRecognizer("Die Besprechung beginnt am Donnerstag", 0.91f),
                    ModelManifest.CJK to FakeRecognizer("junk", 0.2f),
                )
            val s = setup(allPacks - ModelManifest.HEBREW, recognizers)

            assertEquals(Language.German, success(s.engine.recognize(input, null)).language)
        }

    @Test
    fun whenOnlyOnePackIsInstalledItIsNotProbedAndTheDetectorStaysWithinItsLanguages() =
        runTest {
            val recognizer = FakeRecognizer("Собрание начинается в десять часов", 0.9f)
            val s = setup(setOf(ModelManifest.LATIN), mapOf(ModelManifest.LATIN to recognizer))

            val result = success(s.engine.recognize(input, null))

            // No installed language is written in Cyrillic, so the first language of the pack that read it answers.
            assertEquals(Language.English, result.language)
            assertEquals(3, recognizer.widths.size)
        }

    @Test
    fun hebrewIsProbedFirstAndReadByTheHebrewReaderWhenTheProbeIsConfident() =
        runTest {
            val cases =
                listOf(
                    Triple(0.93f, true, "hebrew"),
                    Triple(0.8f, true, "hebrew"),
                    Triple(0.51f, false, "paddle"),
                    Triple(0.0f, false, "paddle"),
                )

            for ((confidence, isHebrew, reader) in cases) {
                val hebrew = FakeHebrewReader(Outcome.Success(confidence))
                val s = setup(allPacks, latinWins("Hello world and all"), hebrew = hebrew)

                val result = success(s.engine.recognize(input, null))

                assertEquals(1, hebrew.probes, "probe at $confidence")
                assertEquals(if (isHebrew) 1 else 0, hebrew.reads, "reads at $confidence")
                assertEquals(if (isHebrew) Language.Hebrew else Language.English, result.language, "language at $confidence ($reader)")
                assertTrue(result.detected)
            }
        }

    @Test
    fun aFailedHebrewProbeFallsBackToThePaddlePacks() =
        runTest {
            val hebrew = FakeHebrewReader(Outcome.Failure("Tesseract failed."))
            val s = setup(allPacks, latinWins("The end of all"), hebrew = hebrew)

            assertEquals(Language.English, success(s.engine.recognize(input, null)).language)
        }

    @Test
    fun hebrewAloneIsReadWhateverTheProbeSays() =
        runTest {
            val hebrew = FakeHebrewReader(Outcome.Success(0.1f))
            val s = setup(setOf(ModelManifest.HEBREW), emptyMap(), hebrew = hebrew)

            assertEquals(Language.Hebrew, success(s.engine.recognize(input, null)).language)

            val failing = FakeHebrewReader(Outcome.Failure("Tesseract failed."))
            val failed = setup(setOf(ModelManifest.HEBREW), emptyMap(), hebrew = failing).engine.recognize(input, null)
            assertEquals(Outcome.Failure("Tesseract failed."), failed)
        }

    @Test
    fun hebrewWithAHintGoesStraightToTheHebrewReader() =
        runTest {
            val hebrew = FakeHebrewReader(Outcome.Success(0.0f))
            val s = setup(allPacks, emptyMap(), hebrew = hebrew)

            val result = success(s.engine.recognize(input, Language.Hebrew))

            assertEquals(RecognitionResult("שלום עולם", Language.Hebrew, detected = false), result)
            assertEquals(0, hebrew.probes)
        }

    @Test
    fun hebrewWithoutAHebrewReaderSaysSo() =
        runTest {
            val s = setup(setOf(ModelManifest.HEBREW), emptyMap())

            assertEquals(Outcome.Failure(OnDeviceOcrEngine.HEBREW_UNAVAILABLE), s.engine.recognize(input, Language.Hebrew))
            assertEquals(Outcome.Failure(OnDeviceOcrEngine.HEBREW_UNAVAILABLE), s.engine.recognize(input, null))
        }

    @Test
    fun expectedProblemsComeBackAsFailures() =
        runTest {
            val recognizers = mapOf(ModelManifest.LATIN to FakeRecognizer("Hello", 0.9f))

            val nothingInstalled = setup(emptySet(), recognizers).engine.recognize(input, null)
            assertEquals(Outcome.Failure("No language models are installed. Install one to read text."), nothingInstalled)

            val noText = setup(setOf(ModelManifest.LATIN), recognizers, boxes = emptyList())
            assertEquals(Outcome.Failure(OnDeviceOcrEngine.NO_TEXT), noText.engine.recognize(input, null))
            assertEquals(Outcome.Failure(OnDeviceOcrEngine.NO_TEXT), noText.engine.recognize(input, Language.English))

            val blankReading = setup(setOf(ModelManifest.LATIN), mapOf(ModelManifest.LATIN to FakeRecognizer("  ", 0.9f)))
            assertEquals(Outcome.Failure(OnDeviceOcrEngine.NO_TEXT), blankReading.engine.recognize(input, null))

            val unreadable = Outcome.Failure("The file is not an image.")
            val badImage = setup(setOf(ModelManifest.LATIN), recognizers, decoder = FakeDecoder(unreadable))
            assertEquals(unreadable, badImage.engine.recognize(input, null))
            assertEquals(unreadable, badImage.engine.recognize(input, Language.English))
        }

    @Test
    fun closeReleasesTheModels() =
        runTest {
            val recognizer = FakeRecognizer("Hello", 0.9f)
            val s = setup(setOf(ModelManifest.LATIN), mapOf(ModelManifest.LATIN to recognizer))
            s.engine.recognize(input, Language.English)

            s.engine.close()

            assertTrue(recognizer.closed)
            assertTrue(s.detector.closed)
        }
}

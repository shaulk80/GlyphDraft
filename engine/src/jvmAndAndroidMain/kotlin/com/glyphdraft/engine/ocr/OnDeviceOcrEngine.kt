package com.glyphdraft.engine.ocr

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.LanguageDetector
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Script
import com.glyphdraft.core.installedPacks
import com.glyphdraft.engine.language.TextScripts
import com.glyphdraft.engine.layout.PageLayout
import com.glyphdraft.engine.layout.PageText
import com.glyphdraft.engine.models.ModelManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The on-device [OcrEngine]: reads with the installed model packs and chooses among them.
 *
 * - With a language: the installed pack that serves it reads the image (Hebrew through [hebrew], the rest through
 *   [paddle]).
 * - Without: if Hebrew is installed, [HebrewReader.probe] runs first and a confidence of [HEBREW_PROBE_THRESHOLD] or
 *   more, with a text that is written in Hebrew, means Hebrew. Otherwise the text is detected once, every installed PP-OCRv5 pack reads the two longest lines
 *   ([PaddleReader.chooseBest]) and the best reads the whole page. The language is then told from the text by
 *   [languageDetector], among all installed languages.
 *
 * The apps build it in the composition root (task 8); [close] releases the models.
 */
class OnDeviceOcrEngine(
    private val decoder: ImageDecoder,
    private val catalog: ModelCatalog,
    private val paddle: PaddleReader,
    private val languageDetector: LanguageDetector,
    private val hebrew: HebrewReader? = null,
) : OcrEngine,
    AutoCloseable {
    override suspend fun recognize(image: ImageInput, languageHint: Language?): Outcome<RecognitionResult> =
        withContext(Dispatchers.Default) {
            when (val decoded = decoder.decode(image.bytes)) {
                is Outcome.Failure -> decoded
                is Outcome.Success -> {
                    if (languageHint != null) {
                        readInLanguage(decoded.value, languageHint)
                    } else {
                        readDetectingLanguage(decoded.value)
                    }
                }
            }
        }

    override fun close() {
        paddle.close()
        hebrew?.close()
    }

    private suspend fun readInLanguage(page: PixelImage, language: Language): Outcome<RecognitionResult> {
        val pack = catalog.installedPacks().firstOrNull { language in it.languages }
        if (pack == null) {
            return Outcome.Failure("The ${language.displayName} models are not installed.")
        }

        return when (val read = readWithPack(page, pack)) {
            is Outcome.Failure -> read
            is Outcome.Success -> Outcome.Success(resultOf(read.value, language, detected = false))
        }
    }

    private fun resultOf(read: TextRead, language: Language, detected: Boolean): RecognitionResult =
        RecognitionResult(read.page.text, language, detected, read.lowQuality, read.page.runningText)

    private suspend fun readDetectingLanguage(page: PixelImage): Outcome<RecognitionResult> {
        // A translation model is in the same catalog (decision 0013) but reads nothing, so it must not take part in the probe.
        val installed = catalog.installedPacks().filter { it.direction == null }
        if (installed.isEmpty()) {
            return Outcome.Failure("No language models are installed. Install one to read text.")
        }

        val paddlePacks = installed.filter { it.id != ModelManifest.HEBREW }
        val hebrewPack = installed.firstOrNull { it.id == ModelManifest.HEBREW }
        if (hebrewPack != null && hebrew != null) {
            val probed = probeHebrew(page, onlyChoice = paddlePacks.isEmpty())
            if (probed != null) {
                return probed
            }
        }

        if (paddlePacks.isEmpty()) {
            return Outcome.Failure(HEBREW_UNAVAILABLE)
        }

        return readBestPack(page, paddlePacks)
    }

    /** A Hebrew result, or null when the page is not Hebrew (or the probe failed) and the other packs should try. */
    private suspend fun probeHebrew(page: PixelImage, onlyChoice: Boolean): Outcome<RecognitionResult>? {
        val reader = hebrew ?: return null
        val confidence =
            when (val probe = reader.probe(page)) {
                is Outcome.Failure -> return if (onlyChoice) probe else null
                is Outcome.Success -> probe.value
            }

        if (confidence < HEBREW_PROBE_THRESHOLD && !onlyChoice) {
            return null
        }

        val text =
            when (val read = reader.read(page)) {
                is Outcome.Failure -> return read
                is Outcome.Success -> PageLayout.compose(read.value)
            }

        // Tesseract is also confident about a page it read nothing of (0.95 on an English photo, where it found no
        // text at all), so the confidence alone does not make a page Hebrew: the text has to be written in Hebrew.
        if (!onlyChoice && TextScripts.profile(text.text).dominant != Script.HEBREW) {
            return null
        }

        val lowQuality = ReadQuality.isLowHebrewReading(confidence)
        return nonBlank(text) { Outcome.Success(resultOf(TextRead(it, lowQuality), Language.Hebrew, detected = true)) }
    }

    private suspend fun readBestPack(page: PixelImage, packs: List<ModelPack>): Outcome<RecognitionResult> {
        val rows =
            when (val detected = paddle.detectRows(page)) {
                is Outcome.Failure -> return detected
                is Outcome.Success -> detected.value
            }

        if (rows.isEmpty()) {
            return Outcome.Failure(NO_TEXT)
        }

        val best =
            when (val chosen = paddle.chooseBest(page, rows, packs.map { it.id })) {
                is Outcome.Failure -> return chosen
                is Outcome.Success -> packs.first { it.id == chosen.value }
            }

        return when (val read = readRows(page, rows, best)) {
            is Outcome.Failure -> read
            is Outcome.Success -> {
                val language = languageOf(read.value.page.text, best, packs)
                Outcome.Success(resultOf(read.value, language, detected = true))
            }
        }
    }

    private suspend fun readWithPack(page: PixelImage, pack: ModelPack): Outcome<TextRead> {
        if (pack.id == ModelManifest.HEBREW) {
            return readHebrew(page)
        }

        val rows =
            when (val detected = paddle.detectRows(page)) {
                is Outcome.Failure -> return detected
                is Outcome.Success -> detected.value
            }

        return readRows(page, rows, pack)
    }

    private suspend fun readRows(page: PixelImage, rows: List<List<TextBox>>, pack: ModelPack): Outcome<TextRead> =
        when (val read = paddle.read(page, rows, pack.id)) {
            is Outcome.Failure -> read
            is Outcome.Success -> {
                val lowQuality = ReadQuality.isLowPaddleReading(rows.flatten(), read.value.text)
                val text = PageLayout.compose(read.value.placedLines())
                nonBlank(text) { Outcome.Success(TextRead(it, lowQuality)) }
            }
        }

    private suspend fun readHebrew(page: PixelImage): Outcome<TextRead> {
        val reader = hebrew ?: return Outcome.Failure(HEBREW_UNAVAILABLE)
        val text =
            when (val read = reader.read(page)) {
                is Outcome.Failure -> return read
                is Outcome.Success -> PageLayout.compose(read.value)
            }

        // The reader keeps the last page, so this costs no second recognition. A failed probe is no reason to doubt the text.
        val confidence = (reader.probe(page) as? Outcome.Success)?.value
        val lowQuality = confidence != null && ReadQuality.isLowHebrewReading(confidence)
        return nonBlank(text) { Outcome.Success(TextRead(it, lowQuality)) }
    }

    /** The text of one reading and whether the engine doubts it ([RecognitionResult.lowQuality]). */
    private class TextRead(val page: PageText, val lowQuality: Boolean)

    /**
     * The language of [text] among the languages of all [installed] packs, not only of the pack that read it: the
     * `eslav` recognizer reads English as well as the `latin` one and may win the probe on an English page. When the
     * detector cannot tell (no marker words), the first installed language written in the script of the text; failing
     * that, the first language of the [pack] that read it.
     */
    private suspend fun languageOf(text: String, pack: ModelPack, installed: List<ModelPack>): Language {
        val languages = installed.flatMap { it.languages }.distinct()
        val guess = languageDetector.detect(text, languages.toSet())
        if (guess != null) {
            return guess.language
        }

        val script = TextScripts.profile(text).dominant
        return languages.firstOrNull { it.script == script } ?: pack.languages.first()
    }

    private inline fun <T> nonBlank(text: PageText, onText: (PageText) -> Outcome<T>): Outcome<T> {
        if (text.text.isBlank()) {
            return Outcome.Failure(NO_TEXT)
        }

        return onText(text)
    }

    companion object {
        /** Mean Tesseract `heb` confidence (0 to 1) from which an image counts as Hebrew; the spike saw 0.93 against 0.18 to 0.51. */
        const val HEBREW_PROBE_THRESHOLD = 0.8f

        const val NO_TEXT = "No text was found in the image."
        const val HEBREW_UNAVAILABLE = "Reading Hebrew is not available in this version."
    }
}

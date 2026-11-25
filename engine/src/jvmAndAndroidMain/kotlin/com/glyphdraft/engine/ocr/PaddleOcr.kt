package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.map
import com.glyphdraft.engine.layout.PlacedLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** One recognised line of text with the [box] it came from (pixels of the original image). */
data class OcrLine(val text: String, val confidence: Float, val box: TextBox)

/** The text of a page: [rows] from top to bottom, each with its lines from left to right. */
data class OcrPage(val rows: List<List<OcrLine>>) {
    val lines: List<OcrLine> get() = rows.flatten()

    /** One text line per row; boxes that share a row are separated by a space. */
    val text: String get() = rows.joinToString("\n") { textOf(it) }

    /** The rows with their places on a level page, for the layout of the text. */
    fun placedLines(): List<PlacedLine> {
        val tilt = PageTilt.of(lines.map { it.box })
        return rows.map { row -> tilt.place(row.map { it.box }, textOf(row)) }
    }

    private fun textOf(row: List<OcrLine>): String = row.joinToString(" ") { it.text.trim() }

    /** Mean confidence of the lines (0 for an empty page). */
    val confidence: Float
        get() {
            val all = lines
            if (all.isEmpty()) {
                return 0f
            }

            return all.map { it.confidence }.average().toFloat()
        }
}

/**
 * Detection, cropping and recognition over a [BoxDetector]. The steps are separate so that a caller that has several
 * recognizers (one per script) detects once: [detectRows], then [read] with the recognizer it chose.
 */
class PaddleOcr(private val detector: BoxDetector) {
    /** Finds the text boxes of [image] and puts them in reading order. */
    suspend fun detectRows(image: PixelImage): Outcome<List<List<TextBox>>> =
        withContext(Dispatchers.Default) { detector.detect(image).map(ReadingOrder::rows) }

    /**
     * Reads the boxes of [rows] with [recognizer]; lines that come out empty are dropped. The rows of the page are
     * then formed again from the boxes with text: a speck that reads as nothing can sit between the boxes of a row and
     * split it. Cancellable between lines.
     */
    suspend fun read(image: PixelImage, rows: List<List<TextBox>>, recognizer: LineRecognizer): Outcome<OcrPage> =
        withContext(Dispatchers.Default) {
            val lines = ArrayList<OcrLine>()
            for (box in rows.flatten()) {
                coroutineContext.ensureActive()
                when (val read = recognizer.recognize(ImageOps.cropUpright(image, box))) {
                    is Outcome.Failure -> return@withContext read
                    is Outcome.Success -> {
                        if (read.value.text.isNotBlank()) {
                            lines.add(OcrLine(read.value.text, read.value.confidence, box))
                        }
                    }
                }
            }

            Outcome.Success(OcrPage(ReadingOrder.rows(lines) { it.box }))
        }

    /** Detects and reads in one go. */
    suspend fun read(image: PixelImage, recognizer: LineRecognizer): Outcome<OcrPage> =
        when (val detected = detectRows(image)) {
            is Outcome.Failure -> detected
            is Outcome.Success -> read(image, detected.value, recognizer)
        }
}

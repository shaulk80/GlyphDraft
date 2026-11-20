package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.map
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
    val text: String get() = rows.joinToString("\n") { row -> row.joinToString(" ") { it.text } }

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
 * Detection, cropping and recognition over a [TextDetector]. The steps are separate so that a caller that has several
 * recognizers (one per script) detects once: [detectRows], then [read] with the recognizer it chose.
 */
class PaddleOcr(private val detector: TextDetector) {
    /** Finds the text boxes of [image] and puts them in reading order. */
    suspend fun detectRows(image: PixelImage): Outcome<List<List<TextBox>>> =
        withContext(Dispatchers.Default) { detector.detect(image).map(ReadingOrder::rows) }

    /** Reads the boxes of [rows] with [recognizer]; lines that come out empty are dropped. Cancellable between lines. */
    suspend fun read(image: PixelImage, rows: List<List<TextBox>>, recognizer: TextRecognizer): Outcome<OcrPage> =
        withContext(Dispatchers.Default) {
            val pageRows = ArrayList<List<OcrLine>>()
            for (row in rows) {
                val lines = ArrayList<OcrLine>()
                for (box in row) {
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

                if (lines.isNotEmpty()) {
                    pageRows.add(lines)
                }
            }

            Outcome.Success(OcrPage(pageRows))
        }

    /** Detects and reads in one go. */
    suspend fun read(image: PixelImage, recognizer: TextRecognizer): Outcome<OcrPage> =
        when (val detected = detectRows(image)) {
            is Outcome.Failure -> detected
            is Outcome.Success -> read(image, detected.value, recognizer)
        }
}

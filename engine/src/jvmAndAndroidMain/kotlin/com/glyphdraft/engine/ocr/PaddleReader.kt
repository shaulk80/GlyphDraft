package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The PP-OCRv5 side of the engine: opens the detector and the recognizers of [source] when they are first needed and
 * keeps them (an ONNX session takes a few hundred milliseconds to load), and reads a page with the recognizer of a pack.
 * [close] releases the sessions.
 */
class PaddleReader(private val source: PaddleModelSource) : AutoCloseable {
    private val lock = Mutex()
    private var detector: BoxDetector? = null
    private var ocr: PaddleOcr? = null
    private val recognizers = HashMap<String, LineRecognizer>()

    /** Finds the text of [image] once, in reading order; the rows can then be read with any pack ([read], [chooseBest]). */
    suspend fun detectRows(image: PixelImage): Outcome<List<List<TextBox>>> =
        when (val pipeline = pipeline()) {
            is Outcome.Failure -> pipeline
            is Outcome.Success -> pipeline.value.detectRows(image)
        }

    /** Reads [rows] with the recognizer of the pack [packId]. */
    suspend fun read(image: PixelImage, rows: List<List<TextBox>>, packId: String): Outcome<OcrPage> {
        val pipeline =
            when (val opened = pipeline()) {
                is Outcome.Failure -> return opened
                is Outcome.Success -> opened.value
            }

        return when (val recognizer = recognizer(packId)) {
            is Outcome.Failure -> recognizer
            is Outcome.Success -> pipeline.read(image, rows, recognizer.value)
        }
    }

    /**
     * The pack of [packIds] whose recognizer reads the page best, judged on its [PROBE_LINES] longest lines (spike
     * strategy C: the same choice as reading every line with every recognizer, with the detection shared). The
     * measure is the mean confidence of the lines. It only ranks these recognizers against each other: a recognizer is
     * also sure of itself on text it cannot read (a Latin one reads Hebrew with 0.84), so it is no proof that the
     * winner is right. A tie goes to the first pack. [packIds] must not be empty.
     */
    suspend fun chooseBest(image: PixelImage, rows: List<List<TextBox>>, packIds: List<String>): Outcome<String> {
        if (packIds.size == 1) {
            return Outcome.Success(packIds.first())
        }

        val probe = longestLines(rows)
        var best = packIds.first()
        var bestConfidence = -1f
        for (packId in packIds) {
            when (val page = read(image, probe, packId)) {
                is Outcome.Failure -> return page
                is Outcome.Success -> {
                    if (page.value.confidence > bestConfidence) {
                        best = packId
                        bestConfidence = page.value.confidence
                    }
                }
            }
        }

        return Outcome.Success(best)
    }

    override fun close() {
        recognizers.values.forEach { it.close() }
        recognizers.clear()
        detector?.close()
        detector = null
        ocr = null
    }

    private suspend fun pipeline(): Outcome<PaddleOcr> =
        lock.withLock {
            val existing = ocr
            if (existing != null) {
                return@withLock Outcome.Success(existing)
            }

            when (val opened = withContext(Dispatchers.Default) { source.openDetector() }) {
                is Outcome.Failure -> opened
                is Outcome.Success -> {
                    detector = opened.value
                    val created = PaddleOcr(opened.value)
                    ocr = created
                    Outcome.Success(created)
                }
            }
        }

    private suspend fun recognizer(packId: String): Outcome<LineRecognizer> =
        lock.withLock {
            val existing = recognizers[packId]
            if (existing != null) {
                return@withLock Outcome.Success(existing)
            }

            when (val opened = withContext(Dispatchers.Default) { source.openRecognizer(packId) }) {
                is Outcome.Failure -> opened
                is Outcome.Success -> {
                    recognizers[packId] = opened.value
                    opened
                }
            }
        }

    /** The [PROBE_LINES] longest boxes of the page, each in a row of its own, in reading order. */
    private fun longestLines(rows: List<List<TextBox>>): List<List<TextBox>> {
        val longest = rows.flatten().sortedByDescending { it.length }.take(PROBE_LINES)
        return rows.flatten().filter { box -> longest.any { it === box } }.map { listOf(it) }
    }

    companion object {
        const val PROBE_LINES = 2
    }
}

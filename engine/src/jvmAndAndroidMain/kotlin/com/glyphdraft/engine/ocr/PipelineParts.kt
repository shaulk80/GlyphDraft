package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome

/** Finds the lines of text in an image. [TextDetector] is the PP-OCRv5 one; tests use fakes. */
interface BoxDetector : AutoCloseable {
    /** Oriented boxes in pixels of [image], in no particular order (see [ReadingOrder]). */
    fun detect(image: PixelImage): Outcome<List<TextBox>>
}

/** Reads one line of text from an upright crop. [TextRecognizer] is the PP-OCRv5 one; tests use fakes. */
interface LineRecognizer : AutoCloseable {
    fun recognize(line: PixelImage): Outcome<LineText>
}

/** Opens the detector and the recognizers of the installed packs. [PaddleModels] is the real one. */
interface PaddleModelSource {
    /** The text detector; every recognizer pack includes it. */
    fun openDetector(): Outcome<BoxDetector>

    /** The recognizer of the pack [packId]. */
    fun openRecognizer(packId: String): Outcome<LineRecognizer>
}

/**
 * Reads Hebrew, which no PP-OCRv5 recognizer can. [TesseractHebrewReader] is the real one; the engine only needs these
 * two calls, so tests use fakes.
 */
interface HebrewReader : AutoCloseable {
    override fun close() {}

    /**
     * A quick read to find out whether [image] is Hebrew: the mean word confidence, 0 to 1. The spike measured 0.93 on
     * Hebrew and 0.18 to 0.51 on every other fixture ([OnDeviceOcrEngine.HEBREW_PROBE_THRESHOLD]).
     */
    suspend fun probe(image: PixelImage): Outcome<Float>

    /** The Hebrew text of [image], lines separated by line breaks. */
    suspend fun read(image: PixelImage): Outcome<String>
}

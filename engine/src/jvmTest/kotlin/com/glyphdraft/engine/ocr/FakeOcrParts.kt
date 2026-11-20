package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome

/** Fakes of the pipeline parts for the tests of the engine's choices; no model and no ONNX Runtime involved. */
internal class FakeDecoder(private val result: Outcome<PixelImage> = Outcome.Success(TestImages.solid(400, 100, PixelImage.WHITE))) : ImageDecoder {
    override fun decode(bytes: ByteArray): Outcome<PixelImage> = result
}

/** Returns [boxes] for every image and counts the calls. */
internal class FakeDetector(private val boxes: List<TextBox>) : BoxDetector {
    var calls = 0
    var closed = false

    override fun detect(image: PixelImage): Outcome<List<TextBox>> {
        calls++
        return Outcome.Success(boxes)
    }

    override fun close() {
        closed = true
    }
}

/** Reads every line as [text] with [confidence]; remembers the widths of the lines it was given. */
internal class FakeRecognizer(private val text: String, private val confidence: Float) : LineRecognizer {
    val widths = ArrayList<Int>()
    var closed = false

    override fun recognize(line: PixelImage): Outcome<LineText> {
        widths.add(line.width)
        return Outcome.Success(LineText(text, confidence))
    }

    override fun close() {
        closed = true
    }
}

/** Opens the given fakes; [opened] lists the pack ids that were asked for, in order. */
internal class FakeModelSource(
    private val detector: BoxDetector,
    private val recognizers: Map<String, LineRecognizer>,
) : PaddleModelSource {
    val opened = ArrayList<String>()

    override fun openDetector(): Outcome<BoxDetector> = Outcome.Success(detector)

    override fun openRecognizer(packId: String): Outcome<LineRecognizer> {
        opened.add(packId)
        val recognizer = recognizers[packId]
        if (recognizer == null) {
            return Outcome.Failure("No fake recognizer for $packId.")
        }

        return Outcome.Success(recognizer)
    }
}

internal class FakeHebrewReader(
    private val probe: Outcome<Float>,
    private val text: String = "שלום עולם",
) : HebrewReader {
    var probes = 0
    var reads = 0

    override suspend fun probe(image: PixelImage): Outcome<Float> {
        probes++
        return probe
    }

    override suspend fun read(image: PixelImage): Outcome<String> {
        reads++
        return Outcome.Success(text)
    }
}

/** A horizontal line box of [length] by 20 pixels centred at x = 200 and the given [y]. */
internal fun lineBox(y: Double, length: Double): TextBox = TextBox(cx = 200.0, cy = y, length = length, thickness = 20.0, angle = 0.0)

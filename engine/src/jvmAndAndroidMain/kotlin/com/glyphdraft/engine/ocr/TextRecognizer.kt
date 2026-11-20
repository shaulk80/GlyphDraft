package com.glyphdraft.engine.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import com.glyphdraft.core.Outcome
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Reads one line of text (an upright crop) with one PP-OCRv5 mobile recognizer; each recognizer serves some scripts. */
class TextRecognizer internal constructor(
    private val session: OrtSession,
    private val dictionary: CharacterDictionary,
) : LineRecognizer {
    private val inputName = session.inputNames.first()

    /** The text of [line] and its confidence. */
    override fun recognize(line: PixelImage): Outcome<LineText> {
        val height = INPUT_HEIGHT
        val width = max(MIN_WIDTH, min(MAX_WIDTH, (line.width * height / line.height.toDouble()).roundToInt()))
        val input = normalise(ImageOps.resize(line, width, height))
        return try {
            OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), FloatBuffer.wrap(input), longArrayOf(1, 3, height.toLong(), width.toLong())).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { result ->
                    // The output is [1, steps, classes].
                    @Suppress("UNCHECKED_CAST")
                    val steps = (result[0].value as Array<Array<FloatArray>>)[0]
                    Outcome.Success(CtcDecoder.decode(steps, dictionary))
                }
            }
        } catch (e: OrtException) {
            Outcome.Failure("Text recognition failed: ${e.message}", e)
        }
    }

    override fun close() {
        session.close()
    }

    /** Planes in blue, green, red order, scaled to -1..1. */
    private fun normalise(image: PixelImage): FloatArray {
        val plane = image.width * image.height
        val input = FloatArray(3 * plane)
        for (i in 0 until plane) {
            input[i] = (image.blue(i) / 255f - 0.5f) / 0.5f
            input[plane + i] = (image.green(i) / 255f - 0.5f) / 0.5f
            input[2 * plane + i] = (image.red(i) / 255f - 0.5f) / 0.5f
        }

        return input
    }

    private companion object {
        const val INPUT_HEIGHT = 48
        const val MIN_WIDTH = 16
        const val MAX_WIDTH = 3200
    }
}

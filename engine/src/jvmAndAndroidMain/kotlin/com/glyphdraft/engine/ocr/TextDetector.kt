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

/** Finds the lines of text in an image with the PP-OCRv5 mobile detector. One detector serves every recognizer. */
class TextDetector internal constructor(
    private val session: OrtSession,
    private val postProcessor: DetectionPostProcessor = DetectionPostProcessor(),
) : AutoCloseable {
    private val inputName = session.inputNames.first()

    /** Oriented boxes in pixels of [image], in no particular order (see [ReadingOrder]). */
    fun detect(image: PixelImage): Outcome<List<TextBox>> {
        val width = fitToModel(image.width, image)
        val height = fitToModel(image.height, image)
        val input = normalise(ImageOps.resize(image, width, height))
        return try {
            OnnxTensor.createTensor(OrtEnvironment.getEnvironment(), FloatBuffer.wrap(input), longArrayOf(1, 3, height.toLong(), width.toLong())).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { result ->
                    val probabilities = flatten(result[0].value)
                    val scaleX = image.width.toDouble() / width
                    val scaleY = image.height.toDouble() / height
                    Outcome.Success(postProcessor.boxes(probabilities, width, height, scaleX, scaleY))
                }
            }
        } catch (e: OrtException) {
            Outcome.Failure("Text detection failed: ${e.message}", e)
        }
    }

    override fun close() {
        session.close()
    }

    /** The side length the model gets: the image shrunk so that its long side is at most 960, rounded to a multiple of 32. */
    private fun fitToModel(side: Int, image: PixelImage): Int {
        val scale = min(1.0, MAX_LONG_SIDE.toDouble() / max(image.width, image.height))
        return max(STRIDE, ((side * scale) / STRIDE).roundToInt() * STRIDE)
    }

    /** Planes in blue, green, red order with mean and standard deviation per channel, as the reference does. */
    private fun normalise(image: PixelImage): FloatArray {
        val plane = image.width * image.height
        val input = FloatArray(3 * plane)
        for (i in 0 until plane) {
            input[i] = (image.blue(i) / 255f - MEAN[0]) / STD[0]
            input[plane + i] = (image.green(i) / 255f - MEAN[1]) / STD[1]
            input[2 * plane + i] = (image.red(i) / 255f - MEAN[2]) / STD[2]
        }

        return input
    }

    /** The output is [1, 1, height, width]. */
    private fun flatten(value: Any): FloatArray {
        @Suppress("UNCHECKED_CAST")
        val rows = (value as Array<Array<Array<FloatArray>>>)[0][0]
        val flat = FloatArray(rows.size * rows[0].size)
        var next = 0
        for (row in rows) {
            row.copyInto(flat, next)
            next += row.size
        }

        return flat
    }

    private companion object {
        const val MAX_LONG_SIDE = 960
        const val STRIDE = 32
        val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}

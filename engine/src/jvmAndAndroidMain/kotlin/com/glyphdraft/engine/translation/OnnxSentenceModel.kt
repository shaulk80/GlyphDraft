package com.glyphdraft.engine.translation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import com.glyphdraft.core.Outcome
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.coroutines.CoroutineContext

/**
 * An OPUS-MT model on ONNX Runtime: the [encoder] reads the source ids once, a [GreedyDecoder] then writes the
 * translation one id at a time. Both sessions belong to this object and are closed with it.
 */
internal class OnnxSentenceModel(
    private val encoder: OrtSession,
    private val decoder: OrtSession,
    private val greedy: GreedyDecoder,
) : SentenceModel {
    override fun generate(sourceIds: IntArray, maxNewTokens: Int, context: CoroutineContext): Outcome<IntArray> {
        val env = OrtEnvironment.getEnvironment()
        return try {
            val input = longTensor(env, LongArray(sourceIds.size) { sourceIds[it].toLong() })
            val mask = longTensor(env, LongArray(sourceIds.size) { 1L })
            input.use {
                mask.use {
                    encoder.run(mapOf(ENCODER_INPUT to input, ENCODER_MASK to mask)).use { encoded ->
                        val hidden = encoded.get(ENCODER_OUTPUT).get() as OnnxTensor
                        Outcome.Success(greedy.decode(hidden, mask, maxNewTokens, context))
                    }
                }
            }
        } catch (e: OrtException) {
            Outcome.Failure("The translation failed: ${e.message}", e)
        }
    }

    override fun close() {
        encoder.close()
        decoder.close()
    }

    private companion object {
        const val ENCODER_INPUT = "input_ids"
        const val ENCODER_MASK = "attention_mask"
        const val ENCODER_OUTPUT = "last_hidden_state"
    }
}

/** Writes the ids of a translation from the encoder output, always taking the most likely next id. */
internal interface GreedyDecoder {
    fun decode(hidden: OnnxTensor, mask: OnnxTensor, maxNewTokens: Int, context: CoroutineContext): IntArray
}

/** A tensor of shape `[1, values.size]`, as the models read ids and masks. */
internal fun longTensor(env: OrtEnvironment, values: LongArray): OnnxTensor =
    OnnxTensor.createTensor(env, LongBuffer.wrap(values), longArrayOf(1, values.size.toLong()))

/**
 * The most likely id at the last position of [logits] (shape `[1, positions, vocabulary]`), never [excludedId]: Marian
 * models must not write their padding token, which is also the id the decoder starts with.
 */
internal fun mostLikelyLast(logits: OnnxTensor, excludedId: Int): Int {
    val vocabulary = logits.info.shape.last().toInt()
    val values: FloatBuffer = logits.floatBuffer
    val base = values.limit() - vocabulary
    var best = 0
    var bestValue = Float.NEGATIVE_INFINITY
    for (id in 0 until vocabulary) {
        if (id == excludedId) {
            continue
        }

        val value = values.get(base + id)
        if (value > bestValue) {
            bestValue = value
            best = id
        }
    }

    return best
}

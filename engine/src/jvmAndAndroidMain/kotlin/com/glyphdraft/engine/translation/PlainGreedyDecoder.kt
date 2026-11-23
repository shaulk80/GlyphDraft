package com.glyphdraft.engine.translation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Decoder without a key/value cache (the Hebrew exports): every step feeds the whole prefix written so far and reads the
 * logits of its last position. Slower for long sentences than [CachedGreedyDecoder], the same result.
 */
internal class PlainGreedyDecoder(
    private val decoder: OrtSession,
    private val config: MarianConfig,
) : GreedyDecoder {
    override fun decode(hidden: OnnxTensor, mask: OnnxTensor, maxNewTokens: Int, context: CoroutineContext): IntArray {
        val env = OrtEnvironment.getEnvironment()
        val prefix = ArrayList<Long>()
        prefix.add(config.startId.toLong())
        for (step in 0 until maxNewTokens) {
            context.ensureActive()
            val next = nextId(env, prefix, hidden, mask)
            if (next == config.endId) {
                break
            }

            prefix.add(next.toLong())
        }

        return IntArray(prefix.size - 1) { prefix[it + 1].toInt() }
    }

    private fun nextId(env: OrtEnvironment, prefix: List<Long>, hidden: OnnxTensor, mask: OnnxTensor): Int {
        longTensor(env, prefix.toLongArray()).use { ids ->
            val inputs = mapOf(DECODER_INPUT to ids, HIDDEN to hidden, ENCODER_MASK to mask)
            decoder.run(inputs).use { result ->
                return mostLikelyLast(result.get(LOGITS).get() as OnnxTensor, config.padId)
            }
        }
    }

    private companion object {
        const val DECODER_INPUT = "input_ids"
        const val HIDDEN = "encoder_hidden_states"
        const val ENCODER_MASK = "encoder_attention_mask"
        const val LOGITS = "logits"
    }
}

package com.glyphdraft.engine.translation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Merged decoder with a key/value cache (the Optimum export of the `Xenova` models). The first step runs with the
 * cache branch off and returns the cache of the encoder output (kept for the whole sentence) and of the first id; every
 * later step feeds one id and the decoder cache of the step before.
 *
 * Memory: the decoder cache grows with every step, so the result of a step is closed as soon as the next step has read
 * it; only the first result (which owns the encoder cache) lives to the end.
 */
internal class CachedGreedyDecoder(
    private val decoder: OrtSession,
    private val config: MarianConfig,
) : GreedyDecoder {
    override fun decode(hidden: OnnxTensor, mask: OnnxTensor, maxNewTokens: Int, context: CoroutineContext): IntArray {
        val env = OrtEnvironment.getEnvironment()
        val generated = ArrayList<Int>()
        val empty = PastNames.all(config.layers).associateWith { emptyCache(env) }
        var first: OrtSession.Result? = null
        var latest: OrtSession.Result? = null
        try {
            var encoderCache: Map<String, OnnxTensor> = empty
            var decoderCache: Map<String, OnnxTensor> = empty
            var next = config.startId
            for (step in 0 until maxNewTokens) {
                context.ensureActive()
                val result = runStep(env, next, step, hidden, mask, encoderCache + decoderCache)
                if (step == 0) {
                    first = result
                    encoderCache = collect(result, PastNames.ENCODER)
                } else {
                    latest?.close()
                    latest = result
                }
                decoderCache = collect(result, PastNames.DECODER)

                next = mostLikelyLast(result.get(LOGITS).get() as OnnxTensor, config.padId)
                if (next == config.endId) {
                    break
                }

                generated.add(next)
            }
        } finally {
            latest?.close()
            first?.close()
            empty.values.forEach { it.close() }
        }

        return generated.toIntArray()
    }

    private fun runStep(
        env: OrtEnvironment,
        id: Int,
        step: Int,
        hidden: OnnxTensor,
        mask: OnnxTensor,
        cache: Map<String, OnnxTensor>,
    ): OrtSession.Result {
        longTensor(env, longArrayOf(id.toLong())).use { ids ->
            OnnxTensor.createTensor(env, booleanArrayOf(step > 0)).use { useCache ->
                val inputs = HashMap<String, OnnxTensor>(cache)
                inputs[INPUT] = ids
                inputs[HIDDEN] = hidden
                inputs[ENCODER_MASK] = mask
                inputs[USE_CACHE] = useCache
                return decoder.run(inputs)
            }
        }
    }

    /** At step 0 the cache branch is off; the cache inputs still need the right rank, with zero positions. */
    private fun emptyCache(env: OrtEnvironment): OnnxTensor =
        OnnxTensor.createTensor(env, FloatBuffer.allocate(0), longArrayOf(1, config.heads.toLong(), 0, config.headSize.toLong()))

    /** The cache tensors of [kind] in [result], under the names the next step reads them by. */
    private fun collect(result: OrtSession.Result, kind: String): Map<String, OnnxTensor> =
        PastNames.of(kind, config.layers).associate { name ->
            name to result.get(name.replace(PAST, PRESENT)).get() as OnnxTensor
        }

    private companion object {
        const val INPUT = "input_ids"
        const val HIDDEN = "encoder_hidden_states"
        const val ENCODER_MASK = "encoder_attention_mask"
        const val USE_CACHE = "use_cache_branch"
        const val LOGITS = "logits"
        const val PAST = "past_key_values"
        const val PRESENT = "present"
    }
}

/** Names of the cache inputs of the merged decoder: `past_key_values.<layer>.<decoder|encoder>.<key|value>`. */
private object PastNames {
    const val DECODER = "decoder"
    const val ENCODER = "encoder"

    fun of(kind: String, layers: Int): List<String> =
        (0 until layers).flatMap { layer -> listOf("key", "value").map { part -> "past_key_values.$layer.$kind.$part" } }

    fun all(layers: Int): List<String> = of(DECODER, layers) + of(ENCODER, layers)
}

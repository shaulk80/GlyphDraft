package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.flatMap

/**
 * The numbers of an OPUS-MT model that the decode loop needs, from its `config.json`. The special ids differ between
 * models (`he-en` ends on 25568 and starts on 60268, `en-de` ends on 0 and starts on 58100), so they are read here and
 * never written as constants.
 *
 * [startId] is the token the decoder starts with and [padId] the token that must never be generated; for Marian they are
 * the same id. [maxPositions] is the longest sequence the model has position embeddings for, and [maxLength] the
 * generation limit of the model card (null in some exports).
 */
data class MarianConfig(
    val startId: Int,
    val endId: Int,
    val padId: Int,
    val layers: Int,
    val heads: Int,
    val headSize: Int,
    val maxPositions: Int,
    val maxLength: Int,
) {
    /** The most tokens the decoder may write for a source of [sourceLength] ids: twice the source plus ten, within the model's limits. */
    fun generationLimit(sourceLength: Int): Int = minOf(maxLength, maxPositions, sourceLength * 2 + EXTRA_TOKENS)

    private companion object {
        const val EXTRA_TOKENS = 10
    }
}

object MarianConfigReader {
    private const val DEFAULT_MAX_LENGTH = 512

    fun read(configJson: String): Outcome<MarianConfig> =
        JsonReader.parse(configJson).flatMap { root ->
            val fields = root as? Map<*, *> ?: return@flatMap Outcome.Failure("config.json is not an object")
            build(fields)
        }

    private fun build(fields: Map<*, *>): Outcome<MarianConfig> {
        val missing = REQUIRED.filter { intOf(fields, it) == null }
        if (missing.isNotEmpty()) {
            return Outcome.Failure("config.json has no number for: ${missing.joinToString()}")
        }

        val heads = intOf(fields, "decoder_attention_heads")!!
        val modelSize = intOf(fields, "d_model")!!
        if (heads <= 0 || modelSize % heads != 0) {
            return Outcome.Failure("config.json has d_model $modelSize that does not divide into $heads heads")
        }

        val maxPositions = intOf(fields, "max_position_embeddings") ?: DEFAULT_MAX_LENGTH
        return Outcome.Success(
            MarianConfig(
                startId = intOf(fields, "decoder_start_token_id")!!,
                endId = intOf(fields, "eos_token_id")!!,
                padId = intOf(fields, "pad_token_id")!!,
                layers = intOf(fields, "decoder_layers")!!,
                heads = heads,
                headSize = modelSize / heads,
                maxPositions = maxPositions,
                maxLength = intOf(fields, "max_length") ?: maxPositions,
            ),
        )
    }

    /** A whole number under [key]; null when it is missing, null or not a number (some exports write `"max_length": null`). */
    private fun intOf(fields: Map<*, *>, key: String): Int? = (fields[key] as? Double)?.toInt()

    private val REQUIRED =
        listOf("decoder_start_token_id", "eos_token_id", "pad_token_id", "decoder_layers", "decoder_attention_heads", "d_model")
}

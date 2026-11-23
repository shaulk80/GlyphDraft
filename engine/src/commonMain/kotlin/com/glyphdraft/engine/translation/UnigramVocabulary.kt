package com.glyphdraft.engine.translation

/**
 * The pieces of a SentencePiece unigram model. Marian ids are not SentencePiece ids, so the id of a piece comes from
 * the model's own vocabulary ([pieceIds]) and the score that ranks a split comes from the tokenizer file ([scores]).
 *
 * The special pieces (end of sequence, unknown, padding) are never used to split text: a "</s>" typed by the user is
 * ordinary text and must not end the sequence.
 */
class UnigramVocabulary(
    private val scores: Map<String, Float>,
    private val pieceIds: Map<String, Int>,
) {
    val endOfSequenceId: Int = pieceIds[END_OF_SEQUENCE] ?: 0
    val unknownId: Int = pieceIds[UNKNOWN] ?: 1
    val padId: Int = pieceIds[PAD] ?: (size - 1)

    /** Number of ids, counting the highest id plus one so that an id gap does not shrink the output width. */
    val size: Int get() = (pieceIds.values.maxOrNull() ?: -1) + 1

    /** Pieces that text can be split into, with their scores. */
    val splitPieces: Map<String, Float> =
        scores.filterKeys { piece -> piece in pieceIds && piece !in SPECIAL_PIECES }

    /** Id of [piece], or null when the model does not know it. */
    fun idOf(piece: String): Int? = pieceIds[piece]

    /** Id to piece, for decoding. */
    val piecesById: Map<Int, String> = pieceIds.entries.associate { it.value to it.key }

    /** Ids that never appear in decoded text (an unknown piece would print as the literal text of its name). */
    fun isSpecial(id: Int): Boolean = id == endOfSequenceId || id == padId || id == unknownId

    private companion object {
        const val END_OF_SEQUENCE = "</s>"
        const val UNKNOWN = "<unk>"
        const val PAD = "<pad>"
        val SPECIAL_PIECES = setOf(END_OF_SEQUENCE, UNKNOWN, PAD)
    }
}

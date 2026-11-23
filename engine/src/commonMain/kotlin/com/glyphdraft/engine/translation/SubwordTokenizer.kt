package com.glyphdraft.engine.translation

/**
 * Turns a sentence into the token ids a translation model reads and the ids it writes back into text. The translator
 * (task 5) depends on this interface only, so a model with another tokenizer needs no change there.
 */
interface SubwordTokenizer {
    /** Id that ends a sequence; [encode] appends it and the decoder stops on it. */
    val endOfSequenceId: Int

    /** Id of the padding token; Marian models also start the decoder with it. */
    val padId: Int

    /** Number of ids the model has (the width of its output). */
    val vocabularySize: Int

    /** Ids of [text] followed by [endOfSequenceId]. A blank text gives just that id. */
    fun encode(text: String): IntArray

    /** Text of [ids]; special ids (end of sequence, padding) are left out. */
    fun decode(ids: List<Int>): String
}

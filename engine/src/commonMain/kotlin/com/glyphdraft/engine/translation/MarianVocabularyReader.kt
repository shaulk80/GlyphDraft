package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.flatMap

/**
 * Builds the [UnigramVocabulary] of an OPUS-MT model from the two files of its Hugging Face repository:
 * `vocab.json` (object: piece to Marian id) and `tokenizer.json` (`model.vocab`: array of `[piece, score]`).
 * `source.spm` is not needed.
 */
object MarianVocabularyReader {
    /** A readable problem is a failure with a message for the log; the caller shows its own text to the user. */
    fun read(vocabJson: String, tokenizerJson: String): Outcome<UnigramVocabulary> =
        readIds(vocabJson).flatMap { ids ->
            readScores(tokenizerJson).flatMap { scores -> check(UnigramVocabulary(scores, ids)) }
        }

    private fun readIds(vocabJson: String): Outcome<Map<String, Int>> =
        JsonReader.parse(vocabJson).flatMap { root ->
            val entries = root as? Map<*, *> ?: return@flatMap Outcome.Failure("vocab.json is not an object")
            val ids = HashMap<String, Int>(entries.size)
            for ((piece, id) in entries) {
                val number = id as? Double
                if (piece !is String || number == null) {
                    return@flatMap Outcome.Failure("vocab.json has an entry that is not a piece with a number")
                }
                ids[piece] = number.toInt()
            }

            Outcome.Success(ids)
        }

    private fun readScores(tokenizerJson: String): Outcome<Map<String, Float>> =
        JsonReader.parse(tokenizerJson).flatMap { root ->
            val vocab = ((root as? Map<*, *>)?.get("model") as? Map<*, *>)?.get("vocab") as? List<*>
                ?: return@flatMap Outcome.Failure("tokenizer.json has no model.vocab array")
            val scores = HashMap<String, Float>(vocab.size)
            for (entry in vocab) {
                val pair = entry as? List<*>
                val piece = pair?.getOrNull(0) as? String
                val score = pair?.getOrNull(1) as? Double
                if (piece == null || score == null) {
                    return@flatMap Outcome.Failure("tokenizer.json has a vocab entry that is not [piece, score]")
                }
                scores[piece] = score.toFloat()
            }

            Outcome.Success(scores)
        }

    /** Files from different models (almost no piece of one has an id in the other) are a failure, not a silent mess. */
    private fun check(vocabulary: UnigramVocabulary): Outcome<UnigramVocabulary> {
        if (vocabulary.splitPieces.isEmpty()) {
            return Outcome.Failure("The tokenizer files share no piece: they belong to different models")
        }

        return Outcome.Success(vocabulary)
    }
}

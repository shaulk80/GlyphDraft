package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * The part of a translation model that runs the network: the ids of one source sentence in, the ids of its translation
 * out (without the end id). An implementation owns native resources, so it is [AutoCloseable].
 */
interface SentenceModel : AutoCloseable {
    /**
     * Writes at most [maxNewTokens] ids. It checks [context] between steps and throws a `CancellationException` when the
     * job is cancelled; every other problem is an [Outcome.Failure].
     */
    fun generate(sourceIds: IntArray, maxNewTokens: Int, context: CoroutineContext): Outcome<IntArray>
}

/**
 * One loaded translation direction: turns sentences of the source language into sentences of the target language,
 * one at a time. Blank sentences are not sent to the model, and a sentence longer than the model's positions is cut at
 * the end (the segmenter keeps sentences far below that, so this only guards against a surprise).
 */
class DirectionTranslator(
    private val tokenizer: SubwordTokenizer,
    private val config: MarianConfig,
    private val model: SentenceModel,
) : AutoCloseable {
    /** The translation of each of [sentences], in order; stops at the first failure. */
    fun translate(sentences: List<String>, context: CoroutineContext): Outcome<List<String>> {
        val result = ArrayList<String>(sentences.size)
        for (sentence in sentences) {
            context.ensureActive()
            when (val translated = translateSentence(sentence, context)) {
                is Outcome.Failure -> return translated
                is Outcome.Success -> result.add(translated.value)
            }
        }

        return Outcome.Success(result)
    }

    override fun close() {
        model.close()
    }

    private fun translateSentence(sentence: String, context: CoroutineContext): Outcome<String> {
        if (sentence.isBlank()) {
            return Outcome.Success("")
        }

        val ids = fitted(tokenizer.encode(sentence))
        return when (val generated = model.generate(ids, config.generationLimit(ids.size), context)) {
            is Outcome.Failure -> generated
            is Outcome.Success -> Outcome.Success(tokenizer.decode(generated.value.toList()))
        }
    }

    /** [ids] cut to the positions the model has, keeping the end id as the last one. */
    private fun fitted(ids: IntArray): IntArray {
        if (ids.size <= config.maxPositions) {
            return ids
        }

        val cut = ids.copyOf(config.maxPositions)
        cut[cut.lastIndex] = tokenizer.endOfSequenceId
        return cut
    }
}

package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TranslationDirection
import kotlin.coroutines.CoroutineContext

/** Characters are the ids and 0 ends a sequence, so that a test reads a sentence off the ids without a vocabulary. */
internal class CharacterTokenizer : SubwordTokenizer {
    override val endOfSequenceId = 0
    override val padId = 1
    override val vocabularySize = 0x10000

    override fun encode(text: String): IntArray = IntArray(text.length + 1) { if (it < text.length) text[it].code else endOfSequenceId }

    override fun decode(ids: List<Int>): String = ids.filter { it > padId }.joinToString("") { it.toChar().toString() }
}

internal fun testConfig(maxPositions: Int = 512, maxLength: Int = 512): MarianConfig =
    MarianConfig(
        startId = 1,
        endId = 0,
        padId = 1,
        layers = 6,
        heads = 8,
        headSize = 64,
        maxPositions = maxPositions,
        maxLength = maxLength,
    )

/**
 * A model that writes its input and then a [tag] (`Hello.` becomes `Hello.[de]`), so that a test sees which models a text
 * went through and in which order. [onGenerate] runs first, with the number of the call, and may fail or cancel.
 */
internal class TaggingModel(
    private val tag: String,
    private val events: MutableList<String> = mutableListOf(),
    private val onGenerate: (Int, CoroutineContext) -> Outcome<Unit> = { _, _ -> Outcome.Success(Unit) },
) : SentenceModel {
    val sources = mutableListOf<IntArray>()
    val limits = mutableListOf<Int>()
    var closed = false
        private set

    override fun generate(sourceIds: IntArray, maxNewTokens: Int, context: CoroutineContext): Outcome<IntArray> {
        sources.add(sourceIds)
        limits.add(maxNewTokens)
        val checked = onGenerate(sources.size, context)
        if (checked is Outcome.Failure) {
            return checked
        }

        val text = sourceIds.filter { it != 0 }.joinToString("") { it.toChar().toString() } + tag
        return Outcome.Success(text.map { it.code }.toIntArray())
    }

    override fun close() {
        closed = true
        events.add("close $tag")
    }
}

/** Opens a [TaggingModel] per direction and records "open <from>-<to>" and "close <tag>" in [events]. */
internal class TaggingSource(
    val events: MutableList<String> = mutableListOf(),
    private val failing: Set<TranslationDirection> = emptySet(),
    private val onGenerate: (Int, CoroutineContext) -> Outcome<Unit> = { _, _ -> Outcome.Success(Unit) },
) : DirectionSource {
    override fun open(direction: TranslationDirection): Outcome<DirectionTranslator> {
        if (direction in failing) {
            return Outcome.Failure("cannot open ${direction.from.code}-${direction.to.code}")
        }

        events.add("open ${direction.from.code}-${direction.to.code}")
        val model = TaggingModel("[${direction.to.code}]", events, onGenerate)
        return Outcome.Success(DirectionTranslator(CharacterTokenizer(), testConfig(), model))
    }
}

package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Translation
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.Translator
import com.glyphdraft.core.isInstalled
import com.glyphdraft.core.packFor
import com.glyphdraft.core.translationRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Opens the installed model of one direction; a model that is missing or does not load is an [Outcome.Failure]. */
fun interface DirectionSource {
    fun open(direction: TranslationDirection): Outcome<DirectionTranslator>
}

/**
 * The on-device [Translator] (decision 0015): cuts the text into sentences, runs them through the models of the route
 * one direction after the other (de to he = de to en, en to ru, ru to he) and puts the translated sentences back into
 * the paragraphs of the original.
 *
 * At most one direction is open at a time: a model takes about 500 MB while loaded, so the previous direction is closed
 * before the next one is opened, and the last one stays open for the next call (the same direction again costs no load).
 * Calls run one after the other. [close] releases the open direction; call it when no translation is running.
 */
class OnDeviceTranslator(
    private val catalog: ModelCatalog,
    private val source: DirectionSource,
    private val segmenter: TextSegmenter = TextSegmenter(),
) : Translator,
    AutoCloseable {
    private val lock = Mutex()
    private var open: OpenDirection? = null

    override suspend fun translate(text: String, from: Language, to: Language): Outcome<Translation> {
        if (from == to) {
            return Outcome.Success(Translation(text, from, to, listOf(from)))
        }

        if (text.isBlank()) {
            return Outcome.Failure("There is no text to translate.")
        }

        val steps =
            catalog.translationRoutes().steps(from, to)
                ?: return Outcome.Failure("There is no translation from ${from.displayName} to ${to.displayName}.")
        val missing = steps.firstOrNull { !isInstalled(it) }
        if (missing != null) {
            return Outcome.Failure("The model for ${missing.from.displayName} to ${missing.to.displayName} is not installed.")
        }

        val route = listOf(from) + steps.map { it.to }
        return withContext(Dispatchers.Default) {
            lock.withLock { run(text, steps, route) }
        }
    }

    private fun isInstalled(direction: TranslationDirection): Boolean {
        val pack = catalog.packFor(direction)
        if (pack == null) {
            return false
        }

        return catalog.isInstalled(pack)
    }

    override fun close() {
        open?.translator?.close()
        open = null
    }

    private suspend fun run(text: String, steps: List<TranslationDirection>, route: List<Language>): Outcome<Translation> {
        val segmented = segmenter.segment(text)
        if (segmented.sentences.isEmpty()) {
            return Outcome.Failure("There is no text to translate.")
        }

        val context = currentCoroutineContext()
        var sentences = segmented.sentences
        for (step in steps) {
            val translator =
                when (val opened = directionOf(step)) {
                    is Outcome.Failure -> return opened
                    is Outcome.Success -> opened.value
                }

            sentences =
                when (val translated = translator.translate(sentences, context)) {
                    is Outcome.Failure -> return translated
                    is Outcome.Success -> repaired(sentences, translated.value, step.to)
                }
        }

        val from = route.first()
        val to = route.last()
        return when (val joined = segmented.join(sentences, to)) {
            is Outcome.Failure -> joined
            is Outcome.Success -> Outcome.Success(Translation(joined.value, from, to, route))
        }
    }

    /** [translations] of [sources] into [target], without the known slips of the model that writes that language. */
    private fun repaired(sources: List<String>, translations: List<String>, target: Language): List<String> {
        if (target != Language.Hebrew) {
            return translations
        }

        return sources.zip(translations, HebrewTimes::join)
    }

    /** The open translator of [direction]; the one that was open before is closed first. */
    private fun directionOf(direction: TranslationDirection): Outcome<DirectionTranslator> {
        val current = open
        if (current != null && current.direction == direction) {
            return Outcome.Success(current.translator)
        }

        close()
        return when (val opened = source.open(direction)) {
            is Outcome.Failure -> opened
            is Outcome.Success -> {
                open = OpenDirection(direction, opened.value)
                opened
            }
        }
    }

    private class OpenDirection(val direction: TranslationDirection, val translator: DirectionTranslator)
}

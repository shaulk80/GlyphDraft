package com.glyphdraft.engine.translation

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Translation
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.stub.StubModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class OnDeviceTranslatorTest {
    private val en = Language.English
    private val de = Language.German
    private val fr = Language.French
    private val ru = Language.Russian
    private val he = Language.Hebrew
    private val ja = Language.Japanese

    /** The shape of the shipping set: English is the hub, Hebrew is reached through Russian, nothing leads to Japanese. */
    private val directions =
        listOf(en to de, de to en, en to fr, fr to en, en to ru, ru to en, ja to en, ru to he).map { (from, to) -> TranslationDirection(from, to) }

    private fun packOf(direction: TranslationDirection) =
        ModelPack("mt-${direction.from.code}-${direction.to.code}", direction.from.displayName, emptySet(), 1, direction)

    private fun catalog(installed: List<TranslationDirection> = directions) =
        StubModelCatalog(directions.map(::packOf), installed.mapTo(mutableSetOf()) { packOf(it).id })

    private val source = TaggingSource()
    private val translator = OnDeviceTranslator(catalog(), source)

    private suspend fun success(text: String, from: Language, to: Language): Translation {
        val outcome = translator.translate(text, from, to)

        return (outcome as Outcome.Success).value
    }

    @Test
    fun aDirectPairIsTranslatedByItsModel() = runTest {
        val translation = success("Hello.", en, de)

        assertEquals("Hello.[de]", translation.text)
        assertEquals(listOf(en, de), translation.route)
        assertEquals(emptyList(), translation.via)
    }

    @Test
    fun aPairWithoutAModelOfItsOwnRunsThroughTheHubsInOrder() = runTest {
        val cases =
            listOf(
                Triple(de, fr, listOf(de, en, fr)),
                Triple(ja, de, listOf(ja, en, de)),
                Triple(de, he, listOf(de, en, ru, he)),
            )

        for ((from, to, route) in cases) {
            val translation = success("Text.", from, to)

            assertEquals(route, translation.route, "${from.code} to ${to.code}")
            assertEquals("Text." + route.drop(1).joinToString("") { "[${it.code}]" }, translation.text, "${from.code} to ${to.code}")
            assertEquals(route.drop(1).dropLast(1), translation.via, "${from.code} to ${to.code}")
        }
    }

    @Test
    fun paragraphsAndSentencesComeBackInTheLayoutOfTheOriginal() = runTest {
        val translation = success("First one. Second one.\nstill the second.\n\n\nThird one.", en, de)

        assertEquals("First one.[de] Second one. still the second.[de]\n\nThird one.[de]", translation.text)
    }

    @Test
    fun theSamePairNeedsNoModelAndReturnsTheTextUnchanged() = runTest {
        val untouched = OnDeviceTranslator(catalog(installed = emptyList()), source)

        val outcome = untouched.translate("  Same text.  ", en, en)

        assertEquals(Outcome.Success(Translation("  Same text.  ", en, en, listOf(en))), outcome)
        assertEquals(emptyList(), source.events)
    }

    @Test
    fun problemsTheCallerCanFixAreFailuresWithAMessage() = runTest {
        val partlyInstalled = OnDeviceTranslator(catalog(installed = listOf(TranslationDirection(de, en))), source)
        val cases =
            listOf(
                Triple(translator, "   \n", en to de),
                Triple(translator, "Hello.", en to ja),
                Triple(partlyInstalled, "Hallo.", de to fr),
            )

        for ((candidate, text, pair) in cases) {
            val outcome = candidate.translate(text, pair.first, pair.second)

            assertIs<Outcome.Failure>(outcome, "${pair.first.code} to ${pair.second.code}")
        }
        assertEquals(emptyList(), source.events)
    }

    @Test
    fun aMissingModelIsNamedInTheMessage() = runTest {
        val partlyInstalled = OnDeviceTranslator(catalog(installed = listOf(TranslationDirection(de, en))), source)

        val outcome = partlyInstalled.translate("Hallo.", de, fr) as Outcome.Failure

        assertTrue("English to French" in outcome.message, outcome.message)
    }

    @Test
    fun onlyOneDirectionIsOpenAtATimeAndTheLastOneStaysOpenForTheNextCall() = runTest {
        success("Hallo.", de, fr)
        success("Hallo.", de, fr)
        success("Hello.", en, fr)
        success("Hello.", en, de)

        assertEquals(
            listOf("open de-en", "close [en]", "open en-fr", "close [fr]", "open de-en", "close [en]", "open en-fr", "close [fr]", "open en-de"),
            source.events,
        )
    }

    @Test
    fun closeReleasesTheOpenDirection() = runTest {
        success("Hello.", en, de)

        translator.close()

        assertEquals(listOf("open en-de", "close [de]"), source.events)
    }

    @Test
    fun aDirectionThatDoesNotOpenIsAFailureWithItsMessage() = runTest {
        val broken = OnDeviceTranslator(catalog(), TaggingSource(failing = setOf(TranslationDirection(en, fr))))

        val outcome = broken.translate("Hallo.", de, fr)

        assertEquals(Outcome.Failure("cannot open en-fr"), outcome)
    }

    @Test
    fun aFailureInTheSecondModelOfARouteFailsTheWholeTranslation() = runTest {
        val failing = TaggingSource(onGenerate = { _, _ -> Outcome.Failure("out of memory") })
        val failed = OnDeviceTranslator(catalog(), failing).translate("Hallo.", de, fr)

        assertEquals(Outcome.Failure("out of memory"), failed)
    }

    @Test
    fun aCancelledTranslationStopsAndLeavesTheTranslatorUsable() = runTest {
        val cancelling =
            TaggingSource(
                onGenerate = { call, context ->
                    if (call == 1) {
                        context[Job]?.cancel()
                    }
                    Outcome.Success(Unit)
                },
            )
        val cancellable = OnDeviceTranslator(catalog(), cancelling)

        var cancelled = false
        val job =
            launch {
                try {
                    cancellable.translate("One. Two. Three.", en, de)
                } catch (e: CancellationException) {
                    cancelled = true
                    throw e
                }
            }
        job.join()
        val again = cancellable.translate("Hello.", en, de)

        assertTrue(cancelled)
        assertEquals("Hello.[de]", (again as Outcome.Success).value.text)
    }
}

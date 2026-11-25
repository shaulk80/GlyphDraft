package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class DirectionTranslatorTest {
    private val model = TaggingModel("[de]")
    private val translator = DirectionTranslator(CharacterTokenizer(), testConfig(), model)

    @Test
    fun translatesEverySentenceInOrder() {
        val result = translator.translate(listOf("One.", "Two.", "Three."), EmptyCoroutineContext)

        assertEquals(Outcome.Success(listOf("One.[de]", "Two.[de]", "Three.[de]")), result)
    }

    @Test
    fun anAddressIsNotSentToTheModel() {
        val result = translator.translate(listOf("Mail a@b.com now."), EmptyCoroutineContext)

        val read = CharacterTokenizer().decode(model.sources.single().toList())
        assertEquals("Mail Z19 now.", read)
        assertEquals(Outcome.Success(listOf("Mail a@b.com now.[de]")), result)
    }

    @Test
    fun aBlankSentenceIsNotSentToTheModel() {
        for (blank in listOf("", " ", "\t\n")) {
            val fresh = TaggingModel("[de]")
            val result = DirectionTranslator(CharacterTokenizer(), testConfig(), fresh).translate(listOf("A.", blank, "B."), EmptyCoroutineContext)

            assertEquals(Outcome.Success(listOf("A.[de]", "", "B.[de]")), result, "blank: '$blank'")
            assertEquals(2, fresh.sources.size, "blank: '$blank'")
        }
    }

    @Test
    fun theModelGetsTheIdsWithTheEndIdAndALimitFromTheSourceLength() {
        translator.translate(listOf("abc"), EmptyCoroutineContext)

        assertEquals(listOf('a'.code, 'b'.code, 'c'.code, 0), model.sources.single().toList())
        assertEquals(listOf(4 * 2 + 10), model.limits)
    }

    @Test
    fun aSentenceLongerThanThePositionsIsCutAndStillEndsWithTheEndId() {
        val short = TaggingModel("")
        val cutting = DirectionTranslator(CharacterTokenizer(), testConfig(maxPositions = 5), short)

        cutting.translate(listOf("abcdefgh"), EmptyCoroutineContext)

        assertEquals(listOf('a'.code, 'b'.code, 'c'.code, 'd'.code, 0), short.sources.single().toList())
    }

    @Test
    fun theFirstFailureStopsTheRunAndKeepsItsMessage() {
        val failing =
            TaggingModel("[de]") { call, _ ->
                if (call == 2) {
                    Outcome.Failure("the model broke")
                } else {
                    Outcome.Success(Unit)
                }
            }
        val result = DirectionTranslator(CharacterTokenizer(), testConfig(), failing).translate(listOf("A.", "B.", "C."), EmptyCoroutineContext)

        assertEquals(Outcome.Failure("the model broke"), result)
        assertEquals(2, failing.sources.size)
    }

    @Test
    fun aCancelledJobStopsBeforeTheNextSentence() {
        val job = Job()
        val cancelling =
            TaggingModel("[de]") { _, context ->
                context[Job]?.cancel()
                Outcome.Success(Unit)
            }
        val cancellable = DirectionTranslator(CharacterTokenizer(), testConfig(), cancelling)

        assertFailsWith<CancellationException> { cancellable.translate(listOf("A.", "B.", "C."), job) }
        assertEquals(1, cancelling.sources.size)
    }

    @Test
    fun closingTheTranslatorClosesTheModel() {
        translator.close()

        assertTrue(model.closed)
    }

    @Test
    fun noSentencesIsAnEmptySuccess() {
        assertIs<Outcome.Success<List<String>>>(translator.translate(emptyList(), EmptyCoroutineContext))
        assertEquals(emptyList(), model.sources)
    }
}

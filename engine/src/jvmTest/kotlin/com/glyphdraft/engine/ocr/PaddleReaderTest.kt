package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PaddleReaderTest {
    private val image = TestImages.solid(400, 100, PixelImage.WHITE)
    private val rows = listOf(listOf(lineBox(20.0, 100.0)), listOf(lineBox(50.0, 300.0)), listOf(lineBox(80.0, 200.0)))

    @Test
    fun choosesThePackWithTheHighestMeanConfidenceAndTiesGoToTheFirst() =
        runTest {
            val cases =
                listOf(
                    listOf("a" to 0.5f, "b" to 0.9f, "c" to 0.7f) to "b",
                    listOf("a" to 0.9f, "b" to 0.9f) to "a",
                    listOf("a" to 0.0f, "b" to 0.0f) to "a",
                    listOf("only" to 0.1f) to "only",
                )

            for ((confidences, expected) in cases) {
                val recognizers = confidences.associate { (id, confidence) -> id to FakeRecognizer(id, confidence) }
                val reader = PaddleReader(FakeModelSource(FakeDetector(emptyList()), recognizers))
                val best = reader.chooseBest(image, rows, confidences.map { it.first })
                assertEquals(Outcome.Success(expected), best, "confidences: $confidences")
            }
        }

    @Test
    fun probesOnlyTheTwoLongestLinesAndOpensOnlyWhatItNeeds() =
        runTest {
            val recognizers = mapOf("a" to FakeRecognizer("a", 0.5f), "b" to FakeRecognizer("b", 0.6f), "unused" to FakeRecognizer("unused", 0.9f))
            val source = FakeModelSource(FakeDetector(emptyList()), recognizers)

            PaddleReader(source).chooseBest(image, rows, listOf("a", "b"))

            assertEquals(listOf("a", "b"), source.opened)
            assertEquals(listOf(300, 200), recognizers.getValue("a").widths)
            assertEquals(listOf(300, 200), recognizers.getValue("b").widths)
        }

    @Test
    fun aSingleCandidateIsNotProbed() =
        runTest {
            val recognizer = FakeRecognizer("a", 0.5f)
            val source = FakeModelSource(FakeDetector(emptyList()), mapOf("a" to recognizer))

            assertEquals(Outcome.Success("a"), PaddleReader(source).chooseBest(image, rows, listOf("a")))
            assertEquals(emptyList(), recognizer.widths)
        }

    @Test
    fun keepsRecognizersOpenBetweenCallsAndClosesThemOnClose() =
        runTest {
            val detector = FakeDetector(listOf(lineBox(50.0, 300.0)))
            val recognizer = FakeRecognizer("a", 0.5f)
            val source = FakeModelSource(detector, mapOf("a" to recognizer))
            val reader = PaddleReader(source)

            reader.read(image, rows, "a")
            reader.read(image, rows, "a")
            reader.detectRows(image)

            assertEquals(listOf("a"), source.opened)
            assertEquals(false, recognizer.closed)
            reader.close()
            assertEquals(true, recognizer.closed)
            assertEquals(true, detector.closed)
        }

    @Test
    fun aPackThatCannotBeOpenedIsAFailure() =
        runTest {
            val reader = PaddleReader(FakeModelSource(FakeDetector(emptyList()), emptyMap()))

            assertIs<Outcome.Failure>(reader.read(image, rows, "missing"))
            assertIs<Outcome.Failure>(reader.chooseBest(image, rows, listOf("missing", "other")))
        }
}

package com.glyphdraft.engine.language

import com.glyphdraft.core.Language
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The expected text of every fixture (the `.txt` files in `src/test/resources/fixtures`) goes through the detector with all eight
 * picker languages as candidates and must come out in the language of `manifest.tsv`. No models needed: this is the
 * detector on perfect OCR output; the engine test with real models covers the imperfect kind.
 */
class FixtureLanguageTest {
    private val fixtures = File("src/test/resources/fixtures")
    private val byCode = Language.Supported.associateBy { it.code }

    @Test
    fun detectsTheLanguageOfEveryFixtureText() =
        runTest {
            val detector = TextLanguageDetector()
            val rows = manifest()
            assertTrue(rows.size >= 11, "the manifest has ${rows.size} rows")

            for ((name, code) in rows) {
                // The mixed page is English and Russian; Russian has most of its letters.
                val expected = byCode.getValue(if (code == "en+ru") "ru" else code)
                val text = File(fixtures, "$name.txt").readText(Charsets.UTF_8)

                val guess = detector.detect(text, Language.Supported.toSet())

                assertEquals(expected, guess?.language, "fixture $name")
            }
        }

    private fun manifest(): List<Pair<String, String>> =
        File(fixtures, "manifest.tsv")
            .readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line -> line.split('\t').let { it[0] to it[1] } }
}

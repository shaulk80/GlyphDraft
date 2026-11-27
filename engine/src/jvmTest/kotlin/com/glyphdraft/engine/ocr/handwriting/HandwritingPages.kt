package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.core.Language
import com.glyphdraft.engine.ocr.TestModels
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Who made a page: a person, or an image model that drew it. The two are reported apart (decision 0022). */
internal enum class Source { REAL, AI }

internal enum class Writing { PRINT, CURSIVE }

internal enum class Paper { LINED, PLAIN }

/** One row of the handwriting manifest: the page `<name>.jpg` and the text written on it, `<name>.txt`. */
internal data class HandwritingPage(
    val name: String,
    val language: Language,
    val source: Source,
    val writing: Writing,
    val paper: Paper,
) {
    val image: File get() = File(HandwritingPages.folder, "$name.jpg")
    val expected: File get() = File(HandwritingPages.folder, "$name.txt")
}

/** The handwriting fixtures (`src/test/resources/fixtures/handwriting`, see the README there). */
internal object HandwritingPages {
    const val MANIFEST = "manifest.tsv"
    private const val COLUMNS = 5

    val folder = File(TestModels.fixtures, "handwriting")

    fun load(): List<HandwritingPage> =
        File(folder, MANIFEST)
            .readLines(Charsets.UTF_8)
            .drop(1)
            .filter { it.isNotBlank() }
            .map { page(it) }

    private fun page(row: String): HandwritingPage {
        val cells = row.split('\t')
        assertEquals(COLUMNS, cells.size, "$MANIFEST row: $row")
        val (name, code, source, writing, paper) = cells
        val language = assertNotNull(Language.Supported.firstOrNull { it.code == code }, "$MANIFEST, $name: unknown language $code")

        return HandwritingPage(name, language, valueOf<Source>(source, name), valueOf<Writing>(writing, name), valueOf<Paper>(paper, name))
    }

    private inline fun <reified T : Enum<T>> valueOf(cell: String, name: String): T =
        assertNotNull(enumValues<T>().firstOrNull { it.name.lowercase() == cell }, "$MANIFEST, $name: unknown value $cell")
}

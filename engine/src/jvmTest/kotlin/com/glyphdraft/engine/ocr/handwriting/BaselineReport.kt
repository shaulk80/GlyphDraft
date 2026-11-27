package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.core.Language
import java.util.Locale

/** What the models made of one page. [note] says why a figure may stand out: nothing read, or the engine's doubt. */
internal data class PageScore(val page: HandwritingPage, val errorRate: Double, val note: String = "")

/** The Markdown tables of the handwriting baseline: the mean per language and kind of sample, then every page. */
internal object BaselineReport {
    fun markdown(scores: List<PageScore>): String {
        val lines = ArrayList<String>()
        lines.add("## Handwriting baseline (shipped models)")
        lines.add("")
        lines.add(
            "Character error rate (CER), each page read in its own language. Not counted: whitespace, quote and dash shapes, " +
                "blank length, full-width forms. Over 100 % means invented text. Reported only, never a gate.",
        )
        lines.add("")
        lines.addAll(languageTable(scores))
        lines.add("")
        lines.addAll(pageTable(scores))

        return lines.joinToString("\n", postfix = "\n")
    }

    private fun languageTable(scores: List<PageScore>): List<String> {
        val rows = scores.groupBy { it.page.language to it.page.source }
        val order = rows.keys.sortedWith(compareBy({ Language.Supported.indexOf(it.first) }, { it.second }))

        return listOf("| Language | Samples | Pages | Mean CER % |", "|---|---|---|---|") +
            order.map { key ->
                val pages = rows.getValue(key)
                val mean = pages.map { it.errorRate }.average()
                row(key.first.displayName, cell(key.second), pages.size.toString(), percent(mean))
            }
    }

    private fun pageTable(scores: List<PageScore>): List<String> =
        listOf("| Page | Writing | Paper | CER % | Note |", "|---|---|---|---|---|") +
            scores.map { row(it.page.name, cell(it.page.writing), cell(it.page.paper), percent(it.errorRate), it.note) }

    private fun row(vararg cells: String): String = cells.joinToString(" | ", prefix = "| ", postfix = " |")

    private fun cell(value: Enum<*>): String = value.name.lowercase()

    /** A point as the decimal separator on every machine. */
    private fun percent(rate: Double): String = String.format(Locale.ROOT, "%.1f", rate * 100)
}

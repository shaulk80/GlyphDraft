package com.glyphdraft.engine.layout

/**
 * One line of text and where it is on a level page, measured in the direction of the writing: [start] and [end] along
 * the line (for Hebrew, which runs to the left, the mirrored edges), [center] down the page and the [height] of the line.
 * [text] is trimmed and not blank.
 */
data class PlacedLine(
    val text: String,
    val start: Double,
    val end: Double,
    val center: Double,
    val height: Double,
)

/**
 * The text of a page twice, both with an empty line where the page has a gap between blocks: [text] line for line as on
 * the page, [runningText] with the lines that the page wrapped joined.
 */
data class PageText(val text: String, val runningText: String)

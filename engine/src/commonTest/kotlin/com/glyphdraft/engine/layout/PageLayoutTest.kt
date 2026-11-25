package com.glyphdraft.engine.layout

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pages are written as lines of height 20 whose characters are 10 wide; the usual distance of two lines is 24. */
class PageLayoutTest {
    /** A line that begins at [start], [gap] below the one before it. */
    private class Line(val text: String, val gap: Double = PITCH, val start: Double = 0.0, val height: Double = HEIGHT)

    private fun compose(vararg lines: Line): PageText {
        var center = 0.0
        return PageLayout.compose(
            lines.map { line ->
                center += line.gap
                PlacedLine(line.text, line.start, line.start + line.text.length * CHARACTER, center, line.height)
            },
        )
    }

    /** Whether [second] goes on from [first] in the running text. [full] is long enough to set the margin. */
    private fun joins(first: String, second: String): Boolean {
        val running = compose(Line(first), Line(second)).runningText

        return '\n' !in running
    }

    @Test
    fun aLineBreakIsJoinedOnlyWhereThePageWrappedTheText() {
        val full = "The archive was opened and then Forty"
        val cases: List<Triple<String, String, Boolean>> =
            listOf(
                // The next word had no room.
                Triple(full, "Two boxes were checked", true),
                // The sentence goes on in lower case.
                Triple("A short line", "that goes on", true),
                Triple("informa-", "tion is here", true),
                // A sentence ends, whatever the places say.
                Triple("$full.", "Two boxes were checked", false),
                Triple("$full?\"", "Two boxes were checked", false),
                // An abbreviation does not end one.
                Triple("The paper was read by the famous Dr.", "Ortiz and others", true),
                // A colon announces what follows.
                Triple("$full:", "two boxes", false),
                // A list mark starts a line.
                Triple(full, "- two boxes", false),
                Triple(full, "• two boxes", false),
                Triple(full, "2) two boxes", false),
                // A year at the beginning of a wrapped line is no list mark.
                Triple(full, "1950 was the year", true),
                // A short line with room to spare was meant to end.
                Triple("Item list", "Box 07", false),
                Triple("Comfortable shoes", "Your student card", false),
            )

        for ((first, second, joined) in cases) {
            assertEquals(joined, joins(first, second), "'$first' + '$second'")
        }
    }

    @Test
    fun aSentenceThatEndsAtTheMarginInsideAParagraphDoesNotBreakIt() {
        val full = "The museum is open from morning till"
        val ending = "night and the last visitors may stay."
        // the third line, whether it goes on in the paragraph
        val cases: List<Pair<List<String>, String>> =
            listOf(
                listOf(full, ending, "Groups should book in advance") to "$full $ending Groups should book in advance",
                // Room to spare: the paragraph ended here.
                listOf(full, "night.", "Groups should book in advance") to "$full night.\nGroups should book in advance",
                // Not inside a paragraph: the first of two sentences that each have a line.
                listOf(ending, "Groups should book in advance") to "$ending\nGroups should book in advance",
            )

        for ((lines, expected) in cases) {
            assertEquals(expected, compose(*lines.map { Line(it) }.toTypedArray()).runningText, "lines: $lines")
        }
    }

    @Test
    fun aShortLineIsNotAWrapEvenWhenItIsTheLongestOfThePage() {
        val page = compose(Line("A valid ticket"), Line("Your student card"), Line("A pen"))

        assertEquals("A valid ticket\nYour student card\nA pen", page.runningText)
    }

    @Test
    fun theMarginIsThatOfTheWholePage() {
        // Without the heading, the first item would be the widest line and reach the margin.
        val page = compose(Line("A heading line that is far wider than the list below it:"), Line("Box 07 Postcards of the town"), Line("Box 12 Ledgers"))

        assertEquals(page.text, page.runningText)
    }

    @Test
    fun chineseAndJapaneseLinesAreJoinedWithoutASpaceWhenTheyReachTheMargin() {
        val long = "博物馆每周二至周日开放闭馆前半小时停止入场超过十人的团体"
        val cases: List<Triple<String, String, String>> =
            listOf(
                Triple(long, "请提前预约。", "${long}请提前预约。"),
                Triple("$long。", "请携带门票", "$long。\n请携带门票"),
                Triple("开放时间", long, "开放时间\n$long"),
            )

        for ((first, second, expected) in cases) {
            assertEquals(expected, compose(Line(first), Line(second)).runningText, "'$first' + '$second'")
        }
    }

    @Test
    fun aGapBetweenBlocksIsAnEmptyLineInBothTexts() {
        val page =
            compose(
                Line("Title"),
                Line("The first paragraph is wrapped right", gap = 2 * PITCH),
                Line("here."),
                Line("Second", gap = 2 * PITCH),
                Line("Third"),
            )

        assertEquals("Title\n\nThe first paragraph is wrapped right\nhere.\n\nSecond\nThird", page.text)
        assertEquals("Title\n\nThe first paragraph is wrapped right here.\n\nSecond\nThird", page.runningText)
    }

    @Test
    fun whatCountsAsAGapDependsOnTheUsualDistanceOfTheLines() {
        // distance of the first two lines, distance of the others, whether the first one is a gap
        val cases: List<Triple<Double, Double, Boolean>> =
            listOf(
                Triple(24.0, 24.0, false),
                // Double spacing is the usual distance of its page.
                Triple(40.0, 40.0, false),
                Triple(80.0, 40.0, true),
                // Wider than usual, but too near to be a gap.
                Triple(32.0, 20.0, false),
                // Far apart is a gap even when every line is.
                Triple(70.0, 70.0, true),
            )

        for ((first, others, isGap) in cases) {
            val page = compose(Line("One."), Line("Two.", gap = first), Line("Three.", gap = others), Line("Four.", gap = others), Line("Five.", gap = others))

            assertEquals(isGap, page.text.startsWith("One.\n\n"), "first $first, others $others")
        }
    }

    @Test
    fun aStepUpToTheNextColumnIsAGap() {
        val page = compose(Line("Left column"), Line("goes on"), Line("Right column", gap = -PITCH, start = 500.0))

        assertEquals("Left column\ngoes on\n\nRight column", page.text)
    }

    @Test
    fun aLineThatMeasuresLowDoesNotMakeAGap() {
        // As Tesseract measures Hebrew: only the lines with a descender are tall.
        val page = compose(Line("One.", height = 32.0), Line("Two.", gap = 61.0, height = 27.0), Line("Three.", gap = 68.0, height = 20.0))

        assertEquals("One.\nTwo.\nThree.", page.text)
    }

    @Test
    fun noLinesAndOneLine() {
        assertEquals(PageText("", ""), PageLayout.compose(emptyList()))
        assertEquals(PageText("Alone", "Alone"), compose(Line("Alone")))
    }

    private companion object {
        const val CHARACTER = 10.0
        const val HEIGHT = 20.0
        const val PITCH = 24.0
    }
}

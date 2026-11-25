package com.glyphdraft.engine.ocr

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.ModelManifest
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/**
 * The layout of a whole page with the real models, after the page of the M3 hand test (findings 1 and 2): a heading, a
 * wrapped paragraph, a list with columns and two single lines. The page is drawn here, so that the places of its lines
 * are known. Needs the Latin models like [FixtureOcrTest] and does nothing without.
 */
class FixtureLayoutTest {
    /** One text line of the page: the baseline and the pieces with their left edges. */
    private class Drawn(val baseline: Int, vararg val pieces: Pair<Int, String>)

    private val page =
        listOf(
            Drawn(60, 40 to "GlyphDraft field test"),
            Drawn(150, 40 to "The archive opened at 9:15 on Monday, 23 November 2026. Forty-two boxes"),
            Drawn(192, 40 to "were checked, and 17 of them held letters written before 1950."),
            Drawn(280, 40 to "Item list"),
            Drawn(322, 60 to "Box 07", 220 to "Postcards, 1932-1941", 600 to "3 kg"),
            Drawn(364, 60 to "Box 12", 220 to "Ledgers, 1948", 600 to "11.5 kg"),
            Drawn(406, 60 to "Box 31", 220 to "Maps (fragile)", 600 to "2 kg"),
            Drawn(494, 40 to "\"Please keep the lights low,\" said Dr. Ortiz."),
            Drawn(582, 40 to "Contact: archive@example.org, room B-204."),
        )

    /** The running text: the paragraph in one line, everything else as on the page. Spaces are not compared. */
    private val expected =
        listOf(
            "GlyphDraft field test",
            "",
            "The archive opened at 9:15 on Monday, 23 November 2026. Forty-two boxes were checked, and 17 of them held letters written before 1950.",
            "",
            "Item list",
            "Box 07 Postcards, 1932-1941 3 kg",
            "Box 12 Ledgers, 1948 11.5 kg",
            "Box 31 Maps (fragile) 2 kg",
            "",
            "\"Please keep the lights low,\" said Dr. Ortiz.",
            "",
            "Contact: archive@example.org, room B-204.",
        )

    @Test
    fun theRunningTextKeepsTheLinesAndBlocksOfThePageAndJoinsTheWrappedParagraph() {
        val catalog = TestModels.catalogWith("FixtureLayoutTest", setOf(ModelManifest.LATIN)) ?: return
        val paddle = PaddleReader(PaddleModels(catalog))
        OnDeviceOcrEngine(AwtImageDecoder(), catalog, paddle, TextLanguageDetector()).use { engine ->
            val outcome = runBlocking { engine.recognize(ImageInput("page.png", draw()), Language.English) }

            val result = (outcome as Outcome.Success).value
            println(result.runningText)
            assertEquals(expected.map(::withoutSpaces), result.runningText.lines().map(::withoutSpaces), result.runningText)
            assertEquals(expected.size + 1, result.text.lines().size, result.text)
        }
    }

    private fun withoutSpaces(line: String): String = line.filterNot { it.isWhitespace() }

    private fun draw(): ByteArray {
        // Wide enough for the long line in the wider fonts of Linux.
        val image = BufferedImage(1500, 640, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.color = Color.BLACK
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 28)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        for (line in page) {
            for ((left, text) in line.pieces) {
                graphics.drawString(text, left, line.baseline)
            }
        }
        graphics.dispose()

        val bytes = ByteArrayOutputStream()
        ImageIO.write(image, "png", bytes)
        return bytes.toByteArray()
    }
}

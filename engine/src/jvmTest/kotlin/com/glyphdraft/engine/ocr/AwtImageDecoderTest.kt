package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AwtImageDecoderTest {
    private val decoder = AwtImageDecoder()

    private fun png(image: BufferedImage): ByteArray {
        val bytes = ByteArrayOutputStream()
        ImageIO.write(image, "png", bytes)
        return bytes.toByteArray()
    }

    @Test
    fun pixelsComeBackAsRgbRowByRow() {
        val image = BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB)
        val colors = intArrayOf(0xFF0000, 0x00FF00, 0x0000FF, 0x102030, 0xFFFFFF, 0x000000)
        for (i in colors.indices) {
            image.setRGB(i % 3, i / 3, colors[i])
        }

        val decoded = (decoder.decode(png(image)) as Outcome.Success).value

        assertEquals(3, decoded.width)
        assertEquals(2, decoded.height)
        assertContentEquals(colors, decoded.pixels)
    }

    @Test
    fun transparentPixelsAreBlendedWithWhite() {
        val image = BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0x00000000) // fully transparent
        image.setRGB(1, 0, 0x80000000.toInt()) // black, alpha 128
        image.setRGB(2, 0, 0xFF336699.toInt()) // opaque

        val decoded = (decoder.decode(png(image)) as Outcome.Success).value

        assertEquals(0xFFFFFF, decoded.pixels[0])
        assertEquals(0x7F7F7F, decoded.pixels[1])
        assertEquals(0x336699, decoded.pixels[2])
    }

    @Test
    fun bytesThatAreNotAnImageAreAFailure() {
        val cases = mapOf("empty" to ByteArray(0), "text" to "hello".toByteArray(), "cut off" to png(BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB)).copyOf(30))

        for ((name, bytes) in cases) {
            assertIs<Outcome.Failure>(decoder.decode(bytes), name)
        }
    }

    @Test
    fun aFixtureImageIsDecodedAtItsFullSize() {
        val decoded = (decoder.decode(File("src/test/resources/fixtures/en-clean.png").readBytes()) as Outcome.Success).value

        assertEquals(1100, decoded.width)
    }
}

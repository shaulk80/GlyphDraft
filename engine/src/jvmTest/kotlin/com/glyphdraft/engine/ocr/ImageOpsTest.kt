package com.glyphdraft.engine.ocr

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ImageOpsTest {
    private fun red(image: PixelImage, x: Int, y: Int) = image.red(y * image.width + x)

    private fun green(image: PixelImage, x: Int, y: Int) = image.green(y * image.width + x)

    @Test
    fun resizeToTheSameSizeKeepsEveryPixel() {
        val image = TestImages.gradient(12, 9)

        assertContentEquals(image.pixels, ImageOps.resize(image, 12, 9).pixels)
    }

    @Test
    fun resizeKeepsASolidColourAtAnySize() {
        val image = TestImages.solid(10, 6, 0x336699)

        for ((width, height) in listOf(5 to 3, 20 to 12, 7 to 11)) {
            val resized = ImageOps.resize(image, width, height)

            assertEquals(width, resized.width)
            assertEquals(height, resized.height)
            assertEquals(setOf(0x336699), resized.pixels.toSet(), "$width x $height")
        }
    }

    @Test
    fun cropOfALevelBoxIsThePartUnderIt() {
        val image = TestImages.gradient(20, 20)

        // 4 x 2 box whose pixel centres fall on the pixels x = 3..6, y = 5..6.
        val crop = ImageOps.cropUpright(image, TextBox(cx = 4.5, cy = 5.5, length = 4.0, thickness = 2.0, angle = 0.0))

        assertEquals(4, crop.width)
        assertEquals(2, crop.height)
        for (y in 0..1) {
            for (x in 0..3) {
                assertEquals((3 + x) * 10, red(crop, x, y), "red at $x, $y")
                assertEquals((5 + y) * 10, green(crop, x, y), "green at $x, $y")
            }
        }
    }

    @Test
    fun cropOfABoxTurnedAQuarterReadsDownwards() {
        val image = TestImages.gradient(20, 20)

        // The text direction points down the image, so the crop's rows run down: its x axis is the image's y axis.
        val crop = ImageOps.cropUpright(image, TextBox(cx = 4.5, cy = 5.5, length = 4.0, thickness = 2.0, angle = Math.PI / 2))

        assertEquals(50, red(crop, 0, 0))
        assertEquals(40, green(crop, 0, 0))
        assertEquals(50, red(crop, 3, 0))
        assertEquals(70, green(crop, 3, 0))
        assertEquals(40, red(crop, 0, 1))
    }

    @Test
    fun partsOfABoxOutsideTheImageAreWhite() {
        val image = TestImages.solid(10, 10, 0x000000)

        val crop = ImageOps.cropUpright(image, TextBox(cx = 100.0, cy = 100.0, length = 6.0, thickness = 4.0, angle = 0.3))

        assertEquals(setOf(PixelImage.WHITE), crop.pixels.toSet())
    }
}

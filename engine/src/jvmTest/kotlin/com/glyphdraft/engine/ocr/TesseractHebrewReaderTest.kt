package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.ModelManifest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TesseractHebrewReaderTest {
    @Test
    fun pixelsBecomeRedGreenBlueBytesRowByRow() {
        val image = PixelImage(2, 1, intArrayOf(0x010203, 0xFF8000))

        val bytes = image.toRgbBytes()

        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0x80.toByte(), 0x00), bytes)
    }

    @Test
    fun blankLinesAreDroppedAndTheTextIsTrimmed() {
        val cases =
            listOf(
                "שלום\n\nעולם\n\n" to "שלום\nעולם",
                "אחד\r\n\r\nשניים\r\n" to "אחד\nשניים",
                "  אחד  \n \t \n  שניים  " to "אחד\nשניים",
                "אחד\nשניים" to "אחד\nשניים",
                "\n\n" to "",
                "" to "",
            )

        for ((raw, expected) in cases) {
            assertEquals(expected, raw.withoutBlankLines(), "'${raw.replace("\n", "\\n")}'")
        }
    }

    @Test
    fun withoutTheHebrewDataEveryCallFailsWithAMessage() =
        runTest {
            val folder = Files.createTempDirectory("hebrew-reader-test")
            try {
                TesseractHebrewReader(FileModelCatalog(ModelManifest.packs, folder)).use { reader ->
                    val image = PixelImage(1, 1, intArrayOf(PixelImage.WHITE))

                    val probe = reader.probe(image)
                    val read = reader.read(image)

                    assertIs<Outcome.Failure>(probe)
                    assertIs<Outcome.Failure>(read)
                    assertTrue(read.message.contains("heb.traineddata"), read.message)
                }
            } finally {
                folder.toFile().deleteRecursively()
            }
        }
}

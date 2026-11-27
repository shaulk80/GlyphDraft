package com.glyphdraft.engine.ocr.handwriting

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.ocr.AwtImageDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The rules every handwriting fixture keeps (README next to them); no models needed. The images are public, so the one
 * that matters most is that none carries metadata (a phone photo records where it was taken).
 */
class HandwritingFixturesTest {
    private val pages = HandwritingPages.load()

    @Test
    fun theManifestAndTheFolderListTheSamePages() {
        assertTrue(pages.isNotEmpty(), "the manifest is empty")
        assertEquals(pages.map { it.name }.distinct(), pages.map { it.name }, "a page is listed twice")

        val listed = pages.flatMap { listOf(it.image.name, it.expected.name) } + HandwritingPages.MANIFEST + "README.md"
        assertEquals(listed.sorted(), HandwritingPages.folder.list().orEmpty().sorted())
    }

    @Test
    fun everyPageHasItsTextAndASmallImageWithoutMetadata() {
        val problems = pages.flatMap { page -> problemsOf(page).map { "${page.name}: $it" } }

        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    private fun problemsOf(page: HandwritingPage): List<String> {
        val problems = ArrayList<String>()
        if (page.expected.readText(Charsets.UTF_8).isBlank()) {
            problems.add("the expected text is empty")
        }

        val bytes = page.image.readBytes()
        if (bytes.size > MAX_BYTES) {
            problems.add("the image has ${bytes.size} bytes, more than $MAX_BYTES")
        }

        if (!isOneJpegWithoutMetadata(bytes)) {
            problems.add("the image is not a JPEG without metadata (use scripts/fixtures/PrepareHandwriting.java)")
        }

        when (val decoded = AwtImageDecoder().decode(bytes)) {
            is Outcome.Failure -> problems.add(decoded.message)
            is Outcome.Success -> {
                val longSide = maxOf(decoded.value.width, decoded.value.height)
                if (longSide > MAX_LONG_SIDE) {
                    problems.add("the long side is $longSide pixels, more than $MAX_LONG_SIDE")
                }
            }
        }

        return problems
    }

    /**
     * True when [file] starts as a JPEG, has no application segment besides JFIF (EXIF, XMP and the like) and no comment
     * before the image data, and nothing follows its end (phones append further pictures or video there).
     */
    private fun isOneJpegWithoutMetadata(file: ByteArray): Boolean {
        fun byteAt(index: Int): Int = file.getOrNull(index)?.toInt()?.and(0xFF) ?: -1

        if (byteAt(0) != 0xFF || byteAt(1) != START_OF_IMAGE) {
            return false
        }

        var position = 2
        while (byteAt(position) == 0xFF && byteAt(position + 1) != START_OF_SCAN) {
            val marker = byteAt(position + 1)
            if (marker in METADATA_SEGMENTS || marker == COMMENT) {
                return false
            }

            position += 2 + (byteAt(position + 2) shl 8) + byteAt(position + 3)
        }

        val end = (position until file.size - 1).firstOrNull { byteAt(it) == 0xFF && byteAt(it + 1) == END_OF_IMAGE }
        return end == file.size - 2
    }

    private companion object {
        const val MAX_BYTES = 1024 * 1024
        const val MAX_LONG_SIDE = 2048
        const val START_OF_IMAGE = 0xD8
        const val START_OF_SCAN = 0xDA
        const val END_OF_IMAGE = 0xD9
        const val COMMENT = 0xFE

        /** APP1 to APP15; APP0 is the JFIF header. */
        val METADATA_SEGMENTS = 0xE1..0xEF
    }
}

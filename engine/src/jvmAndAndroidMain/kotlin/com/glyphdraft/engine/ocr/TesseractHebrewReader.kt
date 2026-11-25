package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.layout.PlacedLine
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.ModelManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.tesseract.ResultIterator
import org.bytedeco.tesseract.TessBaseAPI
import org.bytedeco.tesseract.global.tesseract

/**
 * Reads Hebrew with Tesseract (`heb` from `tessdata_fast`) through the JavaCPP presets, the same code on the desktop and
 * on Android. Tesseract is opened on first use and kept until [close]; it is not thread safe, so calls are one at a time.
 *
 * [probe] and [read] both need a full recognition. The engine calls them one after the other with the same image, so the
 * result of the last page is kept and the second call costs nothing.
 *
 * The JavaCPP cache folder has to be set before the first use (`JavaCppNativeLibraries.prepare` on the desktop).
 */
class TesseractHebrewReader(private val catalog: FileModelCatalog) : HebrewReader {
    private val lock = Any()
    private var api: TessBaseAPI? = null
    private var last: PageRead? = null

    override suspend fun probe(image: PixelImage): Outcome<Float> =
        withContext(Dispatchers.Default) {
            when (val page = pageOf(image)) {
                is Outcome.Failure -> page
                is Outcome.Success -> Outcome.Success(page.value.confidence)
            }
        }

    override suspend fun read(image: PixelImage): Outcome<List<PlacedLine>> =
        withContext(Dispatchers.Default) {
            when (val page = pageOf(image)) {
                is Outcome.Failure -> page
                is Outcome.Success -> Outcome.Success(page.value.lines)
            }
        }

    override fun close() {
        synchronized(lock) {
            api?.End()
            api?.close()
            api = null
            last = null
        }
    }

    private fun pageOf(image: PixelImage): Outcome<PageRead> =
        synchronized(lock) {
            val cached = last
            if (cached != null && cached.image === image) {
                return@synchronized Outcome.Success(cached)
            }

            val read = recognize(image)
            if (read is Outcome.Success) {
                last = read.value
            }

            read
        }

    private fun recognize(image: PixelImage): Outcome<PageRead> {
        try {
            val tesseractApi =
                when (val opened = openApi()) {
                    is Outcome.Failure -> return opened
                    is Outcome.Success -> opened.value
                }

            tesseractApi.SetImage(image.toRgbBytes(), image.width, image.height, RGB_BYTES_PER_PIXEL, image.width * RGB_BYTES_PER_PIXEL)
            // The decoded image has no resolution; without this Tesseract guesses one and logs it for every page.
            tesseractApi.SetSourceResolution(SOURCE_DPI)
            if (tesseractApi.Recognize(null) != 0) {
                return Outcome.Failure("Tesseract could not read the image.")
            }

            val lines = readLines(tesseractApi)
            val confidence = tesseractApi.MeanTextConf() / PERCENT
            tesseractApi.Clear()
            return Outcome.Success(PageRead(image, lines, confidence))
        } catch (e: UnsatisfiedLinkError) {
            return Outcome.Failure("Tesseract native library could not be loaded: ${e.message}", e)
        } catch (e: NoClassDefFoundError) {
            // JavaCPP reports a failed library load of a class initializer as this on later uses.
            return Outcome.Failure("Tesseract native library could not be loaded: ${e.message}", e)
        } catch (e: RuntimeException) {
            return Outcome.Failure("Tesseract failed: ${e.message}", e)
        }
    }

    private fun openApi(): Outcome<TessBaseAPI> {
        val open = api
        if (open != null) {
            return Outcome.Success(open)
        }

        if (!catalog.hasFile(ModelManifest.hebrewData)) {
            return Outcome.Failure("The model file ${ModelManifest.hebrewData.path} is not installed.")
        }

        val dataFolder = catalog.pathOf(ModelManifest.hebrewData).parent.toAbsolutePath().toString()
        val created = TessBaseAPI()
        if (created.Init(dataFolder, LANGUAGE) != 0) {
            created.close()
            return Outcome.Failure("Tesseract could not load the Hebrew data from $dataFolder.")
        }

        created.SetPageSegMode(tesseract.PSM_AUTO)
        api = created
        return Outcome.Success(created)
    }

    /**
     * The text lines of the recognized page. Tesseract also groups them into paragraphs, but splits a page where the
     * image has no gap (the Hebrew fixture), so only the places of the lines are used.
     */
    private fun readLines(tesseractApi: TessBaseAPI): List<PlacedLine> {
        val iterator = tesseract.TessBaseAPIGetIterator(tesseractApi)
        if (iterator == null || iterator.isNull) {
            return emptyList()
        }

        try {
            val lines = ArrayList<PlacedLine>()
            do {
                readLine(iterator)?.let(lines::add)
            } while (tesseract.TessResultIteratorNext(iterator, tesseract.RIL_TEXTLINE))

            return lines
        } finally {
            tesseract.TessResultIteratorDelete(iterator)
        }
    }

    /** The line [iterator] stands on, or null when it has no text. */
    private fun readLine(iterator: ResultIterator): PlacedLine? {
        val pointer = tesseract.TessResultIteratorGetUTF8Text(iterator, tesseract.RIL_TEXTLINE)
        if (pointer == null || pointer.isNull) {
            return null
        }

        val text =
            try {
                pointer.string
            } finally {
                tesseract.TessDeleteText(pointer)
            }

        val left = IntArray(1)
        val top = IntArray(1)
        val right = IntArray(1)
        val bottom = IntArray(1)
        val place = tesseract.TessResultIteratorGetPageIterator(iterator)
        if (!tesseract.TessPageIteratorBoundingBox(place, tesseract.RIL_TEXTLINE, left, top, right, bottom)) {
            return null
        }

        return hebrewLine(text, left[0], top[0], right[0], bottom[0])
    }

    private class PageRead(val image: PixelImage, val lines: List<PlacedLine>, val confidence: Float)

    private companion object {
        const val LANGUAGE = "heb"
        const val RGB_BYTES_PER_PIXEL = 3
        const val PERCENT = 100f
        const val SOURCE_DPI = 300
    }
}

/**
 * The line of [text] in the pixel rectangle given, or null when the text is blank. Hebrew runs to the left, so the line
 * starts at its [right] edge: the edges are mirrored.
 */
internal fun hebrewLine(text: String, left: Int, top: Int, right: Int, bottom: Int): PlacedLine? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    return PlacedLine(
        text = trimmed,
        start = -right.toDouble(),
        end = -left.toDouble(),
        center = (top + bottom) / 2.0,
        height = (bottom - top).toDouble(),
    )
}

/** The pixels as bytes R, G, B per pixel, row by row: the layout `TessBaseAPI.SetImage` takes with 3 bytes per pixel. */
internal fun PixelImage.toRgbBytes(): ByteArray {
    val bytes = ByteArray(pixels.size * 3)
    for (index in pixels.indices) {
        bytes[index * 3] = red(index).toByte()
        bytes[index * 3 + 1] = green(index).toByte()
        bytes[index * 3 + 2] = blue(index).toByte()
    }

    return bytes
}

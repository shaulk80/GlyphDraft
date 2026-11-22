package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.ModelManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun read(image: PixelImage): Outcome<String> =
        withContext(Dispatchers.Default) {
            when (val page = pageOf(image)) {
                is Outcome.Failure -> page
                is Outcome.Success -> Outcome.Success(page.value.text)
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

            val text = readText(tesseractApi)
            val confidence = tesseractApi.MeanTextConf() / PERCENT
            tesseractApi.Clear()
            return Outcome.Success(PageRead(image, text.withoutBlankLines(), confidence))
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

    private fun readText(tesseractApi: TessBaseAPI): String {
        val pointer = tesseractApi.GetUTF8Text() ?: return ""
        try {
            return pointer.string
        } finally {
            pointer.deallocate()
        }
    }

    private class PageRead(val image: PixelImage, val text: String, val confidence: Float)

    private companion object {
        const val LANGUAGE = "heb"
        const val RGB_BYTES_PER_PIXEL = 3
        const val PERCENT = 100f
        const val SOURCE_DPI = 300
    }
}

/**
 * One text line per line break, as the PP-OCRv5 readers give it. Tesseract ends every paragraph with an empty line and
 * may split a page into paragraphs where the image has none (the three lines of the Hebrew fixture came back with an
 * empty line after the second), so empty lines are dropped; the page's own paragraph gaps are lost with them.
 */
internal fun String.withoutBlankLines(): String =
    lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

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

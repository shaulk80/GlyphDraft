package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.leptonica.global.leptonica
import org.bytedeco.tesseract.TessBaseAPI

/**
 * Loads the JavaCPP Tesseract and Leptonica native libraries, asks Tesseract for its version and makes and frees a
 * small Leptonica image. It needs no language data, so it runs without a download; it proves only that the native
 * libraries of this OS or ABI can be found and loaded, which is the part that differs between Windows, Linux and
 * Android.
 */
object TesseractSelfTest {
    /** The message of the success outcome; the smoke tests look for it in the log. */
    const val OK_MESSAGE = "tesseract self-test: ok"

    private const val IMAGE_SIZE = 8
    private const val IMAGE_DEPTH = 8

    suspend fun run(): Outcome<String> = withContext(Dispatchers.IO) { runBlockingCall() }

    private fun runBlockingCall(): Outcome<String> {
        try {
            val version = TessBaseAPI.Version().string
            if (version.isNullOrBlank()) {
                return Outcome.Failure("Tesseract returned no version.")
            }

            val pix = leptonica.pixCreate(IMAGE_SIZE, IMAGE_SIZE, IMAGE_DEPTH)
            if (pix == null || pix.isNull) {
                return Outcome.Failure("Leptonica could not create an image.")
            }

            val width = leptonica.pixGetWidth(pix)
            leptonica.pixDestroy(pix)
            if (width != IMAGE_SIZE) {
                return Outcome.Failure("Leptonica made an image of width $width instead of $IMAGE_SIZE.")
            }

            return Outcome.Success("$OK_MESSAGE (Tesseract $version)")
        } catch (e: UnsatisfiedLinkError) {
            return Outcome.Failure("Tesseract native library could not be loaded: ${e.message}", e)
        } catch (e: NoClassDefFoundError) {
            // JavaCPP reports a failed library load of a class initializer as this on later uses.
            return Outcome.Failure("Tesseract native library could not be loaded: ${e.message}", e)
        } catch (e: RuntimeException) {
            return Outcome.Failure("Tesseract failed: ${e.message}", e)
        }
    }
}

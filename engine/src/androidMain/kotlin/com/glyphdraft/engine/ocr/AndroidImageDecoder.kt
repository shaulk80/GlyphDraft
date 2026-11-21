package com.glyphdraft.engine.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.glyphdraft.core.Outcome
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * [ImageDecoder] for Android: `BitmapFactory` (PNG, JPEG, GIF, BMP, WebP, and HEIF where the device has the codec).
 * A phone photo is 12 to 50 MP, which as a bitmap would be 50 to 200 MB, so the image is decoded at a smaller size
 * ([MAX_SIDE] pixels on the longer side, a power-of-two fraction of the original); the detector scales the image down
 * anyway. The EXIF orientation of photos is applied, and transparent pixels are put on white.
 */
class AndroidImageDecoder : ImageDecoder {
    override fun decode(bytes: ByteArray): Outcome<PixelImage> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return Outcome.Failure("The file is not an image this app can read (it reads PNG, JPEG, GIF, BMP and WebP).")
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        val bitmap =
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } catch (e: OutOfMemoryError) {
                return Outcome.Failure("The image is too large for this device's memory.", e)
            }

        if (bitmap == null) {
            return Outcome.Failure("The image could not be read.")
        }

        return Outcome.Success(toPixels(upright(bitmap, orientationOf(bytes))))
    }

    /** The smallest power of two that brings the longer side to [MAX_SIDE] or less. */
    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > MAX_SIDE) {
            sample *= 2
        }

        return sample
    }

    private fun orientationOf(bytes: ByteArray): Int =
        try {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            // Not a format with EXIF data (PNG, for one): the image is taken as it is.
            ExifInterface.ORIENTATION_NORMAL
        }

    /** [bitmap] turned and mirrored as its EXIF [orientation] says; the same bitmap when none is needed. */
    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return turned
    }

    private fun toPixels(bitmap: Bitmap): PixelImage {
        val argb = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val image = PixelImage(bitmap.width, bitmap.height, IntArray(argb.size) { PixelImage.onWhite(argb[it]) })
        bitmap.recycle()
        return image
    }

    private companion object {
        const val MAX_SIDE = 4096
    }
}

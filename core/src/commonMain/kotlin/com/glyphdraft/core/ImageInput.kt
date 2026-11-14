package com.glyphdraft.core

/** An image handed to an engine: platform-neutral, just the file name and its encoded bytes (PNG, JPEG, ...). */
class ImageInput(val name: String, val bytes: ByteArray) {
    override fun toString(): String = "ImageInput(name=$name, size=${bytes.size})"

    companion object {
        private const val BYTES_PER_MB = 1024L * 1024L

        /** Largest image the app reads into memory. */
        const val MAX_BYTES = 50L * BYTES_PER_MB

        fun tooLargeMessage(name: String, size: Long): String =
            "The file $name is too large (${size / BYTES_PER_MB} MB; the limit is ${MAX_BYTES / BYTES_PER_MB} MB)."
    }
}

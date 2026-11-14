package com.glyphdraft.core

/** An image handed to an engine: platform-neutral, just the file name and its encoded bytes (PNG, JPEG, ...). */
class ImageInput(val name: String, val bytes: ByteArray) {
    override fun toString(): String = "ImageInput(name=$name, size=${bytes.size})"
}

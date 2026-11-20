package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tells JavaCPP (Tesseract, Leptonica) to unpack its native libraries into a folder we choose.
 *
 * Left alone, JavaCPP extracts a few dozen files into `~/.javacpp/cache`, outside the data folder. The property
 * `org.bytedeco.javacpp.cachedir` is read when JavaCPP first loads a library, so call [prepare] before the first
 * use of Tesseract or Leptonica.
 */
object JavaCppNativeLibraries {
    const val CACHE_PROPERTY = "org.bytedeco.javacpp.cachedir"

    /** Creates [target], sets [CACHE_PROPERTY] to it and returns it. */
    fun prepare(target: Path): Outcome<Path> {
        try {
            Files.createDirectories(target)
        } catch (e: IOException) {
            return Outcome.Failure("Cannot create the JavaCPP cache folder $target: ${e.message}", e)
        }

        System.setProperty(CACHE_PROPERTY, target.toAbsolutePath().toString())
        return Outcome.Success(target)
    }
}

package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Puts the ONNX Runtime native libraries of this OS into a folder we choose and tells ONNX Runtime to use them.
 *
 * Left alone, ONNX Runtime extracts about 20 MB into the system temp folder on every start and cannot delete them
 * on Windows. The JVM reads `java.io.tmpdir` once at startup, so the app cannot redirect the temp folder from
 * `main`; the property `onnxruntime.native.path` is read when ONNX Runtime loads, so it can. Call [prepare] before
 * the first use of ONNX Runtime. ONNX Runtime still makes (and on exit removes) an empty folder in the temp folder.
 */
object OnnxNativeLibraries {
    const val PATH_PROPERTY = "onnxruntime.native.path"
    private const val RESOURCE_ROOT = "ai/onnxruntime/native"
    private val REQUIRED = listOf("onnxruntime", "onnxruntime4j_jni")
    private val OPTIONAL = listOf("onnxruntime_providers_shared")

    /** Copies the libraries not yet in [target] (or of another size) and sets [PATH_PROPERTY]; returns [target]. */
    fun prepare(
        target: Path,
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch"),
    ): Outcome<Path> {
        val platform = platformFolder(osName, osArch)
        if (platform == null) {
            return Outcome.Failure("No ONNX Runtime libraries for $osName on $osArch.")
        }

        try {
            Files.createDirectories(target)
            for (name in REQUIRED + OPTIONAL) {
                val copied = copyIfNeeded(platform, name, target, required = name in REQUIRED)
                if (copied is Outcome.Failure) {
                    return copied
                }
            }
        } catch (e: IOException) {
            return Outcome.Failure("Cannot prepare the ONNX Runtime libraries in $target: ${e.message}", e)
        }

        System.setProperty(PATH_PROPERTY, target.toAbsolutePath().toString())
        return Outcome.Success(target)
    }

    internal fun platformFolder(osName: String, osArch: String): String? {
        val os =
            when {
                osName.startsWith("Windows", ignoreCase = true) -> "win"
                osName.startsWith("Linux", ignoreCase = true) -> "linux"
                else -> return null
            }
        val arch =
            when (osArch.lowercase()) {
                "amd64", "x86_64" -> "x64"
                "aarch64", "arm64" -> "aarch64"
                else -> return null
            }
        return "$os-$arch"
    }

    private fun copyIfNeeded(platform: String, name: String, target: Path, required: Boolean): Outcome<Unit> {
        val fileName = System.mapLibraryName(name)
        val resource = "$RESOURCE_ROOT/$platform/$fileName"
        val url = OnnxNativeLibraries::class.java.classLoader.getResource(resource)
        if (url == null && required) {
            return Outcome.Failure("The ONNX Runtime library $resource is missing from the app.")
        }

        if (url == null) {
            return Outcome.Success(Unit)
        }

        val connection = url.openConnection()
        val destination = target.resolve(fileName)
        if (Files.exists(destination) && Files.size(destination) == connection.contentLengthLong) {
            return Outcome.Success(Unit)
        }

        // Copy next to the destination and rename, so a crash never leaves a half-written library behind.
        val partial = target.resolve("$fileName.part")
        connection.getInputStream().use { Files.copy(it, partial, StandardCopyOption.REPLACE_EXISTING) }
        Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING)
        return Outcome.Success(Unit)
    }
}

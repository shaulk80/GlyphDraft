package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OnnxNativeLibrariesTest {
    private val folder = Files.createTempDirectory("glyphdraft-native").resolve("native")

    @Test
    fun platformFolderNames() {
        val cases =
            listOf(
                Triple("Windows 10", "amd64", "win-x64"),
                Triple("Windows 11", "x86_64", "win-x64"),
                Triple("Linux", "amd64", "linux-x64"),
                Triple("Linux", "aarch64", "linux-aarch64"),
                Triple("Mac OS X", "aarch64", null),
                Triple("Linux", "riscv64", null),
            )

        for ((os, arch, expected) in cases) {
            assertEquals(expected, OnnxNativeLibraries.platformFolder(os, arch), "$os $arch")
        }
    }

    @Test
    fun unsupportedPlatformIsAFailureAndLeavesTheFolderUntouched() {
        assertIs<Outcome.Failure>(OnnxNativeLibraries.prepare(folder, osName = "Mac OS X", osArch = "aarch64"))

        assertFalse(Files.exists(folder))
    }

    @Test
    fun copiesTheLibrariesOnceAndPointsOnnxRuntimeAtThem() {
        val previous = System.getProperty(OnnxNativeLibraries.PATH_PROPERTY)
        try {
            val first = OnnxNativeLibraries.prepare(folder)

            assertIs<Outcome.Success<*>>(first, first.toString())
            val main = folder.resolve(System.mapLibraryName("onnxruntime"))
            assertTrue(Files.size(main) > 1_000_000)
            assertEquals(folder.toAbsolutePath().toString(), System.getProperty(OnnxNativeLibraries.PATH_PROPERTY))

            val modified = Files.getLastModifiedTime(main)
            assertIs<Outcome.Success<*>>(OnnxNativeLibraries.prepare(folder))
            assertEquals(modified, Files.getLastModifiedTime(main), "an up-to-date library is not copied again")
            assertFalse(Files.exists(folder.resolve("${main.fileName}.part")))
        } finally {
            if (previous == null) {
                System.clearProperty(OnnxNativeLibraries.PATH_PROPERTY)
            } else {
                System.setProperty(OnnxNativeLibraries.PATH_PROPERTY, previous)
            }
        }
    }
}

package com.glyphdraft.engine.models

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.installedPacks
import com.glyphdraft.core.usableLanguages
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileModelCatalogTest {
    private val folder = Files.createTempDirectory("glyphdraft-catalog")
    private val first = ModelFile("a/one.bin", "https://example.invalid/one", "0".repeat(64), 5)
    private val second = ModelFile("b/two.bin", "https://example.invalid/two", "1".repeat(64), 3)
    private val pack = PackManifest(ModelPack("p", "P", setOf(Language.English), 8), listOf(first, second))

    private fun write(file: ModelFile, size: Int) {
        val path = folder.resolve(file.path)
        Files.createDirectories(path.parent)
        Files.write(path, ByteArray(size))
    }

    @Test
    fun nothingIsInstalledInAnEmptyFolder() {
        val catalog = FileModelCatalog(listOf(pack), folder)

        assertTrue(catalog.installedPackIds.value.isEmpty())
        assertTrue(catalog.usableLanguages().isEmpty())
    }

    @Test
    fun aPackIsInstalledOnlyWhenEveryFileHasTheRightSize() {
        val cases =
            listOf(
                Triple(5, 3, true),
                Triple(5, 4, false),
                Triple(4, 3, false),
                Triple(5, -1, false),
                Triple(-1, 3, false),
            )

        for ((sizeOne, sizeTwo, expected) in cases) {
            folder.toFile().deleteRecursively()
            if (sizeOne >= 0) {
                write(first, sizeOne)
            }
            if (sizeTwo >= 0) {
                write(second, sizeTwo)
            }

            val catalog = FileModelCatalog(listOf(pack), folder)

            assertEquals(expected, catalog.installedPackIds.value.isNotEmpty(), "sizes $sizeOne and $sizeTwo")
        }
    }

    @Test
    fun refreshPicksUpFilesThatAppearedAfterTheStart() {
        val catalog = FileModelCatalog(listOf(pack), folder)
        write(first, 5)
        write(second, 3)

        catalog.refresh()

        assertEquals(listOf(pack.pack), catalog.installedPacks())
        assertEquals(setOf(Language.English), catalog.usableLanguages())
    }
}

package com.glyphdraft.engine.models

import com.glyphdraft.core.Language
import com.glyphdraft.core.knownLanguages
import com.glyphdraft.core.stub.StubModelCatalog
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelManifestTest {
    private val files = ModelManifest.packs.flatMap { it.files }

    @Test
    fun everyFileIsPinnedToAnHttpsUrlAndALowerCaseSha256() {
        for (file in files) {
            assertTrue(file.url.startsWith("https://"), file.url)
            assertTrue(Regex("[0-9a-f]{64}").matches(file.sha256), "${file.path}: ${file.sha256}")
            assertTrue(file.sizeBytes > 0, file.path)
            assertTrue(Regex("[0-9a-f]{40}").containsMatchIn(file.url), "no commit revision in ${file.url}")
        }
    }

    @Test
    fun aPathNamesOneFileEverywhere() {
        val byPath = files.groupBy { it.path }

        for ((path, sameName) in byPath) {
            assertEquals(1, sameName.toSet().size, "$path differs between packs")
            assertTrue(!path.startsWith("/") && ".." !in path, path)
        }
    }

    @Test
    fun packIdsAreUniqueAndSizesAreTheSumOfTheirFiles() {
        assertEquals(ModelManifest.packs.size, ModelManifest.packs.map { it.pack.id }.toSet().size)
        for (manifest in ModelManifest.packs) {
            assertEquals(manifest.files.sumOf { it.sizeBytes }, manifest.pack.sizeBytes, manifest.pack.id)
        }
    }

    @Test
    fun theCatalogOffersEveryLanguageOfThePickers() {
        val catalog = FileModelCatalog(ModelManifest.packs, Files.createTempDirectory("glyphdraft-catalog"))

        assertEquals(Language.Supported.toSet(), catalog.knownLanguages())
    }

    @Test
    fun theStubCatalogDescribesTheSamePacks() {
        val stub = StubModelCatalog().packs.associateBy { it.id }

        for (manifest in ModelManifest.packs) {
            assertEquals(stub[manifest.pack.id]?.languages, manifest.pack.languages, manifest.pack.id)
        }
    }
}

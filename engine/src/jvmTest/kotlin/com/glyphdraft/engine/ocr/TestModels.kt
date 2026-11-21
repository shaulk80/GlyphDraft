package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.HttpModelInstaller
import com.glyphdraft.engine.models.ModelManifest
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * The real PP-OCRv5 models for the opt-in tests (about 45 MB, so not in the repo): in `GLYPHDRAFT_MODELS_DIR`, or by
 * default in `engine/build/test-models`. With `GLYPHDRAFT_NETWORK_TESTS=1` the missing packs are downloaded there first
 * (checked by the pinned checksums); without models and without that variable a test does nothing and says so.
 */
internal object TestModels {
    private const val NETWORK_VARIABLE = "GLYPHDRAFT_NETWORK_TESTS"
    private const val FOLDER_VARIABLE = "GLYPHDRAFT_MODELS_DIR"

    val fixtures = File("src/test/resources/fixtures")

    /** A catalog over the models folder with every pack of [packs] installed, or null (after saying so) when it cannot be. */
    fun catalogWith(testName: String, packs: Set<String>): FileModelCatalog? {
        val catalog = FileModelCatalog(ModelManifest.packs, modelsFolder())
        val missing = packs.filter { it !in catalog.installedPackIds.value }
        if (missing.isNotEmpty() && System.getenv(NETWORK_VARIABLE).isNullOrBlank()) {
            println("$testName skipped: models are not installed (set $NETWORK_VARIABLE=1 to download them).")
            return null
        }

        val installer = HttpModelInstaller(catalog)
        for (id in missing) {
            val pack = ModelManifest.packs.first { it.pack.id == id }.pack
            val outcome = runBlocking { installer.install(pack) {} }
            assertTrue(outcome is Outcome.Success, "Installing $id failed: $outcome")
        }

        return catalog
    }

    fun decodeFixture(name: String): PixelImage = (AwtImageDecoder().decode(File(fixtures, "$name.png").readBytes()) as Outcome.Success).value

    fun expectedText(name: String): String = File(fixtures, "$name.txt").readText(Charsets.UTF_8)

    fun modelsFolder(): Path {
        val configured = System.getenv(FOLDER_VARIABLE)
        if (!configured.isNullOrBlank()) {
            return File(configured).toPath()
        }

        return File("build/test-models").toPath()
    }
}

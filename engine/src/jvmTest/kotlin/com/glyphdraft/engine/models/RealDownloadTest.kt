package com.glyphdraft.engine.models

import com.glyphdraft.core.Outcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

/**
 * Downloads two small real packs from the real servers, to catch a manifest URL that no longer works. It uses the
 * network, so it runs only when the environment variable [ENABLE_VARIABLE] is set (CI does not set it):
 * `$env:GLYPHDRAFT_NETWORK_TESTS = "1"; .\gradlew :engine:jvmTest --tests "*RealDownloadTest*"`.
 */
class RealDownloadTest {
    @Test
    fun smallPacksDownloadFromTheRealServersAndMatchTheirChecksums() {
        if (System.getenv(ENABLE_VARIABLE).isNullOrBlank()) {
            return
        }

        val folder = Files.createTempDirectory("glyphdraft-real-models")
        try {
            val catalog = FileModelCatalog(ModelManifest.packs, folder)
            val installer = HttpModelInstaller(catalog)

            // Hebrew comes from GitHub, Russian from Hugging Face (which redirects to its CDN).
            for (id in listOf(ModelManifest.HEBREW, ModelManifest.ESLAV)) {
                val pack = ModelManifest.packs.first { it.pack.id == id }.pack

                val outcome = runBlocking { installer.install(pack) {} }

                assertIs<Outcome.Success<*>>(outcome, "$id: $outcome")
            }

            assertEquals(setOf(ModelManifest.HEBREW, ModelManifest.ESLAV), catalog.installedPackIds.value)
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    private companion object {
        const val ENABLE_VARIABLE = "GLYPHDRAFT_NETWORK_TESTS"
    }
}

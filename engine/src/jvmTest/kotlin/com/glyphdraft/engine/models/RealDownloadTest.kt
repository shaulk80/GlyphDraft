package com.glyphdraft.engine.models

import com.glyphdraft.core.Outcome
import java.net.HttpURLConnection
import java.net.URI
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

    /** The translation models are 1.2 GB together, so each file is asked for its size (HEAD) instead of downloaded. */
    @Test
    fun everyTranslationFileExistsOnTheRealServerWithThePinnedSize() {
        if (System.getenv(ENABLE_VARIABLE).isNullOrBlank()) {
            return
        }

        val wrong = mutableListOf<String>()
        for (file in TranslationManifest.packs.flatMap { it.files }) {
            val size = sizeOnServer(file.url)
            if (size != file.sizeBytes) {
                wrong.add("${file.url}: server says $size, manifest ${file.sizeBytes}")
            }
        }

        assertEquals(emptyList(), wrong)
    }

    private fun sizeOnServer(url: String): Long {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return -connection.responseCode.toLong()
            }

            return connection.contentLengthLong
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENABLE_VARIABLE = "GLYPHDRAFT_NETWORK_TESTS"
    }
}

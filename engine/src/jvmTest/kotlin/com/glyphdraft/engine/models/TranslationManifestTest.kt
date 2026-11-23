package com.glyphdraft.engine.models

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.isInstalled
import com.glyphdraft.core.packsForRoute
import com.glyphdraft.core.translationRoutes
import com.glyphdraft.core.usableDirections
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class TranslationManifestTest {
    private val manifests = TranslationManifest.packs
    private val files = manifests.flatMap { it.files }
    private val hebrew = setOf(Language.Hebrew)

    private val folder: Path = Files.createTempDirectory("glyphdraft-translation")
    private val server = TestModelServer()

    @AfterTest
    fun tearDown() {
        server.close()
        folder.toFile().deleteRecursively()
    }

    @Test
    fun everyFileIsPinnedToAnHttpsUrlAtACommitWithALowerCaseSha256() {
        for (file in files) {
            assertTrue(file.url.startsWith("https://huggingface.co/"), file.url)
            assertTrue(Regex("/resolve/[0-9a-f]{40}/").containsMatchIn(file.url), "no commit revision in ${file.url}")
            assertTrue(Regex("[0-9a-f]{64}").matches(file.sha256), "${file.path}: ${file.sha256}")
            assertTrue(file.sizeBytes > 0, file.path)
        }
    }

    @Test
    fun filesLiveInOneFolderPerDirectionAndNothingIsSharedWithTheOcrPacks() {
        val ocrPaths = ModelManifest.packs.flatMap { it.files }.map { it.path }.toSet()

        for (manifest in manifests) {
            val direction = assertNotNull(manifest.pack.direction)
            val folderName = "mt/${direction.from.code}-${direction.to.code}/"
            assertTrue(manifest.files.all { it.path.startsWith(folderName) && "/" !in it.path.removePrefix(folderName) }, manifest.pack.id)
            assertEquals(manifest.files.size, manifest.files.map { it.path }.toSet().size, manifest.pack.id)
        }

        assertEquals(files.size, files.map { it.path }.toSet().size)
        assertTrue(files.none { it.path in ocrPaths })
    }

    @Test
    fun packsAreDirectionsThatReadNoLanguageWithUniqueIdsAndSummedSizes() {
        val ocrIds = ModelManifest.packs.map { it.pack.id }.toSet()

        assertEquals(manifests.size, manifests.map { it.pack.id }.toSet().size)
        assertEquals(manifests.size, manifests.map { it.pack.direction }.toSet().size)
        for (manifest in manifests) {
            assertTrue(manifest.pack.id !in ocrIds, manifest.pack.id)
            assertEquals(emptySet(), manifest.pack.languages, manifest.pack.id)
            assertEquals(manifest.files.sumOf { it.sizeBytes }, manifest.pack.sizeBytes, manifest.pack.id)
            assertEquals(manifest.pack.id, TranslationManifest.packId(assertNotNull(manifest.pack.direction)))
        }
    }

    @Test
    fun aDirectionWithoutCacheNamesAPlainDecoderAndOneWithCacheTheMergedOne() {
        for (manifest in manifests) {
            val direction = assertNotNull(manifest.pack.direction)
            val model = assertNotNull(TranslationManifest.filesOf(direction))
            val decoderName = model.decoder.path.substringAfterLast('/')

            assertEquals(if (model.decoderHasCache) "decoder_model_merged_quantized.onnx" else "decoder_model_quantized.onnx", decoderName)
            assertEquals("encoder_model_quantized.onnx", model.encoder.path.substringAfterLast('/'))
            assertEquals(model.all, manifest.files)
        }
    }

    @Test
    fun theDirectionsAreTheShippingSetOfDecision0012() {
        val directions = manifests.mapNotNull { it.pack.direction }.toSet()
        val en = Language.English

        for (other in listOf(Language.German, Language.French, Language.Spanish, Language.Russian, Language.Chinese)) {
            assertTrue(TranslationDirection(en, other) in directions, "en to ${other.code}")
            assertTrue(TranslationDirection(other, en) in directions, "${other.code} to en")
        }
        assertTrue(TranslationDirection(Language.Japanese, en) in directions)
        assertTrue(TranslationDirection(Language.Hebrew, en) in directions)
        assertTrue(TranslationDirection(Language.Russian, Language.Hebrew) in directions)
        assertEquals(13, directions.size)
        assertTrue(directions.none { it.to == Language.Japanese }, "Japanese is never a target")
        assertTrue(directions.none { it.to == Language.Hebrew && it.from != Language.Russian }, "Hebrew is reached through Russian only")
    }

    @Test
    fun onlyChineseTargetAndHebrewDirectionsAreExperimental() {
        for (manifest in manifests) {
            val direction = assertNotNull(manifest.pack.direction)
            val expected = direction == TranslationDirection(Language.English, Language.Chinese) || direction.from in hebrew || direction.to in hebrew

            assertEquals(expected, manifest.pack.experimental, manifest.pack.id)
        }
    }

    @Test
    fun aDownloadStaysUnderTheSizeCeilingExceptForHebrew() {
        val ceilingBytes = 125_000_000L

        for (manifest in manifests) {
            val direction = assertNotNull(manifest.pack.direction)
            if (direction.from in hebrew || direction.to in hebrew) {
                continue
            }

            assertTrue(manifest.pack.sizeBytes <= ceilingBytes, "${manifest.pack.id}: ${manifest.pack.sizeBytes}")
        }
    }

    @Test
    fun everyPairOfThePickersHasARouteExceptWithJapaneseAsTarget() {
        val catalog = FileModelCatalog(manifests, folder)
        val routes = catalog.translationRoutes()
        val all = Language.Supported

        assertEquals(all.toSet(), routes.sources)
        assertEquals(all.filter { it != Language.Japanese }.toSet(), routes.targets)
        for (from in all) {
            for (to in all) {
                val expected = to != Language.Japanese || from == to
                assertEquals(expected, routes.route(from, to) != null, "${from.code} to ${to.code}")
            }
        }
        assertEquals(listOf("mt-de-en", "mt-en-ru", "mt-ru-he"), catalog.packsForRoute(Language.German, Language.Hebrew)?.map { it.id })
        assertNull(catalog.packsForRoute(Language.English, Language.Japanese))
    }

    @Test
    fun aTranslationPackInstallsIntoItsFolderAndMakesItsDirectionUsable() = runBlocking {
        val direction = TranslationDirection(Language.German, Language.English)
        val served =
            listOf("config.json", "vocab.json", "tokenizer.json", "encoder_model_quantized.onnx", "decoder_model_merged_quantized.onnx")
                .mapIndexed { index, name -> server.fileFor("/de-en/$name", ByteArray(1_000 + index) { (it % 199).toByte() }, "mt/de-en/$name") }
        val pack = ModelPack("mt-de-en", "German to English", emptySet(), served.sumOf { it.sizeBytes }, direction)
        val catalog = FileModelCatalog(listOf(PackManifest(pack, served)), folder)
        val installer = HttpModelInstaller(catalog, FileDownloader(connectTimeoutMillis = 2_000, readTimeoutMillis = 5_000))
        assertEquals(emptySet(), catalog.usableDirections())

        val outcome = installer.install(pack) {}

        assertIs<Outcome.Success<*>>(outcome)
        assertEquals(setOf(direction), catalog.usableDirections())
        assertTrue(catalog.isInstalled(pack))
        assertEquals(1_000L, Files.size(folder.resolve("mt/de-en/config.json")))
        assertEquals(listOf(pack), catalog.packsForRoute(Language.German, Language.English)?.filter { catalog.isInstalled(it) })
    }
}

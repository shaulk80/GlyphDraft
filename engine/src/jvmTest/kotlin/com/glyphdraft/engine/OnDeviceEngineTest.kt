package com.glyphdraft.engine

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Translation
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.engine.models.ModelManifest
import com.glyphdraft.engine.models.TranslationManifest
import com.glyphdraft.engine.ocr.AwtImageDecoder
import com.glyphdraft.engine.ocr.TestModels
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/** The wiring the apps use: [OnDeviceEngine] over a models folder. */
class OnDeviceEngineTest {
    private val page = ImageInput("en-clean.png", File(TestModels.fixtures, "en-clean.png").readBytes())

    @Test
    fun anEmptyModelsFolderOffersEveryReadingAndTranslationPackAndInstallsNone() {
        val folder = Files.createTempDirectory("glyphdraft-engine")
        try {
            OnDeviceEngine(folder, AwtImageDecoder()).use { engine ->
                assertEquals((ModelManifest.packs + TranslationManifest.packs).map { it.pack }, engine.catalog.packs)
                assertTrue(engine.catalog.installedPackIds.value.isEmpty())
            }
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun recognizingWithoutModelsIsAFailureWithAMessage() {
        val folder = Files.createTempDirectory("glyphdraft-engine")
        try {
            OnDeviceEngine(folder, AwtImageDecoder()).use { engine ->
                for (hint in listOf(null, Language.English)) {
                    val outcome = runBlocking { engine.engine.recognize(page, hint) }

                    val failure = assertIs<Outcome.Failure>(outcome)
                    assertFalse(failure.message.isBlank())
                }
            }
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun translatingWithoutTheModelIsAFailureThatNamesTheMissingModel() {
        val folder = Files.createTempDirectory("glyphdraft-engine")
        try {
            OnDeviceEngine(folder, AwtImageDecoder()).use { engine ->
                val outcome = runBlocking { engine.translator.translate("Hello", Language.English, Language.German) }

                val failure = assertIs<Outcome.Failure>(outcome)
                assertTrue("English to German" in failure.message, failure.message)
            }
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    /** Opt-in, like the fixture tests: the Latin pack from the shared test models folder, read through the factory. */
    @Test
    fun readsAnEnglishPageWithTheRealModels() {
        TestModels.catalogWith("OnDeviceEngineTest", setOf(ModelManifest.LATIN)) ?: return

        OnDeviceEngine(TestModels.modelsFolder(), AwtImageDecoder()).use { engine ->
            val outcome = runBlocking { engine.engine.recognize(page, null) }

            val result = assertIs<Outcome.Success<RecognitionResult>>(outcome).value
            assertEquals(Language.English, result.language)
            assertTrue(result.detected)
            assertFalse(result.text.isBlank())
        }
    }

    /** Opt-in: one translation model (English to German) from the shared test models folder, run through the factory. */
    @Test
    fun translatesWithTheRealModelThroughTheFactory() {
        TestModels.catalogWith("OnDeviceEngineTest", setOf(TranslationManifest.packId(TranslationDirection(Language.English, Language.German)))) ?: return

        OnDeviceEngine(TestModels.modelsFolder(), AwtImageDecoder()).use { engine ->
            val outcome = runBlocking { engine.translator.translate("The house is old.", Language.English, Language.German) }

            val translation = assertIs<Outcome.Success<Translation>>(outcome).value
            assertEquals(listOf(Language.English, Language.German), translation.route)
            assertTrue("Haus" in translation.text, translation.text)
        }
    }
}

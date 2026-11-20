package com.glyphdraft.engine.ocr

import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.flatMap
import com.glyphdraft.core.map
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.ModelFile
import com.glyphdraft.engine.models.ModelManifest
import java.io.IOException
import java.nio.file.Files

/** Opens the installed PP-OCRv5 models of a [FileModelCatalog]. A pack that is not installed is a [Outcome.Failure]. */
class PaddleModels(private val catalog: FileModelCatalog) : PaddleModelSource {
    /** The text detector; every recognizer pack includes it. */
    override fun openDetector(): Outcome<TextDetector> {
        val missing = missingFile(ModelManifest.detectorModel, ModelManifest.detectorConfig)
        if (missing != null) {
            return missing
        }

        return OrtSessions.open(catalog.pathOf(ModelManifest.detectorModel)).map { TextDetector(it) }
    }

    /** The recognizer of the pack [packId] (`latin`, `eslav` or `cjk`). */
    override fun openRecognizer(packId: String): Outcome<TextRecognizer> {
        val files = RECOGNIZER_FILES[packId]
        if (files == null) {
            return Outcome.Failure("The pack '$packId' has no PP-OCRv5 recognizer.")
        }

        val (model, config) = files
        val missing = missingFile(model, config)
        if (missing != null) {
            return missing
        }

        return readDictionary(config).flatMap { dictionary ->
            OrtSessions.open(catalog.pathOf(model)).flatMap { session -> recognizerOf(session, dictionary, packId) }
        }
    }

    /** A dictionary that does not fit the model would decode to nonsense, so it is refused at load. */
    private fun recognizerOf(session: OrtSession, dictionary: CharacterDictionary, packId: String): Outcome<TextRecognizer> {
        val shape = (session.outputInfo.values.first().info as? TensorInfo)?.shape
        val classes = shape?.lastOrNull() ?: UNKNOWN_CLASSES
        if (classes > 0 && classes != dictionary.classCount.toLong()) {
            session.close()
            return Outcome.Failure("The model and the dictionary of the pack '$packId' do not match ($classes classes against ${dictionary.classCount}).")
        }

        return Outcome.Success(TextRecognizer(session, dictionary))
    }

    private fun missingFile(vararg files: ModelFile): Outcome.Failure? {
        val missing = files.firstOrNull { !catalog.hasFile(it) }
        if (missing == null) {
            return null
        }

        return Outcome.Failure("The model file ${missing.path} is not installed.")
    }

    private fun readDictionary(config: ModelFile): Outcome<CharacterDictionary> =
        try {
            CharacterDictionary.parse(String(Files.readAllBytes(catalog.pathOf(config)), Charsets.UTF_8))
        } catch (e: IOException) {
            Outcome.Failure("The file ${config.path} could not be read: ${e.message}", e)
        }

    private companion object {
        const val UNKNOWN_CLASSES = -1L

        val RECOGNIZER_FILES =
            mapOf(
                ModelManifest.LATIN to (ModelManifest.latinModel to ModelManifest.latinConfig),
                ModelManifest.ESLAV to (ModelManifest.eslavModel to ModelManifest.eslavConfig),
                ModelManifest.CJK to (ModelManifest.cjkModel to ModelManifest.cjkConfig),
            )
    }
}

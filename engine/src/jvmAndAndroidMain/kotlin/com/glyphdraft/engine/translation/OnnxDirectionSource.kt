package com.glyphdraft.engine.translation

import ai.onnxruntime.OrtSession
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.flatMap
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.ModelFile
import com.glyphdraft.engine.models.TranslationManifest
import com.glyphdraft.engine.models.TranslationModelFiles
import com.glyphdraft.engine.ocr.OrtSessions
import java.io.IOException
import java.nio.file.Files

/**
 * Opens the installed files of a direction of the [TranslationManifest] as a [DirectionTranslator]: reads the config and
 * the vocabulary, checks that the files belong together, then loads the encoder and decoder into ONNX Runtime.
 */
class OnnxDirectionSource(private val catalog: FileModelCatalog) : DirectionSource {
    override fun open(direction: TranslationDirection): Outcome<DirectionTranslator> {
        val files = TranslationManifest.filesOf(direction)
        if (files == null) {
            return Outcome.Failure("There is no model for ${label(direction)}.")
        }

        val missing = files.all.firstOrNull { !catalog.hasFile(it) }
        if (missing != null) {
            return Outcome.Failure("The model for ${label(direction)} is not installed (${missing.path} is missing).")
        }

        return readTexts(files).flatMap { texts ->
            MarianConfigReader.read(texts.config).flatMap { config ->
                MarianVocabularyReader.read(texts.vocabulary, texts.tokenizer).flatMap { vocabulary ->
                    val tokenizer = UnigramTokenizer(vocabulary)
                    checkIds(config, tokenizer, direction).flatMap { openModel(files, config).asTranslator(tokenizer, config) }
                }
            }
        }
    }

    private fun readTexts(files: TranslationModelFiles): Outcome<ModelTexts> =
        try {
            Outcome.Success(ModelTexts(read(files.config), read(files.vocabulary), read(files.tokenizer)))
        } catch (e: IOException) {
            Outcome.Failure("The model files of ${label(files.direction)} could not be read: ${e.message}", e)
        }

    private fun read(file: ModelFile): String = String(Files.readAllBytes(catalog.pathOf(file)), Charsets.UTF_8)

    /** A vocabulary whose end or padding id differs from the config would decode to nonsense, so it is refused at load. */
    private fun checkIds(config: MarianConfig, tokenizer: SubwordTokenizer, direction: TranslationDirection): Outcome<Unit> {
        if (config.endId != tokenizer.endOfSequenceId || config.padId != tokenizer.padId || config.startId != tokenizer.padId) {
            return Outcome.Failure("The vocabulary and the config of ${label(direction)} do not match (end, pad or start id differ).")
        }

        return Outcome.Success(Unit)
    }

    private fun openModel(files: TranslationModelFiles, config: MarianConfig): Outcome<SentenceModel> =
        OrtSessions.open(catalog.pathOf(files.encoder)).flatMap { encoder ->
            when (val decoder = OrtSessions.open(catalog.pathOf(files.decoder))) {
                is Outcome.Failure -> {
                    encoder.close()
                    decoder
                }

                is Outcome.Success -> Outcome.Success(OnnxSentenceModel(encoder, decoder.value, greedyOf(decoder.value, config, files)))
            }
        }

    private fun greedyOf(decoder: OrtSession, config: MarianConfig, files: TranslationModelFiles): GreedyDecoder =
        if (files.decoderHasCache) CachedGreedyDecoder(decoder, config) else PlainGreedyDecoder(decoder, config)

    private fun Outcome<SentenceModel>.asTranslator(tokenizer: SubwordTokenizer, config: MarianConfig): Outcome<DirectionTranslator> =
        when (this) {
            is Outcome.Failure -> this
            is Outcome.Success -> Outcome.Success(DirectionTranslator(tokenizer, config, value))
        }

    private fun label(direction: TranslationDirection): String = "${direction.from.displayName} to ${direction.to.displayName}"

    private class ModelTexts(val config: String, val vocabulary: String, val tokenizer: String)
}

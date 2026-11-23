package com.glyphdraft.engine

import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.core.Translator
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.HttpModelInstaller
import com.glyphdraft.engine.models.ModelManifest
import com.glyphdraft.engine.models.TranslationManifest
import com.glyphdraft.engine.ocr.ImageDecoder
import com.glyphdraft.engine.ocr.OnDeviceOcrEngine
import com.glyphdraft.engine.ocr.PaddleModels
import com.glyphdraft.engine.ocr.PaddleReader
import com.glyphdraft.engine.ocr.TesseractHebrewReader
import com.glyphdraft.engine.translation.OnDeviceTranslator
import com.glyphdraft.engine.translation.OnnxDirectionSource
import java.nio.file.Path

/**
 * The real engine of the apps, put together once so the two composition roots do not repeat the wiring: the model
 * catalog over `<data>/models` (reading packs first, then one pack per translation direction), the installer that
 * downloads into it, the [OcrEngine] that reads with what is installed and the [Translator]. The models are opened on
 * first use, so building this is cheap.
 *
 * [close] releases the loaded models; call it when no read or translation is running. The apps keep one instance for
 * the life of the process.
 */
class OnDeviceEngine(
    modelsDirectory: Path,
    decoder: ImageDecoder,
) : AutoCloseable {
    private val fileCatalog = FileModelCatalog(ModelManifest.packs + TranslationManifest.packs, modelsDirectory)
    private val ocrEngine =
        OnDeviceOcrEngine(
            decoder = decoder,
            catalog = fileCatalog,
            paddle = PaddleReader(PaddleModels(fileCatalog)),
            languageDetector = TextLanguageDetector(),
            hebrew = TesseractHebrewReader(fileCatalog),
        )

    private val onDeviceTranslator = OnDeviceTranslator(fileCatalog, OnnxDirectionSource(fileCatalog))

    val catalog: ModelCatalog = fileCatalog
    val installer: ModelInstaller = HttpModelInstaller(fileCatalog)
    val engine: OcrEngine = ocrEngine
    val translator: Translator = onDeviceTranslator

    override fun close() {
        ocrEngine.close()
        onDeviceTranslator.close()
    }
}

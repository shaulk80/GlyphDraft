package com.glyphdraft.engine

import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.OcrEngine
import com.glyphdraft.engine.language.TextLanguageDetector
import com.glyphdraft.engine.models.FileModelCatalog
import com.glyphdraft.engine.models.HttpModelInstaller
import com.glyphdraft.engine.models.ModelManifest
import com.glyphdraft.engine.ocr.ImageDecoder
import com.glyphdraft.engine.ocr.OnDeviceOcrEngine
import com.glyphdraft.engine.ocr.PaddleModels
import com.glyphdraft.engine.ocr.PaddleReader
import com.glyphdraft.engine.ocr.TesseractHebrewReader
import java.nio.file.Path

/**
 * The real engine of the apps, put together once so the two composition roots do not repeat the wiring: the model
 * catalog over `<data>/models`, the installer that downloads into it, and the [OcrEngine] that reads with what is
 * installed. The models are opened on first use, so building this is cheap.
 *
 * [close] releases the loaded models; the apps keep one instance for the life of the process.
 */
class OnDeviceEngine(
    modelsDirectory: Path,
    decoder: ImageDecoder,
) : AutoCloseable {
    private val fileCatalog = FileModelCatalog(ModelManifest.packs, modelsDirectory)
    private val ocrEngine =
        OnDeviceOcrEngine(
            decoder = decoder,
            catalog = fileCatalog,
            paddle = PaddleReader(PaddleModels(fileCatalog)),
            languageDetector = TextLanguageDetector(),
            hebrew = TesseractHebrewReader(fileCatalog),
        )

    val catalog: ModelCatalog = fileCatalog
    val installer: ModelInstaller = HttpModelInstaller(fileCatalog)
    val engine: OcrEngine = ocrEngine

    override fun close() {
        ocrEngine.close()
    }
}

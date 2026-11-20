package com.glyphdraft.engine.ocr

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtLoggingLevel
import ai.onnxruntime.OrtSession
import com.glyphdraft.core.Outcome
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

/** Opens ONNX Runtime sessions with the settings every model of the app uses. */
internal object OrtSessions {
    private const val MAX_THREADS = 4

    fun open(model: Path): Outcome<OrtSession> =
        try {
            val options = OrtSession.SessionOptions()
            // The PP-OCRv5 models carry unused initializers; ONNX Runtime warns about each one at load.
            options.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_ERROR)
            options.setIntraOpNumThreads(max(1, min(MAX_THREADS, Runtime.getRuntime().availableProcessors())))
            Outcome.Success(OrtEnvironment.getEnvironment().createSession(model.toString(), options))
        } catch (e: OrtException) {
            Outcome.Failure("The model ${model.fileName} could not be loaded: ${e.message}", e)
        } catch (e: UnsatisfiedLinkError) {
            Outcome.Failure("ONNX Runtime native library could not be loaded: ${e.message}", e)
        }
}

package com.glyphdraft.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads ONNX Runtime, runs the tiny built-in model once and checks the answer. Succeeds only when the native
 * library could be extracted and loaded, which is the part that differs between Windows, Linux and Android.
 */
object OnnxSelfTest {
    /** The message of the success outcome; the smoke tests look for it in the log. */
    const val OK_MESSAGE = "engine self-test: ok"

    suspend fun run(): Outcome<String> = withContext(Dispatchers.IO) { runBlockingCall() }

    private fun runBlockingCall(): Outcome<String> {
        val x = floatArrayOf(1f, 2f, 3f, 4f)
        val y = floatArrayOf(10f, 20f, 30f, 40f)
        val expected = floatArrayOf(11f, 22f, 33f, 44f)

        return try {
            val environment = OrtEnvironment.getEnvironment()
            environment.createSession(TinyModel.bytes()).use { session ->
                OnnxTensor.createTensor(environment, arrayOf(x)).use { xTensor ->
                    OnnxTensor.createTensor(environment, arrayOf(y)).use { yTensor ->
                        session.run(mapOf(TinyModel.INPUT_X to xTensor, TinyModel.INPUT_Y to yTensor)).use { result ->
                            val actual = (result[TinyModel.OUTPUT].get().value as Array<*>)[0] as FloatArray
                            verify(actual, expected)
                        }
                    }
                }
            }
        } catch (e: OrtException) {
            Outcome.Failure("ONNX Runtime failed: ${e.message}", e)
        } catch (e: UnsatisfiedLinkError) {
            Outcome.Failure("ONNX Runtime native library could not be loaded: ${e.message}", e)
        }
    }

    private fun verify(actual: FloatArray, expected: FloatArray): Outcome<String> {
        if (actual.contentEquals(expected)) {
            return Outcome.Success(OK_MESSAGE)
        }

        return Outcome.Failure("ONNX Runtime returned ${actual.toList()} instead of ${expected.toList()}.")
    }
}

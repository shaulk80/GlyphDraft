package spike

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.google.gson.JsonParser
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** One translation direction: OPUS-MT (Marian) exported with Optimum, int8, run through ONNX Runtime; greedy decoding with a key/value cache. */
class MarianTranslator(val dir: File, threads: Int = 4) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(threads)
        setSessionLogLevel(ai.onnxruntime.OrtLoggingLevel.ORT_LOGGING_LEVEL_ERROR)
    }
    private val encoder = env.createSession(File(dir, "onnx/encoder_model_quantized.onnx").path, options)
    private val mergedFile = File(dir, "onnx/decoder_model_merged_quantized.onnx")
    // Community exports (Hebrew) have a plain decoder without a cache: every step re-reads the whole prefix.
    private val withoutCache = !mergedFile.exists()
    private val decoder = env.createSession(
        (if (withoutCache) File(dir, "onnx/decoder_model_quantized.onnx") else mergedFile).path, options)
    val tokenizer = MarianTokenizer(dir)
    private val config = JsonParser.parseString(File(dir, "config.json").readText()).asJsonObject
    private val startId = config["decoder_start_token_id"].asInt
    private val layers = config["decoder_layers"].asInt
    private val heads = config["decoder_attention_heads"].asInt
    private val headDim = config["d_model"].asInt / heads
    private val maxLength = config["max_length"]?.takeIf { !it.isJsonNull }?.asInt ?: 512

    var generatedTokens = 0L
        private set

    private fun translateWithoutCache(ids: IntArray): String {
        val n = ids.size
        val input = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(n) { ids[it].toLong() }), longArrayOf(1, n.toLong()))
        val mask = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(n) { 1L }), longArrayOf(1, n.toLong()))
        val encoded = encoder.run(mapOf("input_ids" to input, "attention_mask" to mask))
        val hidden = encoded.get("last_hidden_state").get() as OnnxTensor
        val prefix = ArrayList<Long>()
        prefix.add(startId.toLong())
        try {
            for (step in 0 until minOf(maxLength, n * 2 + 10)) {
                val stepIds = OnnxTensor.createTensor(env, LongBuffer.wrap(prefix.toLongArray()), longArrayOf(1, prefix.size.toLong()))
                val result = if ("decoder_input_ids" in decoder.inputNames) {
                    decoder.run(mapOf("decoder_input_ids" to stepIds, "encoder_hidden_states" to hidden, "decoder_attention_mask" to mask))
                } else {
                    decoder.run(mapOf("input_ids" to stepIds, "encoder_hidden_states" to hidden, "encoder_attention_mask" to mask))
                }
                val logits = (result.get("logits").get() as OnnxTensor).floatBuffer
                val vocab = tokenizer.padId + 1
                val base = logits.limit() - vocab
                var best = 0
                var bestValue = Float.NEGATIVE_INFINITY
                for (i in 0 until vocab) {
                    if (i != tokenizer.padId && logits.get(base + i) > bestValue) {
                        bestValue = logits.get(base + i)
                        best = i
                    }
                }
                result.close()
                stepIds.close()
                generatedTokens++
                if (best == tokenizer.eosId) {
                    break
                }
                prefix.add(best.toLong())
            }
        } finally {
            encoded.close()
            input.close()
            mask.close()
        }
        return tokenizer.decode(prefix.drop(1).map { it.toInt() })
    }

    fun translate(text: String): String {
        val ids = tokenizer.encode(text)
        if (withoutCache) {
            return translateWithoutCache(ids)
        }
        val n = ids.size
        val input = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(n) { ids[it].toLong() }), longArrayOf(1, n.toLong()))
        val mask = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(n) { 1L }), longArrayOf(1, n.toLong()))
        val encoded = encoder.run(mapOf("input_ids" to input, "attention_mask" to mask))
        val hidden = encoded.get("last_hidden_state").get() as OnnxTensor
        val limit = minOf(maxLength, n * 2 + 10)
        val output = ArrayList<Int>()
        var first: OrtSession.Result? = null
        var previous: OrtSession.Result? = null
        var pastDecoder: Map<String, OnnxTensor> = emptyMap()
        var pastEncoder: Map<String, OnnxTensor> = emptyMap()
        val empties = ArrayList<OnnxTensor>()
        var next = startId
        try {
            for (step in 0 until limit) {
                val inputs = HashMap<String, OnnxTensor>()
                val stepIds = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(next.toLong())), longArrayOf(1, 1))
                val useCache = OnnxTensor.createTensor(env, booleanArrayOf(step > 0))
                inputs["input_ids"] = stepIds
                inputs["encoder_hidden_states"] = hidden
                inputs["encoder_attention_mask"] = mask
                inputs["use_cache_branch"] = useCache
                for (layer in 0 until layers) {
                    for (kind in listOf("decoder", "encoder")) {
                        for (kv in listOf("key", "value")) {
                            val name = "past_key_values.$layer.$kind.$kv"
                            val tensor = if (step == 0) {
                                val empty = emptyPast(kind, n)
                                empties.add(empty)
                                empty
                            } else if (kind == "decoder") {
                                pastDecoder.getValue(name)
                            } else {
                                pastEncoder.getValue(name)
                            }
                            inputs[name] = tensor
                        }
                    }
                }
                val result = decoder.run(inputs)
                stepIds.close()
                useCache.close()
                val logits = (result.get("logits").get() as OnnxTensor).floatBuffer
                next = argmax(logits)
                if (step == 0) {
                    first = result
                    pastEncoder = collect(result, "encoder")
                } else {
                    previous?.close()
                }
                previous = if (step == 0) null else result
                pastDecoder = collect(result, "decoder")
                generatedTokens++
                if (next == tokenizer.eosId) {
                    break
                }
                output.add(next)
            }
        } finally {
            previous?.close()
            first?.close()
            empties.forEach { it.close() }
            encoded.close()
            input.close()
            mask.close()
        }
        return tokenizer.decode(output)
    }

    private fun emptyPast(kind: String, sourceLength: Int): OnnxTensor {
        // At step 0 the cache branch is off; the inputs only need the right rank.
        return OnnxTensor.createTensor(env, FloatBuffer.allocate(0), longArrayOf(1, heads.toLong(), 0, headDim.toLong()))
    }

    private fun collect(result: OrtSession.Result, kind: String): Map<String, OnnxTensor> {
        val map = HashMap<String, OnnxTensor>()
        for (layer in 0 until layers) {
            for (kv in listOf("key", "value")) {
                val tensor = result.get("present.$layer.$kind.$kv").get() as OnnxTensor
                map["past_key_values.$layer.$kind.$kv"] = tensor
            }
        }
        return map
    }

    private fun argmax(logits: FloatBuffer): Int {
        val size = logits.limit()
        val base = 0
        var best = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (i in 0 until size) {
            if (i == tokenizer.padId) {
                continue
            }
            val v = logits.get(base + i)
            if (v > bestValue) {
                bestValue = v
                best = i
            }
        }
        return best
    }

    fun inspect(): String {
        val sb = StringBuilder()
        sb.appendLine("encoder in: ${encoder.inputInfo.keys} out: ${encoder.outputInfo.keys}")
        sb.appendLine("decoder in: ${decoder.inputInfo.keys.sorted().take(12)} ... out: ${decoder.outputInfo.keys.sorted().take(6)} ...")
        sb.appendLine("decoder logits: ${decoder.outputInfo["logits"]}")
        return sb.toString()
    }

    override fun close() {
        encoder.close()
        decoder.close()
        options.close()
    }
}

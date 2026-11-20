package spike

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.yaml.snakeyaml.Yaml
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** One recognised text line with its box (original image pixels) and mean character confidence. */
class OcrLine(val text: String, val confidence: Float, val x: Int, val y: Int, val w: Int, val h: Int)

/** Oriented text box: centre, length along the text, thickness across it, angle in radians. */
class TextBox(val cx: Double, val cy: Double, val length: Double, val thickness: Double, val angle: Double)

class OcrOutput(val lines: List<OcrLine>) {
    val text: String get() = lines.joinToString("\n") { it.text }
    val confidence: Float get() = if (lines.isEmpty()) 0f else lines.map { it.confidence }.average().toFloat()
}

/** Text-box detector (PP-OCRv5 det, DB post-processing simplified to axis-aligned boxes). Spike quality, not product code. */
class PaddleDetector(private val env: OrtEnvironment, modelDir: File, options: OrtSession.SessionOptions) {
    private val session = env.createSession(File(modelDir, "inference.onnx").path, options)
    private val inputName = session.inputNames.first()

    fun detect(image: BufferedImage): List<TextBox> {
        val scale = min(1.0, 960.0 / max(image.width, image.height))
        val w = max(32, ((image.width * scale) / 32).roundToInt() * 32)
        val h = max(32, ((image.height * scale) / 32).roundToInt() * 32)
        val resized = resize(image, w, h)
        val input = FloatBuffer.allocate(3 * w * h)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        for (c in 0 until 3) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val rgb = resized.getRGB(x, y)
                    // The reference pipeline decodes to BGR and applies the mean in that channel order.
                    val value = when (c) {
                        0 -> rgb and 0xFF
                        1 -> (rgb shr 8) and 0xFF
                        else -> (rgb shr 16) and 0xFF
                    }
                    input.put((value / 255f - mean[c]) / std[c])
                }
            }
        }
        input.rewind()
        OnnxTensor.createTensor(env, input, longArrayOf(1, 3, h.toLong(), w.toLong())).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                val flat = flatten(result[0].value)
                val sx = w.toDouble() / image.width
                val sy = h.toDouble() / image.height
                return boxes(flat, w, h).map { b ->
                    TextBox(b.cx / sx, b.cy / sy, b.length / sx, b.thickness / sx, b.angle)
                }
            }
        }
    }

    /** Threshold, connected components, score filter, oriented box by PCA, unclip. Resized-frame coordinates. */
    private fun boxes(prob: FloatArray, w: Int, h: Int): List<TextBox> {
        val threshold = 0.3f
        val boxThreshold = 0.6
        val unclipRatio = 1.5
        val seen = BooleanArray(w * h)
        val result = ArrayList<TextBox>()
        val queue = IntArray(w * h)
        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)
        for (start in 0 until w * h) {
            if (seen[start] || prob[start] <= threshold) {
                continue
            }
            var head = 0
            var tail = 0
            queue[tail++] = start
            seen[start] = true
            var sum = 0.0
            while (head < tail) {
                val p = queue[head++]
                sum += prob[p]
                for (d in 0 until 4) {
                    val nx = p % w + dx[d]
                    val ny = p / w + dy[d]
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                        continue
                    }
                    val n = ny * w + nx
                    if (!seen[n] && prob[n] > threshold) {
                        seen[n] = true
                        queue[tail++] = n
                    }
                }
            }
            val box = orientedBox(queue, tail, w) ?: continue
            if (min(box.length, box.thickness) < 3 || sum / tail < boxThreshold) {
                continue
            }
            val distance = box.length * box.thickness * unclipRatio / (2.0 * (box.length + box.thickness))
            result.add(TextBox(box.cx, box.cy, box.length + 2 * distance, box.thickness + 2 * distance, box.angle))
        }
        return result
    }

    /** Principal-axis rectangle of the pixels queue[0 until count]; null for degenerate components. */
    private fun orientedBox(queue: IntArray, count: Int, w: Int): TextBox? {
        var mx = 0.0
        var my = 0.0
        for (i in 0 until count) {
            mx += queue[i] % w
            my += queue[i] / w
        }
        mx /= count
        my /= count
        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (i in 0 until count) {
            val x = queue[i] % w - mx
            val y = queue[i] / w - my
            sxx += x * x
            syy += y * y
            sxy += x * y
        }
        val angle = 0.5 * atan2(2 * sxy, sxx - syy)
        val c = cos(angle)
        val s = sin(angle)
        var u0 = Double.MAX_VALUE
        var u1 = -Double.MAX_VALUE
        var v0 = Double.MAX_VALUE
        var v1 = -Double.MAX_VALUE
        for (i in 0 until count) {
            val x = queue[i] % w - mx
            val y = queue[i] / w - my
            val u = x * c + y * s
            val v = -x * s + y * c
            u0 = min(u0, u); u1 = max(u1, u); v0 = min(v0, v); v1 = max(v1, v)
        }
        val length = u1 - u0 + 1
        val thickness = v1 - v0 + 1
        val cu = (u0 + u1) / 2
        val cv = (v0 + v1) / 2
        return TextBox(mx + cu * c - cv * s, my + cu * s + cv * c, length, thickness, angle)
    }
}

/** CTC text recogniser for one PP-OCRv5 recognition model. */
class PaddleRecognizer(private val env: OrtEnvironment, modelDir: File, options: OrtSession.SessionOptions) {
    val name: String = modelDir.name
    private val session = env.createSession(File(modelDir, "inference.onnx").path, options)
    private val inputName = session.inputNames.first()
    private val dictionary: List<String>

    init {
        val yaml = Yaml().load<Map<String, Any>>(File(modelDir, "inference.yml").readText(Charsets.UTF_8))
        @Suppress("UNCHECKED_CAST")
        val post = yaml["PostProcess"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val chars = (post["character_dict"] as List<Any>).map { it.toString() }
        // Index 0 is the CTC blank; the space is appended after the dictionary.
        dictionary = listOf("") + chars + " "
    }

    /** Returns text and mean confidence of the kept characters. */
    fun recognize(line: BufferedImage): Pair<String, Float> {
        val h = 48
        val w = max(16, min(3200, (line.width * h / line.height.toDouble()).roundToInt()))
        val resized = resize(line, w, h)
        val input = FloatBuffer.allocate(3 * h * w)
        for (c in 0 until 3) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val rgb = resized.getRGB(x, y)
                    val value = when (c) {
                        0 -> rgb and 0xFF
                        1 -> (rgb shr 8) and 0xFF
                        else -> (rgb shr 16) and 0xFF
                    }
                    input.put((value / 255f - 0.5f) / 0.5f)
                }
            }
        }
        input.rewind()
        OnnxTensor.createTensor(env, input, longArrayOf(1, 3, h.toLong(), w.toLong())).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                @Suppress("UNCHECKED_CAST")
                val steps = (result[0].value as Array<Array<FloatArray>>)[0]
                return decode(steps)
            }
        }
    }

    private fun decode(steps: Array<FloatArray>): Pair<String, Float> {
        val text = StringBuilder()
        var confidenceSum = 0f
        var kept = 0
        var previous = -1
        for (step in steps) {
            var best = 0
            for (i in step.indices) {
                if (step[i] > step[best]) {
                    best = i
                }
            }
            if (best != 0 && best != previous && best < dictionary.size) {
                text.append(dictionary[best])
                confidenceSum += step[best]
                kept++
            }
            previous = best
        }
        return text.toString() to (if (kept == 0) 0f else confidenceSum / kept)
    }
}

/** Detector plus one recogniser, with the line ordering of the reference pipeline. */
class PaddlePipeline(private val detector: PaddleDetector, private val recognizer: PaddleRecognizer) {
    fun run(image: BufferedImage): OcrOutput = recognizeBoxes(image, detect(image))

    fun detect(image: BufferedImage): List<TextBox> =
        detector.detect(image).sortedWith(compareBy({ (it.cy / 20).toInt() }, { it.cx }))

    fun recognizeBoxes(image: BufferedImage, boxes: List<TextBox>): OcrOutput {
        val lines = boxes.mapNotNull { box ->
            val (text, confidence) = recognizer.recognize(crop(image, box))
            if (text.isBlank()) {
                null
            } else {
                OcrLine(text, confidence, (box.cx - box.length / 2).toInt(), (box.cy - box.thickness / 2).toInt(), box.length.toInt(), box.thickness.toInt())
            }
        }
        return OcrOutput(lines)
    }

    /** Rotates the box upright and cuts it out; vertical boxes are turned a quarter. */
    private fun crop(image: BufferedImage, box: TextBox): BufferedImage {
        val w = max(1, box.length.roundToInt())
        val h = max(1, box.thickness.roundToInt())
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, w, h)
        val transform = AffineTransform()
        transform.translate(w / 2.0, h / 2.0)
        transform.rotate(-box.angle)
        transform.translate(-box.cx, -box.cy)
        g.drawImage(image, transform, null)
        g.dispose()
        return out
    }
}

fun resize(source: BufferedImage, w: Int, h: Int): BufferedImage {
    val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.drawImage(source, 0, 0, w, h, null)
    g.dispose()
    return out
}

@Suppress("UNCHECKED_CAST")
private fun flatten(value: Any): FloatArray {
    val maps = value as Array<Array<Array<FloatArray>>>
    val plane = maps[0][0]
    val out = FloatArray(plane.size * plane[0].size)
    var i = 0
    for (row in plane) {
        for (v in row) {
            out[i++] = v
        }
    }
    return out
}

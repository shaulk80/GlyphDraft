package spike

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import net.sourceforge.tess4j.Tesseract
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureNanoTime

private val models = File("data/spike-models")

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "fixtures" -> Fixtures.generate()
        "bench" -> bench(args.getOrNull(1) ?: "")
        "tess" -> tessBench(args.getOrNull(1) ?: "tessdata_fast")
        "native" -> nativeInfo()
        "tessjc" -> tessJavaCpp()
        "hebprobe" -> hebrewProbe()
        "show" -> show(args.getOrNull(1) ?: "en-skew", args.getOrNull(2) ?: "latin_PP-OCRv5_mobile_rec_onnx")
        else -> println("Usage: fixtures | bench [filter] | tess [tessdata folder] | native | show <fixture> <recognizer>")
    }
}

private fun options(env: OrtEnvironment): OrtSession.SessionOptions =
    OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(4)
        // Unused-initializer warnings from these models flood the log otherwise.
        setSessionLogLevel(ai.onnxruntime.OrtLoggingLevel.ORT_LOGGING_LEVEL_ERROR)
    }

private fun recognizerDirs() = listOf(
    "PP-OCRv5_mobile_rec_onnx", "latin_PP-OCRv5_mobile_rec_onnx",
    "eslav_PP-OCRv5_mobile_rec_onnx", "cyrillic_PP-OCRv5_mobile_rec_onnx",
)

private fun bench(filter: String) {
    val fixtures = Fixtures.load().filter { it.name.contains(filter) }
    val env = OrtEnvironment.getEnvironment()
    val options = options(env)
    val detector = PaddleDetector(env, File(models, "PP-OCRv5_mobile_det_onnx"), options)
    val recognizers = recognizerDirs().associateWith { PaddleRecognizer(env, File(models, it), options) }
    // Warm-up so that the first measured image does not pay for session start.
    val warm = ImageIO.read(fixtures.first().image)
    recognizers.values.first().let { PaddlePipeline(detector, it).run(warm) }

    println("| fixture | recognizer | CER % | ms | confidence |")
    println("|---|---|---|---|---|")
    val best = HashMap<String, Pair<String, Double>>()
    for (fixture in fixtures) {
        val image = ImageIO.read(fixture.image)
        for ((dir, recognizer) in recognizers) {
            lateinit var output: OcrOutput
            val ms = measureNanoTime { output = PaddlePipeline(detector, recognizer).run(image) } / 1_000_000
            val cer = Metrics.cer(fixture.expected, output.text)
            println("| ${fixture.name} | ${dir.removeSuffix("_onnx")} | ${"%.1f".format(cer * 100)} | $ms | ${"%.3f".format(output.confidence)} |")
            if (cer < (best[fixture.name]?.second ?: 2.0)) {
                best[fixture.name] = dir to cer
            }
        }
    }
    println()
    println("Best recognizer by CER per fixture:")
    best.forEach { (name, pair) -> println("- $name: ${pair.first.removeSuffix("_onnx")} (${"%.1f".format(pair.second * 100)} %)") }
    println()
    noLanguageStrategies(fixtures, detector, recognizers)
}

/** Strategy A: default model, read the script of its output, re-run with the matching recognizer.
 *  Strategy B: run every recognizer, keep the highest mean confidence. */
private fun noLanguageStrategies(fixtures: List<Fixture>, detector: PaddleDetector, recognizers: Map<String, PaddleRecognizer>) {
    println("No-language strategies (CER % and total ms)")
    println("| fixture | A: default then script re-run | B: best confidence of all | C: detect once, probe 2 longest lines |")
    println("|---|---|---|---|")
    for (fixture in fixtures) {
        val image = ImageIO.read(fixture.image)
        lateinit var a: OcrOutput
        val msA = measureNanoTime {
            val default = PaddlePipeline(detector, recognizers.getValue("PP-OCRv5_mobile_rec_onnx")).run(image)
            val target = Scripts.recognizerFor(default.text)
            a = if (target == "PP-OCRv5_mobile_rec_onnx") default else PaddlePipeline(detector, recognizers.getValue(target)).run(image)
        } / 1_000_000
        lateinit var b: OcrOutput
        val msB = measureNanoTime {
            b = recognizers.values.map { PaddlePipeline(detector, it).run(image) }.maxBy { it.confidence }
        } / 1_000_000
        lateinit var c: OcrOutput
        var chosen = ""
        val msC = measureNanoTime {
            val first = PaddlePipeline(detector, recognizers.values.first())
            val boxes = first.detect(image)
            val probe = boxes.sortedByDescending { it.length }.take(2)
            val winner = recognizers.entries.maxBy { (_, r) ->
                PaddlePipeline(detector, r).recognizeBoxes(image, probe).confidence
            }
            chosen = winner.key.removeSuffix("_onnx").removeSuffix("_PP-OCRv5_mobile_rec")
            c = PaddlePipeline(detector, winner.value).recognizeBoxes(image, boxes)
        } / 1_000_000
        val cerA = Metrics.cer(fixture.expected, a.text) * 100
        val cerB = Metrics.cer(fixture.expected, b.text) * 100
        val cerC = Metrics.cer(fixture.expected, c.text) * 100
        println("| ${fixture.name} | ${"%.1f".format(cerA)} ($msA ms) | ${"%.1f".format(cerB)} ($msB ms) | ${"%.1f".format(cerC)} ($msC ms, $chosen, conf ${"%.2f".format(c.confidence)}) |")
    }
}

private fun tessBench(folder: String) {
    val codes = mapOf("en" to "eng", "de" to "deu", "ru" to "rus", "he" to "heb", "zh" to "chi_sim", "ja" to "jpn")
    println("Tesseract ($folder)")
    println("| fixture | lang | CER % | ms |")
    println("|---|---|---|---|")
    for (fixture in Fixtures.load()) {
        val code = codes[fixture.language] ?: continue
        if (!File(models, "$folder/$code.traineddata").exists()) {
            continue
        }
        val tess = Tesseract()
        tess.setDatapath(File(models, folder).absolutePath)
        tess.setLanguage(code)
        lateinit var text: String
        val ms = measureNanoTime { text = tess.doOCR(ImageIO.read(fixture.image)) } / 1_000_000
        println("| ${fixture.name} | $code | ${"%.1f".format(Metrics.cer(fixture.expected, text) * 100)} | $ms |")
    }
}

/** Mean Tesseract confidence (0-100) of `heb` on every fixture: can it tell Hebrew from other scripts? */
private fun hebrewProbe() {
    println("| fixture | Tesseract heb confidence |")
    println("|---|---|")
    for (fixture in Fixtures.load()) {
        val tess = Tesseract()
        tess.setDatapath(File(models, "tessdata_fast").absolutePath)
        tess.setLanguage("heb")
        val words = tess.getWords(ImageIO.read(fixture.image), net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD)
        val mean = if (words.isEmpty()) 0.0 else words.map { it.confidence.toDouble() }.average()
        println("| ${fixture.name} | ${"%.0f".format(mean)} (${words.size} words) |")
    }
}

/** Tesseract through the JavaCPP presets (same API on JVM and Android). */
private fun tessJavaCpp() {
    val codes = mapOf("en" to "eng", "de" to "deu", "ru" to "rus", "he" to "heb", "zh" to "chi_sim", "ja" to "jpn")
    println("| fixture | lang | CER % | ms |")
    println("|---|---|---|---|")
    for (fixture in Fixtures.load()) {
        val code = codes[fixture.language] ?: continue
        val api = org.bytedeco.tesseract.TessBaseAPI()
        if (api.Init(File(models, "tessdata_fast").absolutePath, code) != 0) {
            println("| ${fixture.name} | $code | init failed | |")
            continue
        }
        lateinit var text: String
        val ms = measureNanoTime {
            val pix = org.bytedeco.leptonica.global.leptonica.pixRead(fixture.image.absolutePath)
            api.SetImage(pix)
            text = api.GetUTF8Text().string
            org.bytedeco.leptonica.global.leptonica.pixDestroy(pix)
        } / 1_000_000
        println("| ${fixture.name} | $code | ${"%.1f".format(Metrics.cer(fixture.expected, text) * 100)} | $ms |")
        api.End()
    }
}

private fun show(name: String, recognizerDir: String) {
    val fixture = Fixtures.load().first { it.name == name }
    val env = OrtEnvironment.getEnvironment()
    val options = options(env)
    val detector = PaddleDetector(env, File(models, "PP-OCRv5_mobile_det_onnx"), options)
    val output = PaddlePipeline(detector, PaddleRecognizer(env, File(models, recognizerDir), options)).run(ImageIO.read(fixture.image))
    output.lines.forEach { println("[${it.x},${it.y} ${it.w}x${it.h}] ${"%.2f".format(it.confidence)} ${it.text}") }
}

/** Shows where ONNX Runtime unpacks its native library. */
private fun nativeInfo() {
    val temp = File(System.getProperty("java.io.tmpdir"))
    val before = temp.list()?.toSet() ?: emptySet()
    OrtEnvironment.getEnvironment()
    val created = (temp.list()?.toSet() ?: emptySet()) - before
    println("java.io.tmpdir = $temp")
    println("onnxruntime.native.path = ${System.getProperty("onnxruntime.native.path")}")
    println("New entries in tmpdir after OrtEnvironment: $created")
    created.forEach { entry -> File(temp, entry).walkTopDown().forEach { println("  ${it.path} ${if (it.isFile) it.length() else ""}") } }
}

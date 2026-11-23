package spike

import java.io.File
import kotlin.system.measureTimeMillis

private val modelsRoot = File("data/spike-models/mt")
private val corpusRoot = File("engine/src/test/resources/translation")

private fun readLines(language: String): List<String> =
    File(corpusRoot, "$language.txt").readLines().filter { it.isNotBlank() }

/** Model folder names follow the Helsinki-NLP repository names: Japanese as a target is "jap". */
private fun pairDir(from: String, to: String): File {
    val target = if (to == "ja") "jap" else to
    return File(modelsRoot, "$from-$target")
}

private fun workingSetMb(): Long {
    val pid = ProcessHandle.current().pid()
    val process = ProcessBuilder("powershell", "-NoProfile", "-Command", "(Get-Process -Id $pid).WorkingSet64")
        .redirectErrorStream(true).start()
    val text = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    return (text.toLongOrNull() ?: 0L) / (1024 * 1024)
}

private fun translateAll(translator: MarianTranslator, sources: List<String>): Pair<List<String>, Long> {
    val out = ArrayList<String>()
    val ms = measureTimeMillis { sources.forEach { out.add(translator.translate(it)) } }
    return out to ms
}

private fun bench(pair: String, show: Boolean) {
    val (from, to) = pair.split("-")
    val sources = readLines(from)
    val references = readLines(to)
    val dir = pairDir(from, to)
    val sizeMb = File(dir, "onnx").listFiles().sumOf { it.length() } / 1_000_000.0
    var translator: MarianTranslator? = null
    val loadMs = measureTimeMillis { translator = MarianTranslator(dir) }
    val t = translator!!
    t.translate(sources.first())
    t.use {
        val (hyp, ms) = translateAll(t, sources)
        File(modelsRoot, "out").mkdirs()
        File(modelsRoot, "out/$pair.txt").writeText(hyp.joinToString("\n"))
        val chrf = Metrics.chrF(hyp, references)
        println("%s  chrF %.1f  load %d ms  %.0f ms/sentence  %.1f tok/s  files %.0f MB  rss %d MB  missingPieces %d".format(
            pair, chrf, loadMs, ms.toDouble() / sources.size, t.generatedTokens * 1000.0 / ms, sizeMb, workingSetMb(), t.tokenizer.missingInVocab))
        if (show) {
            for (i in 0 until minOf(6, sources.size)) {
                println("  SRC ${sources[i]}\n  HYP ${hyp[i]}\n  REF ${references[i]}")
            }
        }
    }
}

/** Source to English to target, the way the app would run a pair without a direct model. */
private fun pivot(from: String, to: String, via: String = "en") {
    val sources = readLines(from)
    val references = readLines(to)
    val (a, b) = MarianTranslator(pairDir(from, via)).use { first ->
        val (hyp, ms) = translateAll(first, sources)
        hyp to ms
    }
    MarianTranslator(pairDir(via, to)).use { second ->
        val (hyp, ms) = translateAll(second, a)
        File(modelsRoot, "out").mkdirs()
        File(modelsRoot, "out/$from-$via-$to.txt").writeText(hyp.joinToString("\n"))
        println("%s via %s  chrF %.1f  %.0f ms/sentence".format("$from-$to", via, Metrics.chrF(hyp, references), (b + ms).toDouble() / sources.size))
    }
}

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "inspect" -> MarianTranslator(File(args[1])).use { println(it.inspect()) }
        "bench" -> args.drop(1).forEach { bench(it, show = args.size == 2) }
        // "pivot de-fr" goes through en; "pivot en-he@ru" goes through ru.
        "pivot" -> args.drop(1).forEach {
            val via = it.substringAfter("@", "en")
            val pair = it.substringBefore("@")
            pivot(pair.substringBefore("-"), pair.substringAfter("-"), via)
        }
        else -> println("usage: inspect <dir> | bench <from-to>... | pivot <from-to>...")
    }
}

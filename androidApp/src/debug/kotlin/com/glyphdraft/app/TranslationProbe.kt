package com.glyphdraft.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.glyphdraft.core.Language
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.TranslationDirection
import com.glyphdraft.core.isInstalled
import com.glyphdraft.core.packFor
import com.glyphdraft.core.translationRoutes
import com.glyphdraft.engine.OnDeviceEngine
import com.glyphdraft.engine.ocr.AndroidImageDecoder
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only (it lives in `src/debug`, so no release APK has it). Starts with the process; when the app's files
 * folder has `probe.txt`, runs every line `<from code>|<to code>|<text>` through the on-device translator and logs, to
 * logcat tag `GlyphDraftProbe`, the download time of the models the route needs, the translation time and the memory of
 * the process (resident set and its peak, from `/proc/self/status`). Used for the M3 task 7 run on the emulator.
 */
class TranslationProbe : ContentProvider() {
    override fun onCreate(): Boolean {
        val files = context?.filesDir
        if (files == null) {
            return true
        }

        val script = File(files, SCRIPT)
        if (script.isFile) {
            Thread({ run(files, script.readLines()) }, "translation-probe").start()
        }

        return true
    }

    private fun run(files: File, lines: List<String>) {
        OnDeviceEngine(files.toPath().resolve("models"), AndroidImageDecoder()).use { engine ->
            log("start: ${memory()}")
            for (line in lines.filter { it.isNotBlank() }) {
                val parts = line.split("|", limit = 3)
                val from = Language.Supported.firstOrNull { it.code == parts[0] }
                val to = Language.Supported.firstOrNull { it.code == parts[1] }
                if (parts.size < 3 || from == null || to == null) {
                    log("bad line: $line")
                    continue
                }

                translate(engine, from, to, parts[2])
            }
        }
        log("done: ${memory()}")
    }

    private fun translate(engine: OnDeviceEngine, from: Language, to: Language, text: String) {
        val steps = engine.catalog.translationRoutes().steps(from, to)
        if (steps == null) {
            log("${from.code} to ${to.code}: no route")
            return
        }

        for (step in steps) {
            install(engine, step)
        }

        val started = SystemClock.elapsedRealtime()
        val outcome = runBlocking { engine.translator.translate(text, from, to) }
        val seconds = (SystemClock.elapsedRealtime() - started) / MILLIS
        when (outcome) {
            is Outcome.Failure -> log("${from.code} to ${to.code}: FAILED ${outcome.message}")
            is Outcome.Success ->
                log("${from.code} to ${to.code} via ${outcome.value.route.map { it.code }}: ${seconds}s, ${text.length} chars, ${memory()}: ${outcome.value.text.take(PREVIEW)}")
        }
    }

    private fun install(engine: OnDeviceEngine, direction: TranslationDirection) {
        val pack = engine.catalog.packFor(direction)
        if (pack == null || engine.catalog.isInstalled(pack)) {
            return
        }

        val started = SystemClock.elapsedRealtime()
        val outcome = runBlocking { engine.installer.install(pack) {} }
        val seconds = (SystemClock.elapsedRealtime() - started) / MILLIS
        log("install ${pack.id} (${pack.sizeBytes / BYTES_PER_MB} MB): ${if (outcome is Outcome.Failure) "FAILED ${outcome.message}" else "ok"} in ${seconds}s")
    }

    private fun memory(): String {
        val status = File("/proc/self/status").readLines().associate { it.substringBefore(':') to it.substringAfter(':').trim() }
        return "rss ${status["VmRSS"]}, peak ${status["VmHWM"]}"
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0

    private companion object {
        const val TAG = "GlyphDraftProbe"
        const val SCRIPT = "probe.txt"
        const val MILLIS = 1000.0
        const val BYTES_PER_MB = 1024L * 1024L
        const val PREVIEW = 160
    }
}

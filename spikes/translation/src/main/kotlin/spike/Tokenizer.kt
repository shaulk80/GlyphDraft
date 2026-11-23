package spike

import com.google.gson.JsonParser
import java.io.File

/**
 * SentencePiece unigram tokenizer for the OPUS-MT (Marian) models, written without a native library.
 * Pieces and scores come from `tokenizer.json`; ids come from `vocab.json`, because Marian ids are not SentencePiece ids.
 * Source side: whitespace split, "▁" prefix, Viterbi over the piece scores (best total score).
 * Target side: ids back to pieces, join, "▁" to space.
 */
class MarianTokenizer(dir: File) {
    private val pieceScore = HashMap<String, Float>()
    private val pieceId = HashMap<String, Int>()
    private val idPiece = HashMap<Int, String>()
    private var maxPieceLength = 1
    val unkId: Int
    val eosId = 0
    val padId: Int
    var missingInVocab = 0
        private set

    init {
        val vocab = JsonParser.parseString(File(dir, "vocab.json").readText()).asJsonObject
        for ((piece, id) in vocab.entrySet()) {
            pieceId[piece] = id.asInt
            idPiece[id.asInt] = piece
        }
        unkId = pieceId["<unk>"] ?: 1
        padId = pieceId["<pad>"] ?: (vocab.size() - 1)
        val spm = File(dir, "source.spm")
        val entries: List<Pair<String, Float>> = if (spm.exists() && System.getProperty("spm") != null) {
            readSpm(spm.readBytes())
        } else {
            JsonParser.parseString(File(dir, "tokenizer.json").readText()).asJsonObject
                .getAsJsonObject("model").getAsJsonArray("vocab")
                .map { it.asJsonArray[0].asString to it.asJsonArray[1].asFloat }
        }
        for ((piece, score) in entries) {
            if (piece !in pieceId) {
                missingInVocab++
                continue
            }
            pieceScore[piece] = score
            maxPieceLength = maxOf(maxPieceLength, piece.length)
        }
    }

    /** Minimal protobuf reader for a SentencePiece model: field 1 is the repeated piece (string 1, float score 2, int type 3). */
    private fun readSpm(bytes: ByteArray): List<Pair<String, Float>> {
        var pos = 0
        fun varint(): Long {
            var shift = 0
            var value = 0L
            while (true) {
                val b = bytes[pos++].toInt() and 0xff
                value = value or ((b and 0x7f).toLong() shl shift)
                if (b and 0x80 == 0) {
                    return value
                }
                shift += 7
            }
        }
        fun skip(wire: Int) {
            when (wire) {
                0 -> varint()
                1 -> pos += 8
                2 -> pos += varint().toInt()
                5 -> pos += 4
            }
        }
        val result = ArrayList<Pair<String, Float>>()
        while (pos < bytes.size) {
            val tag = varint().toInt()
            if (tag shr 3 != 1 || tag and 7 != 2) {
                skip(tag and 7)
                continue
            }
            val end = pos + varint().toInt()
            var piece = ""
            var score = 0f
            var type = 1
            while (pos < end) {
                val t = varint().toInt()
                when (t) {
                    10 -> {
                        val len = varint().toInt()
                        piece = String(bytes, pos, len, Charsets.UTF_8)
                        pos += len
                    }
                    21 -> {
                        score = java.lang.Float.intBitsToFloat(
                            (bytes[pos].toInt() and 0xff) or ((bytes[pos + 1].toInt() and 0xff) shl 8) or
                                ((bytes[pos + 2].toInt() and 0xff) shl 16) or ((bytes[pos + 3].toInt() and 0xff) shl 24))
                        pos += 4
                    }
                    24 -> type = varint().toInt()
                    else -> skip(t and 7)
                }
            }
            if (type == 1 || type == 4) {
                result.add(piece to score)
            }
        }
        return result
    }

    fun encode(text: String): IntArray {
        val ids = ArrayList<Int>()
        for (word in text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }) {
            segment("▁$word", ids)
        }
        ids.add(eosId)
        return ids.toIntArray()
    }

    private fun segment(word: String, out: MutableList<Int>) {
        val n = word.length
        val best = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val from = IntArray(n + 1)
        val isUnk = BooleanArray(n + 1)
        best[0] = 0f
        for (end in 1..n) {
            for (start in maxOf(0, end - maxPieceLength) until end) {
                if (best[start] == Float.NEGATIVE_INFINITY) {
                    continue
                }
                val score = pieceScore[word.substring(start, end)]
                if (score != null && best[start] + score > best[end]) {
                    best[end] = best[start] + score
                    from[end] = start
                    isUnk[end] = false
                }
            }
            if (best[end] == Float.NEGATIVE_INFINITY && best[end - 1] != Float.NEGATIVE_INFINITY) {
                best[end] = best[end - 1] - 100f
                from[end] = end - 1
                isUnk[end] = true
            }
        }
        val pieces = ArrayList<Int>()
        var pos = n
        while (pos > 0) {
            val start = from[pos]
            pieces.add(if (isUnk[pos]) unkId else pieceId.getValue(word.substring(start, pos)))
            pos = start
        }
        pieces.reverse()
        // Collapse runs of unknown characters into one unk, as SentencePiece does.
        var previousUnk = false
        for (id in pieces) {
            val unk = id == unkId
            if (!(unk && previousUnk)) {
                out.add(id)
            }
            previousUnk = unk
        }
    }

    fun decode(ids: List<Int>): String {
        val sb = StringBuilder()
        for (id in ids) {
            if (id == eosId || id == padId) {
                continue
            }
            sb.append(idPiece[id] ?: "")
        }
        return sb.toString().replace('▁', ' ').trim()
    }
}

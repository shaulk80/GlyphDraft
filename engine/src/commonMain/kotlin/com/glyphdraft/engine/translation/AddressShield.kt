package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.map

/**
 * Keeps e-mail addresses and web links away from a model, which rewrites or drops them (decision 0020). The model reads
 * a stand-in ("Z19") in place of each address. When it does not write every stand-in exactly once, the text between the
 * addresses is translated piece by piece instead. Either way the addresses come out as they went in.
 */
internal object AddressShield {
    /**
     * A link that begins with `http://`, `https://` or `www.`, or an e-mail address. ASCII only, so that Chinese,
     * Japanese or Hebrew letters written against an address are not taken as part of it. A link does not end in
     * punctuation, and a comma or semicolon ends it.
     */
    private val ADDRESS =
        Regex(
            """(?<![a-z0-9])(?:https?://|www\.)[a-z0-9._~:/?#\[\]@!$&'()*+=%-]*[a-z0-9/#=&_~%-]""" +
                """|[a-z0-9_][a-z0-9._%+-]*@[a-z0-9-]+(?:\.[a-z0-9-]+)+""",
            RegexOption.IGNORE_CASE,
        )

    /** [sentence] translated by [model], with its addresses as they were. */
    fun translate(sentence: String, model: (String) -> Outcome<String>): Outcome<String> {
        val addresses = ADDRESS.findAll(sentence).map { it.value }.toList()
        if (addresses.isEmpty()) {
            return model(sentence)
        }

        val between = sentence.split(ADDRESS)
        if (between.none(::hasLetters)) {
            return Outcome.Success(sentence)
        }

        val standIns = standInsFor(sentence, addresses.size)
        val translated =
            when (val whole = model(interleave(between, standIns))) {
                is Outcome.Failure -> return whole
                is Outcome.Success -> whole.value
            }
        if (standIns.all { occursOnce(it, translated) }) {
            return Outcome.Success(restore(translated, standIns, addresses))
        }

        return pieceByPiece(between, addresses, model)
    }

    /** [count] stand-ins that [sentence] does not contain already. */
    private fun standInsFor(sentence: String, count: Int): List<String> =
        generateSequence(1) { it + 1 }
            .map { "Z${it}9" }
            .filter { it !in sentence }
            .take(count)
            .toList()

    /** The pieces with one of [inserts] after each of them but the last. */
    private fun interleave(pieces: List<String>, inserts: List<String>): String =
        buildString {
            pieces.forEachIndexed { index, piece ->
                append(piece)
                append(inserts.getOrElse(index) { "" })
            }
        }

    private fun occursOnce(part: String, text: String): Boolean = text.split(part).size == 2

    private fun restore(translated: String, standIns: List<String>, addresses: List<String>): String =
        standIns.zip(addresses).fold(translated) { text, (standIn, address) -> text.replace(standIn, address) }

    private fun pieceByPiece(between: List<String>, addresses: List<String>, model: (String) -> Outcome<String>): Outcome<String> {
        val translated = ArrayList<String>(between.size)
        for (piece in between) {
            when (val result = translatePiece(piece, model)) {
                is Outcome.Failure -> return result
                is Outcome.Success -> translated.add(result.value)
            }
        }

        return Outcome.Success(interleave(translated, addresses))
    }

    /** The punctuation and spaces that tie [piece] to the addresses around it stay as they are. */
    private fun translatePiece(piece: String, model: (String) -> Outcome<String>): Outcome<String> {
        if (!hasLetters(piece)) {
            return Outcome.Success(piece)
        }

        val start = piece.indexOfFirst { it.isLetterOrDigit() }
        val end = piece.indexOfLast { !it.isWhitespace() } + 1
        return model(piece.substring(start, end)).map { piece.substring(0, start) + it + piece.substring(end) }
    }

    private fun hasLetters(text: String): Boolean = text.any { it.isLetter() }
}

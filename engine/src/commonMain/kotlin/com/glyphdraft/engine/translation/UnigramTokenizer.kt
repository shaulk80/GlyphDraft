package com.glyphdraft.engine.translation

/**
 * SentencePiece unigram tokenizer for the OPUS-MT (Marian) models, in plain Kotlin so that it runs on every target
 * without a native library.
 *
 * Encoding: the text is split at whitespace, every word gets the "▁" prefix and is cut into the pieces with the best
 * total score (Viterbi). A character no piece covers becomes one unknown id; a run of them is collapsed, as
 * SentencePiece does. Decoding joins the pieces and turns "▁" back into a space.
 *
 * The Marian source normalisation (NFKC-like) is not applied: full-width and compatibility forms in the input reach the
 * model as they are, and are unknown to it when it has no piece for them.
 */
class UnigramTokenizer(private val vocabulary: UnigramVocabulary) : SubwordTokenizer {
    private val maxPieceLength: Int = vocabulary.splitPieces.keys.maxOfOrNull { it.length } ?: 1

    override val endOfSequenceId: Int get() = vocabulary.endOfSequenceId

    override val padId: Int get() = vocabulary.padId

    override val vocabularySize: Int get() = vocabulary.size

    override fun encode(text: String): IntArray {
        val ids = ArrayList<Int>()
        for (word in words(text)) {
            addWord(WORD_MARK + word, ids)
        }
        ids.add(vocabulary.endOfSequenceId)

        return ids.toIntArray()
    }

    override fun decode(ids: List<Int>): String {
        val builder = StringBuilder()
        for (id in ids) {
            if (vocabulary.isSpecial(id)) {
                continue
            }
            builder.append(vocabulary.piecesById[id].orEmpty())
        }

        return builder.toString().replace(WORD_MARK, ' ').trim()
    }

    private fun words(text: String): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        for (character in text) {
            if (character.isWhitespace()) {
                flush(current, result)
            } else {
                current.append(character)
            }
        }
        flush(current, result)

        return result
    }

    private fun flush(current: StringBuilder, into: MutableList<String>) {
        if (current.isEmpty()) {
            return
        }

        into.add(current.toString())
        current.clear()
    }

    private fun addWord(word: String, into: MutableList<Int>) {
        val cuts = bestCuts(word)
        var previousUnknown = false
        for (cut in cuts) {
            val id = cut.id ?: vocabulary.unknownId
            val unknown = cut.id == null
            if (!(unknown && previousUnknown)) {
                into.add(id)
            }
            previousUnknown = unknown
        }
    }

    /** Pieces of [word] with the best total score; a character without a piece is an unknown cut, scored very low. */
    private fun bestCuts(word: String): List<Cut> {
        val length = word.length
        val best = FloatArray(length + 1) { Float.NEGATIVE_INFINITY }
        val start = IntArray(length + 1)
        val unknown = BooleanArray(length + 1)
        best[0] = 0f
        for (end in 1..length) {
            for (from in maxOf(0, end - maxPieceLength) until end) {
                if (best[from] == Float.NEGATIVE_INFINITY) {
                    continue
                }

                val score = vocabulary.splitPieces[word.substring(from, end)]
                if (score != null && best[from] + score > best[end]) {
                    best[end] = best[from] + score
                    start[end] = from
                    unknown[end] = false
                }
            }

            if (best[end] == Float.NEGATIVE_INFINITY) {
                // best[end - 1] is always reachable here: every earlier position was filled by a piece or by this branch.
                best[end] = best[end - 1] + UNKNOWN_SCORE
                start[end] = end - 1
                unknown[end] = true
            }
        }

        return collect(word, start, unknown)
    }

    private fun collect(word: String, start: IntArray, unknown: BooleanArray): List<Cut> {
        val cuts = ArrayList<Cut>()
        var position = word.length
        while (position > 0) {
            val from = start[position]
            val id = if (unknown[position]) null else vocabulary.idOf(word.substring(from, position))
            cuts.add(Cut(id))
            position = from
        }
        cuts.reverse()

        return cuts
    }

    /** One piece of a word; a null id is a character the vocabulary does not cover. */
    private class Cut(val id: Int?)

    private companion object {
        const val WORD_MARK = '▁'
        const val UNKNOWN_SCORE = -100f
    }
}

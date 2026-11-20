package com.glyphdraft.engine.ocr

import com.glyphdraft.core.Outcome

/**
 * The symbols a PP-OCRv5 recognizer can output, read from the `character_dict` list in the model's `inference.yml`.
 * Class 0 of the model is the CTC blank, classes 1..n are the listed symbols, and the last class is the space.
 */
class CharacterDictionary private constructor(private val symbols: List<String>) {
    /** Number of classes the model has to output: blank, the symbols, space. */
    val classCount: Int = symbols.size + 2

    /** The text of class [index]; empty for the blank and for an index the dictionary does not have. */
    fun symbol(index: Int): String =
        when {
            index <= 0 -> ""
            index <= symbols.size -> symbols[index - 1]
            index == symbols.size + 1 -> " "
            else -> ""
        }

    companion object {
        private const val LIST_KEY = "character_dict:"
        private const val ITEM_PREFIX = "  - "

        /**
         * Reads the list by hand: the file is machine-written YAML with one `  - symbol` line per entry (plain, or
         * in single quotes with `''` for a quote), and a YAML library that works on Android as well would be a
         * dependency for 20 lines.
         */
        fun parse(yaml: String): Outcome<CharacterDictionary> {
            val lines = yaml.lines().map { it.trimEnd('\r') }
            val keyLine = lines.indexOfFirst { it.trim() == LIST_KEY }
            if (keyLine < 0) {
                return Outcome.Failure("The recognizer's inference.yml has no character_dict list.")
            }

            val symbols =
                lines
                    .drop(keyLine + 1)
                    .takeWhile { it.startsWith(ITEM_PREFIX) }
                    .map { unquote(it.removePrefix(ITEM_PREFIX)) }
            if (symbols.isEmpty()) {
                return Outcome.Failure("The recognizer's character_dict list is empty.")
            }

            return Outcome.Success(CharacterDictionary(symbols))
        }

        private fun unquote(scalar: String): String {
            val quoted = scalar.length >= 2 && scalar.startsWith('\'') && scalar.endsWith('\'')
            if (!quoted) {
                return scalar
            }

            return scalar.substring(1, scalar.length - 1).replace("''", "'")
        }
    }
}

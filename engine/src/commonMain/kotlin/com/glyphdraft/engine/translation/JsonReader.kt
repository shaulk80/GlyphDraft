package com.glyphdraft.engine.translation

import com.glyphdraft.core.Outcome

/**
 * Reads JSON into plain Kotlin values: objects are `Map<String, Any?>`, arrays `List<Any?>`, strings `String`, numbers
 * `Double`, booleans `Boolean` and null `null`. It exists because the tokenizer files of the translation models are
 * JSON and the shared code has no JSON library; it accepts the full grammar but keeps no position information.
 */
internal object JsonReader {
    fun parse(text: String): Outcome<Any?> {
        val parser = JsonParser(text)
        return try {
            Outcome.Success(parser.readDocument())
        } catch (problem: JsonProblem) {
            Outcome.Failure("Not valid JSON: ${problem.message}", problem)
        }
    }
}

/**
 * Raised inside the recursive parser only and turned into an [Outcome.Failure] by [JsonReader.parse], so it never
 * reaches a caller. A recursive descent has no result channel at every level; threading one through would only add noise.
 */
private class JsonProblem(message: String) : Exception(message)

private class JsonParser(private val text: String) {
    private var position = 0

    fun readDocument(): Any? {
        if (text.startsWith(BYTE_ORDER_MARK)) {
            position = 1
        }
        val value = readValue()
        skipWhitespace()
        if (position != text.length) {
            fail("unexpected text after the value")
        }

        return value
    }

    private fun readValue(): Any? {
        skipWhitespace()
        val character = peek()

        return when {
            character == '{' -> readObject()
            character == '[' -> readArray()
            character == '"' -> readString()
            character == '-' || character in '0'..'9' -> readNumber()
            else -> readLiteral()
        }
    }

    private fun readObject(): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        position++
        skipWhitespace()
        if (peek() == '}') {
            position++
            return result
        }

        while (true) {
            skipWhitespace()
            if (peek() != '"') {
                fail("a name in quotes was expected")
            }
            val name = readString()
            skipWhitespace()
            expect(':')
            result[name] = readValue()
            skipWhitespace()
            if (!nextIsSeparator('}')) {
                return result
            }
        }
    }

    private fun readArray(): List<Any?> {
        val result = ArrayList<Any?>()
        position++
        skipWhitespace()
        if (peek() == ']') {
            position++
            return result
        }

        while (true) {
            result.add(readValue())
            skipWhitespace()
            if (!nextIsSeparator(']')) {
                return result
            }
        }
    }

    /** After a member: true for a comma (read on), false for [close] (the container ends); anything else is an error. */
    private fun nextIsSeparator(close: Char): Boolean {
        val character = peek()
        position++
        if (character == ',') {
            return true
        }
        if (character == close) {
            return false
        }

        fail("'$close' or ',' was expected")
    }

    private fun readString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            val character = next()
            when (character) {
                '"' -> return builder.toString()
                '\\' -> builder.append(readEscape())
                else -> builder.append(character)
            }
        }
    }

    private fun readEscape(): Char =
        when (val escape = next()) {
            '"', '\\', '/' -> escape
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> readUnicodeEscape()
            else -> fail("unknown escape \\$escape")
        }

    private fun readUnicodeEscape(): Char {
        if (position + 4 > text.length) {
            fail("a \\u escape is cut off")
        }
        val code = text.substring(position, position + 4).toIntOrNull(16) ?: fail("a \\u escape is not hexadecimal")
        position += 4

        return code.toChar()
    }

    private fun readNumber(): Double {
        val start = position
        while (position < text.length && text[position] in NUMBER_CHARACTERS) {
            position++
        }

        return text.substring(start, position).toDoubleOrNull() ?: fail("not a number")
    }

    private fun readLiteral(): Any? {
        for ((word, value) in LITERALS) {
            if (text.startsWith(word, position)) {
                position += word.length
                return value
            }
        }

        fail("a value was expected")
    }

    private fun skipWhitespace() {
        while (position < text.length && text[position] in WHITESPACE) {
            position++
        }
    }

    private fun peek(): Char {
        if (position >= text.length) {
            fail("the text ends too early")
        }

        return text[position]
    }

    private fun next(): Char {
        val character = peek()
        position++

        return character
    }

    private fun expect(character: Char) {
        if (next() != character) {
            position--
            fail("'$character' was expected")
        }
    }

    private fun fail(reason: String): Nothing = throw JsonProblem("$reason (at character $position)")

    private companion object {
        const val BYTE_ORDER_MARK = '﻿'
        const val WHITESPACE = " \t\r\n"
        const val NUMBER_CHARACTERS = "+-.eE0123456789"
        val LITERALS: List<Pair<String, Any?>> = listOf("true" to true, "false" to false, "null" to null)
    }
}

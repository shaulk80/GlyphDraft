package com.glyphdraft.core

/**
 * Result of an operation that can fail in an expected way (bad input, missing file, I/O error).
 * Used instead of exceptions: callers get a value plus a message they can show or log.
 */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(val message: String, val cause: Throwable? = null) : Outcome<Nothing>
}

fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }

fun <T> Outcome<T>.valueOrNull(): T? =
    when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> null
    }

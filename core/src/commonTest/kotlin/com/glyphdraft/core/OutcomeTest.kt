package com.glyphdraft.core

import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {
    @Test
    fun mapTransformsSuccessAndKeepsFailure() {
        val cases: List<Pair<Outcome<Int>, Outcome<String>>> =
            listOf(
                Outcome.Success(2) to Outcome.Success("2"),
                Outcome.Failure("bad input") to Outcome.Failure("bad input"),
            )

        for ((input, expected) in cases) {
            assertEquals(expected, input.map { it.toString() })
        }
    }

    @Test
    fun valueOrNullReturnsValueOnlyForSuccess() {
        val cases: List<Pair<Outcome<Int>, Int?>> =
            listOf(
                Outcome.Success(7) to 7,
                Outcome.Failure("missing") to null,
            )

        for ((input, expected) in cases) {
            assertEquals(expected, input.valueOrNull())
        }
    }
}

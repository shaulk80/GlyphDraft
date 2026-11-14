package com.glyphdraft.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResultBridgeTest {
    private val bridge = ResultBridge<String, Int>()

    @Test
    fun requestReturnsWhatIsDeliveredAndPassesTheInputToTheLauncher() =
        runTest {
            val cases: List<Int?> = listOf(42, null)

            for (delivered in cases) {
                val inputs = mutableListOf<String>()
                bridge.launcher = { input ->
                    inputs += input
                    Outcome.Success(Unit)
                }
                var outcome: Outcome<Int?>? = null

                launch { outcome = bridge.request("image/*") }
                runCurrent()
                bridge.deliver(delivered)
                advanceUntilIdle()

                assertEquals(listOf("image/*"), inputs)
                assertEquals(Outcome.Success(delivered), outcome)
            }
        }

    @Test
    fun withoutALauncherOrWhenItFailsTheRequestFails() =
        runTest {
            assertIs<Outcome.Failure>(bridge.request("x"))

            bridge.launcher = { Outcome.Failure("no app can handle this") }

            assertEquals(Outcome.Failure("no app can handle this"), bridge.request("x"))
        }

    @Test
    fun resultIsDeliveredToTheSameRequestAfterTheLauncherWasReplaced() =
        runTest {
            bridge.launcher = { Outcome.Success(Unit) }
            var outcome: Outcome<Int?>? = null
            launch { outcome = bridge.request("x") }
            runCurrent()

            bridge.launcher = null
            bridge.launcher = { Outcome.Success(Unit) }
            bridge.deliver(7)
            advanceUntilIdle()

            assertEquals(Outcome.Success(7), outcome)
        }

    @Test
    fun deliveryWithNothingWaitingIsIgnoredAndDoesNotAnswerTheNextRequest() =
        runTest {
            bridge.launcher = { Outcome.Success(Unit) }
            bridge.deliver(1)
            var outcome: Outcome<Int?>? = null

            launch { outcome = bridge.request("x") }
            runCurrent()

            assertEquals(null, outcome)
            bridge.deliver(2)
            advanceUntilIdle()
            assertEquals(Outcome.Success(2), outcome)
        }
}

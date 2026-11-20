package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OnnxSelfTestTest {
    @Test
    fun tinyModelRunsOnTheDesktopRuntime() =
        runTest {
            val outcome = OnnxSelfTest.run()

            val success = assertIs<Outcome.Success<String>>(outcome, outcome.toString())
            assertEquals(OnnxSelfTest.OK_MESSAGE, success.value)
        }

    @Test
    fun tinyModelIsTheExpectedSize() {
        assertEquals(116, TinyModel.bytes().size)
    }
}

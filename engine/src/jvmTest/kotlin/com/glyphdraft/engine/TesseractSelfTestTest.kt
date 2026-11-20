package com.glyphdraft.engine

import com.glyphdraft.core.Outcome
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TesseractSelfTestTest {
    @Test
    fun tesseractAndLeptonicaLoadOnTheDesktopRuntime() =
        runTest {
            val outcome = TesseractSelfTest.run()

            val success = assertIs<Outcome.Success<String>>(outcome, outcome.toString())
            assertTrue(success.value.startsWith(TesseractSelfTest.OK_MESSAGE), success.value)
        }

    @Test
    fun prepareCreatesTheCacheFolderAndSetsTheProperty() {
        val parent = createTempDirectory("javacpp-test")
        val target = parent.resolve("cache")
        val previous = System.getProperty(JavaCppNativeLibraries.CACHE_PROPERTY)
        try {
            val outcome = JavaCppNativeLibraries.prepare(target)

            assertIs<Outcome.Success<*>>(outcome, outcome.toString())
            assertTrue(target.exists())
            assertEquals(target.toAbsolutePath().toString(), System.getProperty(JavaCppNativeLibraries.CACHE_PROPERTY))
        } finally {
            restore(previous)
            parent.toFile().deleteRecursively()
        }
    }

    private fun restore(previous: String?) {
        if (previous == null) {
            System.clearProperty(JavaCppNativeLibraries.CACHE_PROPERTY)
            return
        }

        System.setProperty(JavaCppNativeLibraries.CACHE_PROPERTY, previous)
    }
}

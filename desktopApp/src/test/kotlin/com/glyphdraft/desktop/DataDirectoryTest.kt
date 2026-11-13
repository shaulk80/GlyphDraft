package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DataDirectoryTest {
    private val base = Files.createTempDirectory("glyphdraft-test").toString()

    private fun resolve(os: String, vararg variables: Pair<String, String>) =
        DataDirectory.resolve(
            environment = variables.toMap()::get,
            osName = os,
            userHome = Paths.get(base, "home").toString(),
        )

    @Test
    fun overrideVariableWinsOnEveryOs() {
        val custom = Paths.get(base, "custom")
        for (os in listOf("Windows 10", "Linux")) {
            val result = resolve(os, DataDirectory.OVERRIDE_VARIABLE to custom.toString(), "APPDATA" to "ignored")

            assertEquals(custom, result)
        }
    }

    @Test
    fun windowsUsesAppDataOrHome() {
        val appData = Paths.get(base, "appdata")

        assertEquals(appData.resolve("GlyphDraft"), resolve("Windows 10", "APPDATA" to appData.toString()))
        assertEquals(Paths.get(base, "home", "GlyphDraft"), resolve("Windows 10"))
    }

    @Test
    fun linuxUsesXdgDataHomeOrLocalShare() {
        val xdg = Paths.get(base, "xdg")

        assertEquals(xdg.resolve("glyphdraft"), resolve("Linux", "XDG_DATA_HOME" to xdg.toString()))
        assertEquals(Paths.get(base, "home", ".local", "share", "glyphdraft"), resolve("Linux"))
    }

    @Test
    fun fileLogCreatesFolderAndFile() {
        val logFile = Paths.get(base, "data", "logs", "glyphdraft.log")

        val result = FileLog(logFile).info("hello")

        assertIs<Outcome.Success<Unit>>(result)
        assertTrue(Files.readString(logFile).contains("hello"))
    }
}

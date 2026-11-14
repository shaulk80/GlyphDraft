package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LastFolderTest {
    private val dir = Files.createTempDirectory("glyphdraft-lastfolder")
    private val stateFile = dir.resolve("data").resolve("last-folder.txt")
    private val lastFolder = LastFolder(stateFile, FileLog(dir.resolve("logs").resolve("glyphdraft.log")))

    @Test
    fun nothingRememberedAtFirst() {
        assertNull(lastFolder.load())
    }

    @Test
    fun savedFolderIsLoadedAgainAndCreatesTheStateFolder() {
        val folder = Files.createDirectory(dir.resolve("photos"))

        val saved = lastFolder.save(folder)

        assertIs<Outcome.Success<Unit>>(saved)
        assertEquals(folder, lastFolder.load())
    }

    @Test
    fun folderThatNoLongerExistsOrGarbageIsIgnored() {
        val cases = listOf(dir.resolve("deleted-folder").toString(), "bad\u0000path")

        for (content in cases) {
            Files.createDirectories(stateFile.parent)
            Files.writeString(stateFile, content)

            assertNull(lastFolder.load())
        }
    }
}

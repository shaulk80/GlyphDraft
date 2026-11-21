package com.glyphdraft.desktop

import com.glyphdraft.engine.log.FileLog
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WindowIconTest {
    private val dir = Files.createTempDirectory("glyphdraft-icon")

    @Test
    fun theIconInTheResourcesLoadsAsASquareImage() {
        val icon = loadWindowIcon(FileLog(dir.resolve("logs").resolve("glyphdraft.log")))

        assertNotNull(icon)
        assertEquals(icon.intrinsicSize.width, icon.intrinsicSize.height)
    }
}

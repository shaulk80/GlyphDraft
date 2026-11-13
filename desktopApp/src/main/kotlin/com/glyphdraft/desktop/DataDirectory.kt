package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Where the app keeps its data and logs. The inputs are parameters so tests do not depend on the host OS. */
object DataDirectory {
    const val OVERRIDE_VARIABLE = "GLYPHDRAFT_DATA_DIR"
    private const val APP_FOLDER = "GlyphDraft"

    fun resolve(
        environment: (String) -> String? = System::getenv,
        osName: String = System.getProperty("os.name"),
        userHome: String = System.getProperty("user.home"),
    ): Path {
        val override = environment(OVERRIDE_VARIABLE)
        if (!override.isNullOrBlank()) {
            return Paths.get(override)
        }
        if (osName.startsWith("Windows", ignoreCase = true)) {
            val appData = environment("APPDATA")
            val root = if (appData.isNullOrBlank()) Paths.get(userHome) else Paths.get(appData)
            return root.resolve(APP_FOLDER)
        }
        val xdg = environment("XDG_DATA_HOME")
        if (!xdg.isNullOrBlank()) {
            return Paths.get(xdg).resolve(APP_FOLDER.lowercase())
        }
        return Paths.get(userHome, ".local", "share", APP_FOLDER.lowercase())
    }

    fun ensureExists(directory: Path): Outcome<Path> =
        try {
            Outcome.Success(Files.createDirectories(directory))
        } catch (e: java.io.IOException) {
            Outcome.Failure("Cannot create data folder $directory: ${e.message}", e)
        }
}

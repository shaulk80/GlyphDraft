package com.glyphdraft.core

enum class ThemeMode { System, Light, Dark }

/** What the user can set; kept between runs by a [SettingsStore]. */
data class Settings(
    val theme: ThemeMode = ThemeMode.System,
    /** Language preselected on the Home screen; null means "auto-detect". */
    val defaultLanguage: Language? = null,
)

/**
 * Keeps [Settings] between runs. Both calls do I/O, so they suspend. A missing or unreadable store is not an error for
 * the user: [load] then returns the defaults and the problem goes to the log; only [save] reports a failure, so the
 * screen can tell the user that a change will not survive a restart.
 */
interface SettingsStore {
    suspend fun load(): Settings

    suspend fun save(settings: Settings): Outcome<Unit>
}

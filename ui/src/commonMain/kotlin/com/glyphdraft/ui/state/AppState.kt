package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Language
import com.glyphdraft.core.RecognitionResult

enum class Screen { Home, Result, Settings }

enum class ThemeMode { System, Light, Dark }

/** What the app is busy with. Only one thing at a time: a second request while busy is ignored. */
enum class Activity { Idle, Picking, Recognizing, Saving }

/** A message for the user: an error or a plain confirmation. */
data class Notice(val text: String, val isError: Boolean)

/** User settings. In memory only in M1; persistence is designed with the settings model in M2. */
data class Settings(
    val theme: ThemeMode = ThemeMode.System,
    /** Language preselected on the Home screen; null means "auto-detect". */
    val defaultLanguage: Language? = null,
)

data class AppState(
    /** Last entry is the visible screen; Home is always at the bottom. */
    val backStack: List<Screen> = listOf(Screen.Home),
    val image: ImageInput? = null,
    /** Language chosen for the next recognition; null means "auto-detect". */
    val language: Language? = null,
    val result: RecognitionResult? = null,
    val activity: Activity = Activity.Idle,
    val notice: Notice? = null,
    val settings: Settings = Settings(),
) {
    val screen: Screen get() = backStack.last()

    val busy: Boolean get() = activity != Activity.Idle

    val canRecognize: Boolean get() = image != null && !busy

    /** True when the system Back key has something to do: cancel a running recognition or go back a screen. */
    val handlesBack: Boolean get() = activity == Activity.Recognizing || backStack.size > 1
}

package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Settings

enum class Screen { Home, Result, Settings }

/**
 * What the app is busy with. Only one thing at a time: a second request while busy is ignored. [Cancelling] is the short
 * time between "Cancel" and the engine returning from the native step it cannot interrupt; the progress indicator is
 * gone by then, but nothing new may start yet.
 */
enum class Activity { Idle, Picking, Installing, Recognizing, Cancelling, Saving }

/**
 * The dialog that offers to download [pack], then shows the download. [thenRecognize] continues with the recognition
 * that asked for the pack; [progress] is null until the first report; a non-null [error] means the download failed and
 * the dialog offers a retry.
 */
data class InstallPrompt(
    val pack: ModelPack,
    val thenRecognize: Boolean,
    val progress: InstallProgress? = null,
    val running: Boolean = false,
    val error: String? = null,
)

/** A message for the user: an error or a plain confirmation. */
data class Notice(val text: String, val isError: Boolean)

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
    /** Every model pack the app knows, installed or not, in the order the UI shows them. */
    val packs: List<ModelPack> = emptyList(),
    val installedPackIds: Set<String> = emptySet(),
    val installPrompt: InstallPrompt? = null,
) {
    val screen: Screen get() = backStack.last()

    /** Languages the pickers offer: everything a pack can read, installed or not. */
    val languages: List<Language> get() = packs.flatMap { it.languages }.distinct()

    fun isInstalled(pack: ModelPack): Boolean = pack.id in installedPackIds

    /** The name in the pickers; a language that needs a download first says so. */
    fun labelOf(language: Language): String {
        val usable = packs.any { isInstalled(it) && language in it.languages }
        if (usable) {
            return language.displayName
        }

        return "${language.displayName} (download needed)"
    }

    val busy: Boolean get() = activity != Activity.Idle

    val canRecognize: Boolean get() = image != null && !busy && installPrompt == null

    /** True when the system Back key has something to do: cancel a running download or recognition, close a dialog, or go back a screen. */
    val handlesBack: Boolean
        get() =
            activity == Activity.Recognizing ||
                activity == Activity.Cancelling ||
                activity == Activity.Installing ||
                installPrompt != null ||
                backStack.size > 1
}

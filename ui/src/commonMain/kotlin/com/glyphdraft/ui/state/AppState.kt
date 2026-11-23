package com.glyphdraft.ui.state

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Settings
import com.glyphdraft.core.Translation
import com.glyphdraft.core.TranslationRoutes

enum class Screen { Home, Result, Settings }

/**
 * What the app is busy with. Only one thing at a time: a second request while busy is ignored. [Cancelling] is the short
 * time between "Cancel" and the engine returning from the native step it cannot interrupt. The screen is already free
 * then (not [AppState.busy]); the [AppController] makes whatever starts next wait for the engine.
 */
enum class Activity { Idle, Picking, Installing, Recognizing, Translating, Cancelling, Saving }

/** What a download continues with once the models are installed. */
enum class AfterInstall { Nothing, Recognize, Translate }

/**
 * The dialog that offers to download [packs] (all of them, one after the other: a translation route can need several),
 * then shows the download. [then] continues with the action that asked for the models; [progress] covers all packs and
 * is null until the first report; a non-null [error] means the download failed and the dialog offers a retry.
 */
data class InstallPrompt(
    val packs: List<ModelPack>,
    val then: AfterInstall,
    val progress: InstallProgress? = null,
    val running: Boolean = false,
    val error: String? = null,
) {
    /** Download size of all offered packs. */
    val totalBytes: Long get() = packs.sumOf { it.sizeBytes }
}

/** A message for the user: an error or a plain confirmation. */
data class Notice(val text: String, val isError: Boolean)

data class AppState(
    /** Last entry is the visible screen; Home is always at the bottom. */
    val backStack: List<Screen> = listOf(Screen.Home),
    val image: ImageInput? = null,
    /** Language chosen for the next recognition; null means "auto-detect". */
    val language: Language? = null,
    /** Language to translate the next result into; null means "do not translate". */
    val target: Language? = null,
    val result: RecognitionResult? = null,
    /** The translation of [result] into [target]; null until it is made, and after another image is picked. */
    val translation: Translation? = null,
    val activity: Activity = Activity.Idle,
    val notice: Notice? = null,
    val settings: Settings = Settings(),
    /** Every model pack the app knows, installed or not, in the order the UI shows them. */
    val packs: List<ModelPack> = emptyList(),
    val installedPackIds: Set<String> = emptySet(),
    val installPrompt: InstallPrompt? = null,
) {
    val screen: Screen get() = backStack.last()

    /** Languages the text can be translated into, whether the models are installed or not. */
    val targets: List<Language> get() = translationRoutes.targets.toList()

    /** Routes over every translation model the app knows, installed or not. */
    val translationRoutes: TranslationRoutes
        get() = TranslationRoutes(packs.mapNotNull { it.direction })

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

    val busy: Boolean get() = activity != Activity.Idle && activity != Activity.Cancelling

    val canRecognize: Boolean get() = image != null && !busy && installPrompt == null

    val canTranslate: Boolean get() = result != null && target != null && !busy && installPrompt == null

    /** True when the system Back key has something to do: cancel a running download or recognition, close a dialog, or go back a screen. */
    val handlesBack: Boolean
        get() =
            activity == Activity.Recognizing ||
                activity == Activity.Translating ||
                activity == Activity.Installing ||
                installPrompt != null ||
                backStack.size > 1
}

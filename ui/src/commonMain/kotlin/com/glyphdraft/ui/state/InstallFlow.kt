package com.glyphdraft.ui.state

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.isInstalled
import com.glyphdraft.core.packFor
import com.glyphdraft.core.usableLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The first-use download of a model pack as it shows in [AppState.installPrompt]: offer, progress, failure with retry.
 * The [AppController] owns the coroutine and the busy flag; this class only moves the prompt through its steps.
 */
internal class InstallFlow(
    private val state: MutableStateFlow<AppState>,
    private val catalog: ModelCatalog,
    private val installer: ModelInstaller,
) {
    /** The pack that has to be installed before [language] can be read (null: auto-detect), or null when none has to. */
    fun packNeededFor(language: Language?): ModelPack? {
        if (language != null) {
            val pack = catalog.packFor(language)
            if (pack == null || catalog.isInstalled(pack)) {
                return null
            }

            return pack
        }

        // Translation models (they read nothing) do not count: a device with only those still has no way to read.
        if (catalog.usableLanguages().isEmpty()) {
            return catalog.packs.firstOrNull { it.direction == null }
        }

        return null
    }

    fun offer(pack: ModelPack, thenRecognize: Boolean) {
        state.update { it.copy(installPrompt = InstallPrompt(pack, thenRecognize), notice = null) }
    }

    fun dismiss() {
        state.update { it.copy(installPrompt = null) }
    }

    /**
     * Downloads the pack of [prompt] and returns whether it is installed now. A failure stays in the prompt, with its
     * message, so the dialog can offer a retry; success and cancellation close it.
     */
    suspend fun run(prompt: InstallPrompt): Boolean {
        state.update { it.copy(installPrompt = prompt.copy(running = true, progress = null, error = null)) }
        var installed = false
        try {
            when (val outcome = installer.install(prompt.pack, ::showProgress)) {
                is Outcome.Success -> installed = true
                is Outcome.Failure -> showError(outcome.message)
            }
        } finally {
            finish(installed)
        }

        return installed
    }

    private fun showProgress(progress: InstallProgress) {
        state.update { it.copy(installPrompt = it.installPrompt?.copy(progress = progress)) }
    }

    private fun showError(message: String) {
        state.update { it.copy(installPrompt = it.installPrompt?.copy(running = false, error = message)) }
    }

    private fun finish(installed: Boolean) {
        state.update { current ->
            val prompt = if (installed) null else current.installPrompt?.takeIf { it.error != null }
            current.copy(installPrompt = prompt, installedPackIds = catalog.installedPackIds.value)
        }
    }
}

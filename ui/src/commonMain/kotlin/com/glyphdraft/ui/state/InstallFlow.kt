package com.glyphdraft.ui.state

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import com.glyphdraft.core.isInstalled
import com.glyphdraft.core.packFor
import com.glyphdraft.core.packsForRoute
import com.glyphdraft.core.usableLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The first-use download of model packs as it shows in [AppState.installPrompt]: offer, progress, failure with retry.
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

    /**
     * The translation packs of the route from [from] to [to] that are not installed yet, in order of use (empty when
     * the translation can run now); null when the pair has no route.
     */
    fun packsNeededForTranslation(from: Language, to: Language): List<ModelPack>? =
        catalog.packsForRoute(from, to)?.filterNot { catalog.isInstalled(it) }

    fun offer(packs: List<ModelPack>, then: AfterInstall) {
        state.update { it.copy(installPrompt = InstallPrompt(packs, then), notice = null) }
    }

    fun dismiss() {
        state.update { it.copy(installPrompt = null) }
    }

    /**
     * Downloads the packs of [prompt] that are not installed yet, one after the other, and returns whether all of them
     * are installed now. Progress counts the bytes of all packs together. A failure stays in the prompt, with its
     * message, so the dialog can offer a retry (which skips the packs already installed); success and cancellation close it.
     */
    suspend fun run(prompt: InstallPrompt): Boolean {
        state.update { it.copy(installPrompt = prompt.copy(running = true, progress = null, error = null)) }
        var installed = false
        try {
            installed = installAll(prompt.packs.filterNot { catalog.isInstalled(it) })
        } finally {
            finish(installed)
        }

        return installed
    }

    private suspend fun installAll(pending: List<ModelPack>): Boolean {
        val total = pending.sumOf { it.sizeBytes }
        var finishedBytes = 0L
        for (pack in pending) {
            val before = finishedBytes
            val outcome = installer.install(pack) { showProgress(InstallProgress(before + it.bytesDone, total)) }
            if (outcome is Outcome.Failure) {
                showError(outcome.message)
                return false
            }

            finishedBytes += pack.sizeBytes
        }

        return true
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

package com.glyphdraft.engine.models

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Installs the packs of a [FileModelCatalog] by downloading the files its manifest lists that are not on disk yet.
 * Files that two packs share (the text detector) are downloaded once. Packs are installed one at a time. After a
 * failure or a cancel, files that were finished stay (the next install skips them); the unfinished one is gone.
 */
class HttpModelInstaller(
    private val catalog: FileModelCatalog,
    private val downloader: FileDownloader = FileDownloader(),
) : ModelInstaller {
    private val oneAtATime = Mutex()

    override suspend fun install(pack: ModelPack, onProgress: (InstallProgress) -> Unit): Outcome<Unit> =
        oneAtATime.withLock { installLocked(pack, onProgress) }

    private suspend fun installLocked(pack: ModelPack, onProgress: (InstallProgress) -> Unit): Outcome<Unit> {
        val manifest = catalog.manifestOf(pack.id)
        if (manifest == null) {
            return Outcome.Failure("Unknown model pack '${pack.id}'.")
        }

        val missing = manifest.files.filterNot(catalog::hasFile)
        val total = missing.sumOf { it.sizeBytes }
        var finishedBytes = 0L
        onProgress(InstallProgress(0L, total))
        for (file in missing) {
            val before = finishedBytes
            val outcome = downloader.download(file, catalog.pathOf(file)) { received ->
                onProgress(InstallProgress(before + received, total))
            }
            if (outcome is Outcome.Failure) {
                return outcome
            }

            finishedBytes += file.sizeBytes
        }

        catalog.refresh()
        return Outcome.Success(Unit)
    }
}

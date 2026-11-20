package com.glyphdraft.engine.models

import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelPack
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The catalog over the models folder: a pack is installed when every file of its manifest is there with the size the
 * manifest says. A file only gets its final name after its checksum matched (see [HttpModelInstaller]), so a file of
 * the right size is a verified one; the check at start is by size only, which keeps the start fast.
 */
class FileModelCatalog(
    private val manifests: List<PackManifest>,
    private val modelsDirectory: Path,
) : ModelCatalog {
    private val installedIds = MutableStateFlow(findInstalled())

    override val packs: List<ModelPack> = manifests.map { it.pack }

    override val installedPackIds: StateFlow<Set<String>> = installedIds

    /** Where [file] lives (or will live) on disk. */
    fun pathOf(file: ModelFile): Path = modelsDirectory.resolve(file.path)

    fun manifestOf(packId: String): PackManifest? = manifests.firstOrNull { it.pack.id == packId }

    /** True when [file] is on disk with the expected size. */
    fun hasFile(file: ModelFile): Boolean {
        val path = pathOf(file)
        return Files.isRegularFile(path) && Files.size(path) == file.sizeBytes
    }

    /** Looks at the disk again; called by the installer after it finished a pack. */
    fun refresh() {
        installedIds.value = findInstalled()
    }

    private fun findInstalled(): Set<String> =
        manifests
            .filter { manifest -> manifest.files.all(::hasFile) }
            .mapTo(mutableSetOf()) { it.pack.id }
}

package com.glyphdraft.core

import kotlinx.coroutines.flow.StateFlow

/**
 * One downloadable unit of models. [id] is stable (it names the folder in the data folder); [languages] are the
 * languages the pack lets the engine read; [sizeBytes] is the download size shown to the user before installing.
 */
data class ModelPack(
    val id: String,
    val displayName: String,
    val languages: Set<Language>,
    val sizeBytes: Long,
)

/** Which model packs exist and which of them are installed on this device. */
interface ModelCatalog {
    /** Every pack the app knows, installed or not, in the order the UI shows them. */
    val packs: List<ModelPack>

    /** Ids of the installed packs; emits again after an install finishes. */
    val installedPackIds: StateFlow<Set<String>>
}

/** Installed packs, in catalog order. */
fun ModelCatalog.installedPacks(): List<ModelPack> {
    val installed = installedPackIds.value
    return packs.filter { it.id in installed }
}

/** Languages that can be read right now, without any download. */
fun ModelCatalog.usableLanguages(): Set<Language> = installedPacks().flatMapTo(mutableSetOf()) { it.languages }

/** Languages the app can read once the right pack is installed (installed or not). */
fun ModelCatalog.knownLanguages(): Set<Language> = packs.flatMapTo(mutableSetOf()) { it.languages }

/** The installed pack that serves [language], else the first pack that would serve it, else null. */
fun ModelCatalog.packFor(language: Language): ModelPack? {
    val serving = packs.filter { language in it.languages }
    val installed = installedPackIds.value
    return serving.firstOrNull { it.id in installed } ?: serving.firstOrNull()
}

fun ModelCatalog.isInstalled(pack: ModelPack): Boolean = pack.id in installedPackIds.value

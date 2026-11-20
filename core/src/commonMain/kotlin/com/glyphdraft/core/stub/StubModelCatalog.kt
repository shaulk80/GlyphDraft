package com.glyphdraft.core.stub

import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelCatalog
import com.glyphdraft.core.ModelPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory catalog for tests and the skeleton: nothing is persisted. Packs listed in [installed] start installed. */
class StubModelCatalog(
    override val packs: List<ModelPack> = defaultPacks(),
    installed: Set<String> = packs.mapTo(mutableSetOf()) { it.id },
) : ModelCatalog {
    private val installedIds = MutableStateFlow(installed)

    override val installedPackIds: StateFlow<Set<String>> = installedIds

    fun markInstalled(packId: String) {
        installedIds.value = installedIds.value + packId
    }

    companion object {
        private const val MB = 1024L * 1024L

        fun defaultPacks(): List<ModelPack> =
            listOf(
                ModelPack("latin", "Latin scripts", setOf(Language.English, Language.German, Language.French, Language.Spanish), 13 * MB),
                ModelPack("eslav", "Russian", setOf(Language.Russian), 13 * MB),
                ModelPack("cjk", "Chinese and Japanese", setOf(Language.Chinese, Language.Japanese), 21 * MB),
                ModelPack("hebrew", "Hebrew", setOf(Language.Hebrew), 2 * MB),
            )
    }
}

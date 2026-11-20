package com.glyphdraft.core.stub

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.ModelInstaller
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.delay

/** Pretends to download: reports [steps] progress steps, waits [stepMillis] after each, then marks the pack installed. */
class StubModelInstaller(
    private val catalog: StubModelCatalog,
    private val steps: Int = DEFAULT_STEPS,
    private val stepMillis: Long = DEFAULT_STEP_MILLIS,
) : ModelInstaller {
    override suspend fun install(pack: ModelPack, onProgress: (InstallProgress) -> Unit): Outcome<Unit> {
        if (pack.id !in catalog.packs.map { it.id }) {
            return Outcome.Failure("Unknown model pack '${pack.id}'.")
        }

        if (pack.id in catalog.installedPackIds.value) {
            return Outcome.Success(Unit)
        }

        for (step in 1..steps) {
            delay(stepMillis)
            onProgress(InstallProgress(bytesDone = pack.sizeBytes * step / steps, bytesTotal = pack.sizeBytes))
        }

        catalog.markInstalled(pack.id)
        return Outcome.Success(Unit)
    }

    companion object {
        const val DEFAULT_STEPS = 10
        const val DEFAULT_STEP_MILLIS = 100L
    }
}

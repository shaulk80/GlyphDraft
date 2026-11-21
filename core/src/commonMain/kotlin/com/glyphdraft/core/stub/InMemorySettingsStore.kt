package com.glyphdraft.core.stub

import com.glyphdraft.core.Outcome
import com.glyphdraft.core.Settings
import com.glyphdraft.core.SettingsStore

/** Keeps the settings in memory only; for tests and for a run without a data folder. */
class InMemorySettingsStore(initial: Settings = Settings()) : SettingsStore {
    var current: Settings = initial
        private set

    override suspend fun load(): Settings = current

    override suspend fun save(settings: Settings): Outcome<Unit> {
        current = settings
        return Outcome.Success(Unit)
    }
}

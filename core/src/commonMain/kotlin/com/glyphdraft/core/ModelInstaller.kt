package com.glyphdraft.core

/** Download progress of one pack. [bytesTotal] is 0 while the size is not known yet. */
data class InstallProgress(val bytesDone: Long, val bytesTotal: Long) {
    /** Between 0 and 1, or null while the total is unknown. */
    val fraction: Double?
        get() {
            if (bytesTotal <= 0L) {
                return null
            }

            return (bytesDone.toDouble() / bytesTotal).coerceIn(0.0, 1.0)
        }
}

/** Downloads and installs model packs. Successful installs show up in [ModelCatalog.installedPackIds]. */
interface ModelInstaller {
    /**
     * Installs [pack], reporting progress through [onProgress] (may be called from any thread).
     * Expected failures (no network, checksum mismatch, disk full) come back as [Outcome.Failure]; nothing is
     * installed then. Installing an installed pack succeeds at once.
     *
     * Cancelling the calling coroutine stops the download and removes partial files; the pack stays not installed
     * and a later call starts again.
     */
    suspend fun install(pack: ModelPack, onProgress: (InstallProgress) -> Unit): Outcome<Unit>
}

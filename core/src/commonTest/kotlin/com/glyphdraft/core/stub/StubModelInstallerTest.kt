package com.glyphdraft.core.stub

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StubModelInstallerTest {
    private val pack = StubModelCatalog.defaultPacks().first()

    private fun notInstalledCatalog() = StubModelCatalog(installed = emptySet())

    @Test
    fun installReportsGrowingProgressAndMarksPackInstalled() =
        runTest {
            val catalog = notInstalledCatalog()
            val installer = StubModelInstaller(catalog, steps = 4, stepMillis = 10)
            val reported = mutableListOf<InstallProgress>()

            val outcome = installer.install(pack) { reported += it }

            assertIs<Outcome.Success<Unit>>(outcome)
            assertEquals(listOf(0.25, 0.5, 0.75, 1.0), reported.map { it.fraction })
            assertTrue(pack.id in catalog.installedPackIds.value)
        }

    @Test
    fun installedPackSucceedsWithoutProgress() =
        runTest {
            val catalog = StubModelCatalog()
            val reported = mutableListOf<InstallProgress>()

            val outcome = StubModelInstaller(catalog).install(pack) { reported += it }

            assertIs<Outcome.Success<Unit>>(outcome)
            assertTrue(reported.isEmpty())
        }

    @Test
    fun unknownPackIsAFailureNotAnException() =
        runTest {
            val stranger = ModelPack("nope", "Nope", emptySet(), 1)

            val outcome = StubModelInstaller(notInstalledCatalog()).install(stranger) {}

            assertIs<Outcome.Failure>(outcome)
        }

    @Test
    fun cancellingLeavesPackNotInstalled() =
        runTest {
            val catalog = notInstalledCatalog()
            val installer = StubModelInstaller(catalog, steps = 10, stepMillis = 100)
            val reported = mutableListOf<InstallProgress>()

            val job = launch { installer.install(pack) { reported += it } }
            delay(250)
            job.cancelAndJoin()

            assertEquals(2, reported.size)
            assertFalse(pack.id in catalog.installedPackIds.value)
        }
}

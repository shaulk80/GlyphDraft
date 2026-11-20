package com.glyphdraft.engine.models

import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.Language
import com.glyphdraft.core.ModelPack
import com.glyphdraft.core.Outcome
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.name
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HttpModelInstallerTest {
    private val server = TestModelServer()
    private val folder: Path = Files.createTempDirectory("glyphdraft-models")

    private val shared = server.fileFor("/det/model.onnx", ByteArray(150_000) { (it % 251).toByte() }, "det/model.onnx")
    private val latinModel = server.fileFor("/latin/model.onnx", ByteArray(200_000) { (it % 241).toByte() }, "latin/model.onnx")
    private val russianModel = server.fileFor("/ru/model.onnx", ByteArray(120_000) { (it % 239).toByte() }, "ru/model.onnx")

    private val latin = manifest("latin", shared, latinModel)
    private val russian = manifest("ru", shared, russianModel)
    private val catalog = FileModelCatalog(listOf(latin, russian), folder)
    private val installer = HttpModelInstaller(catalog, FileDownloader(connectTimeoutMillis = 2_000, readTimeoutMillis = 5_000))

    @AfterTest
    fun tearDown() {
        server.close()
        folder.toFile().deleteRecursively()
    }

    @Test
    fun installsAPackReportsProgressAndShowsItInTheCatalog() = blocking {
        val progress = CopyOnWriteArrayList<InstallProgress>()

        val outcome = installer.install(latin.pack) { progress.add(it) }

        assertIs<Outcome.Success<*>>(outcome)
        assertEquals(setOf("latin"), catalog.installedPackIds.value)
        assertTrue(Files.isRegularFile(folder.resolve("latin/model.onnx")))
        assertTrue(Files.isRegularFile(folder.resolve("det/model.onnx")))
        assertEquals(latin.pack.sizeBytes, progress.last().bytesDone)
        assertEquals(latin.pack.sizeBytes, progress.last().bytesTotal)
        assertEquals(0L, progress.first().bytesDone)
        assertEquals(progress.map { it.bytesDone }.sorted(), progress.map { it.bytesDone }, "progress never goes back")
        assertNoPartialFiles()
    }

    @Test
    fun installingAnInstalledPackSucceedsWithoutANetworkRequest() = blocking {
        installer.install(latin.pack) {}
        server.requests.clear()

        val outcome = installer.install(latin.pack) {}

        assertIs<Outcome.Success<*>>(outcome)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun aFileSharedByTwoPacksIsDownloadedOnce() = blocking {
        installer.install(latin.pack) {}
        server.requests.clear()

        val outcome = installer.install(russian.pack) {}

        assertIs<Outcome.Success<*>>(outcome)
        assertEquals(listOf("/ru/model.onnx"), server.requests.toList())
        assertEquals(setOf("latin", "ru"), catalog.installedPackIds.value)
    }

    @Test
    fun aPackIsInstalledAgainAfterItsFilesWereDeleted() = blocking {
        installer.install(latin.pack) {}
        Files.delete(folder.resolve("latin/model.onnx"))
        catalog.refresh()
        assertFalse("latin" in catalog.installedPackIds.value)

        val outcome = installer.install(latin.pack) {}

        assertIs<Outcome.Success<*>>(outcome)
        assertTrue("latin" in catalog.installedPackIds.value)
    }

    @Test
    fun aFileThatDoesNotMatchTheChecksumIsRefused() = blocking {
        val good = latinModel
        val corrupted = ByteArray(good.sizeBytes.toInt()) { 7 }
        server.serve("/latin/model.onnx", corrupted)

        val outcome = installer.install(latin.pack) {}

        val failure = assertIs<Outcome.Failure>(outcome)
        assertTrue("checksum" in failure.message, failure.message)
        assertFalse(Files.exists(folder.resolve("latin/model.onnx")))
        assertTrue(catalog.installedPackIds.value.isEmpty())
        assertNoPartialFiles()
    }

    @Test
    fun aFileOfTheWrongSizeIsRefused() = blocking {
        val cases =
            mapOf(
                "shorter" to ByteArray(1_000),
                "longer" to ByteArray(latinModel.sizeBytes.toInt() + 100_000),
            )

        for ((name, body) in cases) {
            server.serve("/latin/model.onnx", body)

            val outcome = installer.install(latin.pack) {}

            assertIs<Outcome.Failure>(outcome, name)
            assertFalse(Files.exists(folder.resolve("latin/model.onnx")), name)
            assertNoPartialFiles()
        }
    }

    @Test
    fun aServerErrorIsAFailureAndNothingIsInstalled() = blocking {
        for (status in listOf(404, 500, 403)) {
            server.failWith("/latin/model.onnx", status)

            val outcome = installer.install(latin.pack) {}

            val failure = assertIs<Outcome.Failure>(outcome, "status $status")
            assertTrue("$status" in failure.message, failure.message)
            assertFalse("latin" in catalog.installedPackIds.value)
        }
    }

    @Test
    fun noNetworkIsAFailureWithAReadableMessage() = blocking {
        val closedPort = ServerSocket(0).use { it.localPort }
        val unreachable = ModelFile("x/model.onnx", "http://127.0.0.1:$closedPort/model.onnx", latinModel.sha256, latinModel.sizeBytes)
        val offline = manifest("offline", unreachable)
        val offlineCatalog = FileModelCatalog(listOf(offline), folder)

        val outcome = HttpModelInstaller(offlineCatalog, FileDownloader(connectTimeoutMillis = 1_000)).install(offline.pack) {}

        val failure = assertIs<Outcome.Failure>(outcome)
        assertTrue("Cannot reach" in failure.message, failure.message)
        assertTrue(offlineCatalog.installedPackIds.value.isEmpty())
    }

    @Test
    fun unknownPackIsAFailure() = blocking {
        val outcome = installer.install(ModelPack("nope", "Nope", setOf(Language.English), 1)) {}

        assertIs<Outcome.Failure>(outcome)
    }

    @Test
    fun cancellingReturnsAtOnceEvenWhenTheServerStallsAndTheUnfinishedFileGoesAway() = blocking {
        server.stall("/latin/model.onnx")
        val started = CompletableDeferred<Unit>()
        // A read timeout far above the test timeouts: the cancel must not wait for it.
        val patient = HttpModelInstaller(catalog, FileDownloader(connectTimeoutMillis = 2_000, readTimeoutMillis = 120_000))

        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                patient.install(latin.pack) { progress ->
                    if (progress.bytesDone > shared.sizeBytes) {
                        started.complete(Unit)
                    }
                }
            }
        withTimeout(10_000) { started.await() }
        withTimeout(5_000) { job.cancelAndJoin() }

        assertTrue(job.isCancelled)
        assertFalse(Files.exists(folder.resolve("latin/model.onnx")))
        assertFalse("latin" in catalog.installedPackIds.value)
        assertTrue(Files.exists(folder.resolve("det/model.onnx")), "a finished file stays for the next try")

        // The transfer that was left blocked ends when the connection does, and takes its partial file with it.
        server.close()
        awaitNoPartialFiles()
    }

    @Test
    fun anInstallAfterACancelWorksWhileTheOldTransferIsStillEnding() = blocking {
        server.stall("/latin/model.onnx")
        val started = CompletableDeferred<Unit>()
        val patient = HttpModelInstaller(catalog, FileDownloader(connectTimeoutMillis = 2_000, readTimeoutMillis = 120_000))
        val job =
            launch(start = CoroutineStart.UNDISPATCHED) {
                patient.install(latin.pack) { progress ->
                    if (progress.bytesDone > shared.sizeBytes) {
                        started.complete(Unit)
                    }
                }
            }
        withTimeout(10_000) { started.await() }
        withTimeout(5_000) { job.cancelAndJoin() }

        // Same pack again, now from a server that answers in full; the stalled transfer is still blocked.
        TestModelServer().use { healthy ->
            val good = healthy.fileFor("/latin/model.onnx", ByteArray(latinModel.sizeBytes.toInt()) { (it % 241).toByte() }, "latin/model.onnx")
            val retry = manifest("latin", shared, good)
            val retryCatalog = FileModelCatalog(listOf(retry), folder)

            val outcome = HttpModelInstaller(retryCatalog).install(retry.pack) {}

            assertIs<Outcome.Success<*>>(outcome)
            assertTrue("latin" in retryCatalog.installedPackIds.value)
        }
    }

    @Test
    fun twoInstallsOfTheSamePackRunOneAfterTheOther() = blocking {
        val first = async { installer.install(latin.pack) {} }
        val second = async { installer.install(latin.pack) {} }

        assertIs<Outcome.Success<*>>(first.await())
        assertIs<Outcome.Success<*>>(second.await())
        assertEquals(1, server.requests.count { it == "/latin/model.onnx" })
    }

    // JUnit needs test methods that return Unit, which runBlocking does not always do.
    private fun blocking(block: suspend CoroutineScope.() -> Unit) {
        runBlocking { block() }
    }

    private fun partialFiles(): List<Path> =
        Files.walk(folder).use { stream -> stream.filter { FileDownloader.PARTIAL_SUFFIX in it.name }.toList() }

    private fun assertNoPartialFiles() {
        assertEquals(emptyList(), partialFiles())
    }

    private suspend fun awaitNoPartialFiles() {
        withTimeout(15_000) {
            while (partialFiles().isNotEmpty()) {
                delay(50)
            }
        }
    }

    private fun manifest(id: String, vararg files: ModelFile): PackManifest =
        PackManifest(ModelPack(id, id, setOf(Language.English), files.sumOf { it.sizeBytes }), files.toList())
}

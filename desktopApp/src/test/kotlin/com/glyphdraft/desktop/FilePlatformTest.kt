package com.glyphdraft.desktop

import com.glyphdraft.core.ImageInput
import com.glyphdraft.core.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeChooser(var toOpen: Path? = null, var toSave: Path? = null) : FileChooser {
    var suggestedName: String? = null
    var failure: Outcome.Failure? = null

    override suspend fun chooseToOpen(title: String, extensions: Set<String>): Outcome<Path?> = failure ?: Outcome.Success(toOpen)

    override suspend fun chooseToSave(title: String, suggestedName: String): Outcome<Path?> {
        this.suggestedName = suggestedName
        return failure ?: Outcome.Success(toSave)
    }
}

class FilePlatformTest {
    private val dir = Files.createTempDirectory("glyphdraft-files")
    private val logFile = dir.resolve("logs").resolve("glyphdraft.log")
    private val chooser = FakeChooser()
    private val picker = DesktopImagePicker(chooser, FileLog(logFile), Dispatchers.Unconfined)
    private val saver = DesktopTextSaver(chooser, FileLog(logFile), Dispatchers.Unconfined)

    @Test
    fun pickerReadsTheChosenFile() =
        runBlocking {
            val file = dir.resolve("scan.png")
            Files.write(file, byteArrayOf(1, 2, 3))
            chooser.toOpen = file

            val image = assertIs<Outcome.Success<ImageInput?>>(picker.pick()).value

            assertEquals("scan.png", image?.name)
            assertContentEquals(byteArrayOf(1, 2, 3), image?.bytes)
        }

    @Test
    fun dialogFailureIsPassedOnByPickerAndSaver() =
        runBlocking {
            val failure = Outcome.Failure("no dialog available")
            chooser.failure = failure

            assertEquals<Outcome<Any?>>(failure, picker.pick())
            assertEquals<Outcome<Any?>>(failure, saver.save("scan.txt", "text"))
        }

    @Test
    fun pickerReportsCancelAsNull() =
        runBlocking {
            chooser.toOpen = null

            val outcome = picker.pick()

            assertNull(assertIs<Outcome.Success<ImageInput?>>(outcome).value)
        }

    @Test
    fun pickerFailsOnMissingFileAndLogsIt() =
        runBlocking {
            chooser.toOpen = dir.resolve("gone.png")

            val outcome = picker.pick()

            assertIs<Outcome.Failure>(outcome)
            assertTrue(Files.readString(logFile).contains("ERROR Cannot read image"))
        }

    @Test
    fun saverWritesTextAndPassesSuggestedName() =
        runBlocking {
            val target = dir.resolve("out.txt")
            chooser.toSave = target

            val outcome = saver.save("scan.txt", "Привет, мир")

            assertEquals(Outcome.Success(true), outcome)
            assertEquals("scan.txt", chooser.suggestedName)
            assertEquals("Привет, мир", Files.readString(target))
        }

    @Test
    fun saverReportsCancelAsFalseAndWriteErrorAsFailure() =
        runBlocking {
            val cases: List<Pair<Path?, (Outcome<Boolean>) -> Unit>> =
                listOf(
                    null to { outcome -> assertEquals(Outcome.Success(false), outcome) },
                    dir.resolve("missing-folder").resolve("out.txt") to { outcome -> assertIs<Outcome.Failure>(outcome) },
                )

            for ((target, check) in cases) {
                chooser.toSave = target

                check(saver.save("scan.txt", "text"))
            }
        }
}

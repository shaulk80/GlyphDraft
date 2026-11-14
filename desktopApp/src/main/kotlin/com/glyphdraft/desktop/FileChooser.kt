package com.glyphdraft.desktop

import com.glyphdraft.core.Outcome
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogException
import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitPickerException
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.awt.Window
import java.nio.file.Path

/** Asks the user for a file. Success with null means the user cancelled; failure means no dialog could be shown. */
interface FileChooser {
    suspend fun chooseToOpen(title: String, extensions: Set<String>): Outcome<Path?>

    suspend fun chooseToSave(title: String, suggestedName: String): Outcome<Path?>
}

/**
 * The operating system's own dialogs through FileKit (Windows common dialog, XDG portal on Linux with an AWT fallback).
 * The dialogs are parented to [parent] and start in the folder of the previous choice, which [lastFolder] keeps across runs.
 */
class FileKitFileChooser(
    private val parent: Window?,
    private val lastFolder: LastFolder,
    private val log: FileLog,
) : FileChooser {
    override suspend fun chooseToOpen(title: String, extensions: Set<String>): Outcome<Path?> =
        show("Cannot open the file dialog") {
            FileKit.openFilePicker(
                type = FileKitType.File(extensions),
                directory = startFolder(),
                dialogSettings = settings(title),
            )
        }

    override suspend fun chooseToSave(title: String, suggestedName: String): Outcome<Path?> =
        show("Cannot open the save dialog") {
            FileKit.openFileSaver(
                suggestedName = suggestedName.substringBeforeLast('.'),
                defaultExtension = suggestedName.substringAfterLast('.', "txt"),
                directory = startFolder(),
                dialogSettings = settings(title),
            )
        }

    private suspend fun show(failureMessage: String, dialog: suspend () -> PlatformFile?): Outcome<Path?> {
        try {
            val chosen = dialog()
            val path = chosen?.file?.toPath()
            rememberFolderOf(path)
            return Outcome.Success(path)
        } catch (e: FileKitDialogException) {
            return failed(failureMessage, e)
        } catch (e: FileKitPickerException) {
            return failed(failureMessage, e)
        }
    }

    private fun failed(message: String, cause: Exception): Outcome.Failure {
        log.error(message, cause)
        return Outcome.Failure("$message: ${cause.message}", cause)
    }

    private fun rememberFolderOf(path: Path?) {
        val folder = path?.parent
        if (folder != null) {
            lastFolder.save(folder)
        }
    }

    private fun startFolder(): PlatformFile? = lastFolder.load()?.let { PlatformFile(it.toFile()) }

    private fun settings(title: String): FileKitDialogSettings =
        if (parent == null) {
            FileKitDialogSettings(title = title)
        } else {
            FileKitDialogSettings(title = title, parent = FileKitDialogParent.awt(parent))
        }
}

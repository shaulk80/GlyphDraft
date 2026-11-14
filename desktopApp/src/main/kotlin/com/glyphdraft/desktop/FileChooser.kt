package com.glyphdraft.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path
import java.nio.file.Paths

/** Asks the user for a file. Blocks until the dialog closes, so call it off the UI thread. Null means cancelled. */
interface FileChooser {
    fun chooseToOpen(title: String, extensions: Set<String>): Path?

    fun chooseToSave(title: String, suggestedName: String): Path?
}

/** The operating system's own file dialog (AWT). Windows ignores the extension filter and shows all files. */
class AwtFileChooser : FileChooser {
    override fun chooseToOpen(title: String, extensions: Set<String>): Path? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in extensions }
        return show(dialog)
    }

    override fun chooseToSave(title: String, suggestedName: String): Path? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = suggestedName
        return show(dialog)
    }

    private fun show(dialog: FileDialog): Path? {
        try {
            dialog.isVisible = true
            val name = dialog.file
            if (name == null) {
                return null
            }

            return Paths.get(dialog.directory ?: "", name)
        } finally {
            dialog.dispose()
        }
    }
}

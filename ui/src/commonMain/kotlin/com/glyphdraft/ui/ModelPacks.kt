package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glyphdraft.core.InstallProgress
import com.glyphdraft.core.ModelPack
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AfterInstall
import com.glyphdraft.ui.state.AppState
import com.glyphdraft.ui.state.InstallPrompt

private const val BYTES_PER_MB = 1024L * 1024L
private const val TENTHS = 10L

/** "13.4 MB": the download sizes the user sees, to one decimal. */
internal fun megabytes(bytes: Long): String {
    val tenths = bytes * TENTHS / BYTES_PER_MB
    return "${tenths / TENTHS}.${tenths % TENTHS} MB"
}

private fun languagesOf(pack: ModelPack): String = pack.languages.joinToString { it.displayName }

/** The download dialog of the first use: offer, progress with cancel, failure with retry. Shows nothing without a prompt. */
@Composable
fun InstallDialog(state: AppState, controller: AppController) {
    val prompt = state.installPrompt
    if (prompt == null) {
        return
    }

    AlertDialog(
        onDismissRequest = controller::dismissInstall,
        title = { Text(titleOf(prompt)) },
        text = { InstallDialogBody(prompt) },
        confirmButton = {
            if (!prompt.running) {
                TextButton(onClick = controller::confirmInstall) { Text(if (prompt.error == null) "Download" else "Retry") }
            }
        },
        dismissButton = {
            TextButton(onClick = controller::dismissInstall) { Text(if (prompt.running || prompt.error == null) "Cancel" else "Close") }
        },
    )
}

private fun titleOf(prompt: InstallPrompt): String =
    when {
        prompt.error != null -> "Download failed"
        prompt.running -> "Downloading ${namesOf(prompt)}"
        else -> "Download language models?"
    }

private fun namesOf(prompt: InstallPrompt): String = prompt.packs.joinToString { it.displayName }

private fun offerOf(prompt: InstallPrompt): String {
    val size = megabytes(prompt.totalBytes)
    if (prompt.then == AfterInstall.Translate) {
        return "Translating needs ${namesOf(prompt)}: $size, downloaded once."
    }

    val pack = prompt.packs.first()
    return "Reading ${languagesOf(pack)} needs the ${pack.displayName} models: $size, downloaded once."
}

@Composable
private fun InstallDialogBody(prompt: InstallPrompt) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            prompt.error != null -> Text(prompt.error)
            prompt.running -> DownloadProgress(prompt.progress)
            else -> {
                Text(offerOf(prompt))
                if (prompt.packs.any { it.experimental }) {
                    Text("Part of this translation is experimental: the quality is lower.")
                }
                Text("Afterwards it works offline. Images never leave this device.")
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: InstallProgress?) {
    val fraction = progress?.fraction
    if (fraction == null) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(progress = { fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
    }

    if (progress != null && progress.bytesTotal > 0L) {
        Text("${megabytes(progress.bytesDone)} of ${megabytes(progress.bytesTotal)}", style = MaterialTheme.typography.bodySmall)
    }
}

/** Under the name of a pack: what it reads (a translation model reads nothing) and the mark of an experimental one. */
@Composable
private fun PackDetail(pack: ModelPack) {
    val languages = languagesOf(pack)
    if (languages.isNotEmpty() && languages != pack.displayName) {
        Text(languages, style = MaterialTheme.typography.bodySmall)
    }

    if (pack.experimental) {
        Text("Experimental", style = MaterialTheme.typography.bodySmall)
    }
}

/** The settings list of model packs: size, whether installed, and the download button for those that are not. */
@Composable
fun ModelPackSection(state: AppState, controller: AppController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Language models", style = MaterialTheme.typography.titleMedium)
        for (pack in state.packs) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(pack.displayName)
                    PackDetail(pack)
                }
                if (state.isInstalled(pack)) {
                    Text("Installed")
                } else {
                    OutlinedButton(onClick = { controller.requestInstall(pack) }, enabled = !state.busy) {
                        Text("Download ${megabytes(pack.sizeBytes)}")
                    }
                }
            }
        }
    }
}

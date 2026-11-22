package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AppState
import kotlinx.coroutines.launch

/** The recognised text with the language it was read in, plus copy and save. */
@Composable
fun ResultScreen(state: AppState, controller: AppController) {
    val result = state.result
    CenteredColumn {
        if (result == null) {
            Text("Nothing recognised yet.")
        } else {
            ResultContent(result, state, controller)
        }
    }
}

@Composable
private fun ResultContent(result: RecognitionResult, state: AppState, controller: AppController) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Text(languageLine(result), style = MaterialTheme.typography.titleMedium)
    if (result.lowQuality) {
        LowQualityHint()
    }
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Text(result.text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            scope.launch {
                clipboard.setClipEntry(plainTextClipEntry(result.text))
                controller.showCopied()
            }
        }) { Text("Copy") }
        OutlinedButton(onClick = controller::saveResult, enabled = !state.busy) { Text("Save as…") }
    }
}

/** Warns that the text below is probably wrong and says what to try; the text stays visible and can still be copied. */
@Composable
private fun LowQualityHint() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
        Text(LOW_QUALITY_HINT, modifier = Modifier.padding(16.dp))
    }
}

internal const val LOW_QUALITY_HINT =
    "This text looks wrong. The language models for this image may not be installed, or another language was chosen. " +
        "Install the models in Settings or choose the language of the image, then read it again."

private fun languageLine(result: RecognitionResult): String {
    val how = if (result.detected) "detected" else "chosen"
    return "Language: ${result.language.displayName} ($how)"
}

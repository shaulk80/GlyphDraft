package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
    val translation = state.translation
    var showOriginal by remember(translation) { mutableStateOf(false) }
    val shownTranslation = translation.takeIf { !showOriginal }
    val shownText = shownTranslation?.text ?: result.text

    Text(languageLine(result), style = MaterialTheme.typography.titleMedium)
    if (result.lowQuality) {
        LowQualityHint()
    }
    TranslationControls(result, state, controller)
    if (translation != null) {
        TranslationHeader(translation, state, showOriginal, onShowOriginal = { showOriginal = it })
    }
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ResultText(shownText, Modifier.padding(16.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            scope.launch {
                clipboard.setClipEntry(plainTextClipEntry(shownText))
                controller.showCopied()
            }
        }) { Text("Copy") }
        OutlinedButton(onClick = { controller.saveResult(translated = shownTranslation != null) }, enabled = !state.busy) { Text("Save as…") }
    }
}

/**
 * The text, selectable with the mouse or the keyboard. A read-only text field rather than a `SelectionContainer`: it
 * takes the standard shortcuts of a text field (Ctrl+C and Ctrl+Insert to copy, Ctrl+A to select all), where the
 * container copies on Ctrl+C only (hand test, finding 2). The direction of each paragraph follows its first strong
 * character, so Hebrew reads right to left, sits at the right edge and keeps its full stop at the left end (finding 14).
 */
@Composable
private fun ResultText(text: String, modifier: Modifier = Modifier) {
    BasicTextField(
        value = text,
        onValueChange = {},
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current,
            textAlign = TextAlign.Start,
            textDirection = TextDirection.Content,
        ),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    )
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

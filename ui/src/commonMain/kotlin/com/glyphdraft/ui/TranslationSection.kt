package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glyphdraft.core.RecognitionResult
import com.glyphdraft.core.Translation
import com.glyphdraft.ui.state.Activity
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AppState
import com.glyphdraft.ui.state.routeInfo

internal const val EXPERIMENTAL_HINT =
    "Experimental: this translation is of lower quality or has not been checked well. Read it before you rely on it."

internal const val NOT_TRANSLATED_HINT =
    "The text was not translated because it looks wrong. Check it, or press Translate to translate it anyway."

/** Choose the target, translate (also again for another target), see the progress and cancel a running translation. */
@Composable
internal fun TranslationControls(result: RecognitionResult, state: AppState, controller: AppController) {
    if (state.targets.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Translate into", style = MaterialTheme.typography.titleMedium)
        TargetDropdown(
            selected = state.target,
            source = result.language,
            state = state,
            enabled = !state.busy,
            onSelected = controller::selectTarget,
        )
        if (state.activity == Activity.Translating) {
            TranslatingRow(controller)
        } else {
            TranslateRow(result, state, controller)
        }
    }
}

@Composable
private fun TranslatingRow(controller: AppController) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text("Translating…")
        OutlinedButton(onClick = controller::cancelRecognition) { Text("Cancel") }
    }
}

@Composable
private fun TranslateRow(result: RecognitionResult, state: AppState, controller: AppController) {
    val translated = state.translation?.to == state.target
    Button(onClick = controller::translate, enabled = state.canTranslate && !translated) { Text("Translate") }
    if (result.lowQuality && state.target != null && state.translation == null) {
        Text(NOT_TRANSLATED_HINT, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Above the text of a translation: the choice between the translation and the original, the route it took and, for an
 * experimental route, a warning. [showOriginal] is the state of the choice, kept by the caller.
 */
@Composable
internal fun TranslationHeader(translation: Translation, state: AppState, showOriginal: Boolean, onShowOriginal: (Boolean) -> Unit) {
    val info = state.routeInfo(translation.from, translation.to)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showOriginal, onClick = { onShowOriginal(false) }, label = { Text("Translation") })
            FilterChip(selected = showOriginal, onClick = { onShowOriginal(true) }, label = { Text("Original") })
        }
        Text(routeLine(translation, info?.viaPhrase), style = MaterialTheme.typography.bodyMedium)
        if (info?.experimental == true) {
            ExperimentalHint()
        }
    }
}

private fun routeLine(translation: Translation, viaPhrase: String?): String {
    val pair = "${translation.from.displayName} to ${translation.to.displayName}"
    if (viaPhrase == null) {
        return "Translated from $pair"
    }

    return "Translated from $pair, $viaPhrase"
}

@Composable
private fun ExperimentalHint() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
        Text(EXPERIMENTAL_HINT, modifier = Modifier.padding(16.dp))
    }
}

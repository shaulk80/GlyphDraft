package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glyphdraft.core.ImageInput
import com.glyphdraft.ui.state.Activity
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AppState

/** Pick an image, choose the language (or auto-detect), start recognition. */
@Composable
fun HomeScreen(state: AppState, controller: AppController) {
    BoxWithConstraints {
        val wide = maxWidth >= WideLayoutMinWidth
        CenteredColumn {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ImageCard(state, controller, Modifier.weight(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LanguageSection(state, controller)
                        RecognizeSection(state, controller)
                    }
                }
            } else {
                ImageCard(state, controller, Modifier.fillMaxWidth())
                LanguageSection(state, controller)
                RecognizeSection(state, controller)
            }
        }
    }
}

@Composable
private fun ImageCard(state: AppState, controller: AppController, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1. Image", style = MaterialTheme.typography.titleMedium)
            Text(describe(state.image), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = controller::pickImage, enabled = !state.busy) {
                Text(if (state.image == null) "Choose image…" else "Choose another image…")
            }
        }
    }
}

@Composable
private fun LanguageSection(state: AppState, controller: AppController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("2. Language of the text", style = MaterialTheme.typography.titleMedium)
        LanguageDropdown(selected = state.language, enabled = !state.busy, onSelected = controller::selectLanguage)
    }
}

@Composable
private fun RecognizeSection(state: AppState, controller: AppController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("3. Read the text", style = MaterialTheme.typography.titleMedium)
        if (state.activity == Activity.Recognizing) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                OutlinedButton(onClick = controller::cancelRecognition) { Text("Cancel") }
            }
        } else {
            Button(onClick = controller::recognize, enabled = state.canRecognize) { Text("Recognize text") }
        }
    }
}

private fun describe(image: ImageInput?): String =
    if (image == null) {
        "No image chosen yet."
    } else {
        "${image.name} (${image.bytes.size} bytes)"
    }

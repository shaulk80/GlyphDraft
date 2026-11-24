package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AppState
import com.glyphdraft.core.ThemeMode

/** Theme, default language and translation, the language models and where recognition runs. */
@Composable
fun SettingsScreen(state: AppState, controller: AppController, version: String) {
    CenteredColumn {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            for (mode in ThemeMode.entries) {
                ThemeOption(mode, selected = state.settings.theme == mode, onSelect = { controller.setTheme(mode) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Default language", style = MaterialTheme.typography.titleMedium)
            LanguageDropdown(
                selected = state.settings.defaultLanguage,
                state = state,
                enabled = true,
                onSelected = controller::setDefaultLanguage,
            )
        }
        DefaultTargetSection(state, controller)
        ModelPackSection(state, controller)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Engine", style = MaterialTheme.typography.titleMedium)
            Text("On this device. Images are never uploaded; only the language models are downloaded, once.")
        }
        CreditsSection(state)
        Text("GlyphDraft $version", style = MaterialTheme.typography.bodySmall)
    }
}

/** The CC-BY 4.0 attribution for the translation models; shown when the models can translate at all (the README has the same words). */
@Composable
private fun CreditsSection(state: AppState) {
    if (state.targets.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Credits", style = MaterialTheme.typography.titleMedium)
        Text(TRANSLATION_CREDITS)
    }
}

/**
 * Translation models are OPUS-MT models by the Helsinki-NLP group (CC-BY 4.0 or Apache 2.0, treated as CC-BY 4.0), converted
 * to ONNX by Xenova and, for the two Hebrew ones, by a community uploader (decisions 0012 and 0016).
 */
internal const val TRANSLATION_CREDITS =
    "Translation uses OPUS-MT models by Helsinki-NLP (University of Helsinki), licensed CC-BY 4.0, " +
        "converted to ONNX by Xenova and, for the Hebrew models, by R4kSo1997. Changes: int8 quantization and conversion only."

/** Shown when the models can translate at all; changing it also preselects the target on the Home screen. */
@Composable
private fun DefaultTargetSection(state: AppState, controller: AppController) {
    if (state.targets.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Default translation", style = MaterialTheme.typography.titleMedium)
        TargetDropdown(
            selected = state.settings.defaultTarget,
            source = null,
            state = state,
            enabled = true,
            onSelected = controller::setDefaultTarget,
        )
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(mode.name, modifier = Modifier.padding(start = 8.dp))
    }
}

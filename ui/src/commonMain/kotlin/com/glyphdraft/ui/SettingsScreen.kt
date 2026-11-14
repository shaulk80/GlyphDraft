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
import com.glyphdraft.ui.state.ThemeMode

/** Theme, default language and which engine is in use. Kept in memory in M1. */
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
                enabled = true,
                onSelected = controller::setDefaultLanguage,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Engine", style = MaterialTheme.typography.titleMedium)
            Text("Stub (returns fixed text; real recognition arrives in the next milestone)")
        }
        Text("GlyphDraft $version", style = MaterialTheme.typography.bodySmall)
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

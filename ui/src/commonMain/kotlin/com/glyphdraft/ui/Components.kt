package com.glyphdraft.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glyphdraft.core.Language
import com.glyphdraft.ui.state.Notice

/** Widest the content grows; on a wide window it stays readable and centred. */
val ContentMaxWidth: Dp = 720.dp

/** Windows at least this wide put side-by-side what a phone stacks. */
val WideLayoutMinWidth: Dp = 600.dp

const val AUTO_DETECT_LABEL = "Auto-detect"

/** Dropdown over [Language.Supported] plus "Auto-detect" (null). */
@Composable
fun LanguageDropdown(selected: Language?, enabled: Boolean, onSelected: (Language?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.displayName ?: AUTO_DETECT_LABEL)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options: List<Language?> = listOf(null) + Language.Supported
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(option?.displayName ?: AUTO_DETECT_LABEL) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

/** An error or confirmation shown above the screen content until dismissed or the user navigates. */
@Composable
fun NoticeBanner(notice: Notice, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val colors =
        if (notice.isError) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        }
    Card(colors = colors, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(notice.text, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/** Centres [content] and limits its width to [ContentMaxWidth]. */
@Composable
fun CenteredColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

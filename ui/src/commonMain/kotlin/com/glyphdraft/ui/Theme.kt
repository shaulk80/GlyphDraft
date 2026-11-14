package com.glyphdraft.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.glyphdraft.ui.state.ThemeMode

/** Resolves the user's theme choice; "System" follows the operating system. */
@Composable
fun isDarkTheme(mode: ThemeMode): Boolean =
    when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

@Composable
fun GlyphDraftTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}

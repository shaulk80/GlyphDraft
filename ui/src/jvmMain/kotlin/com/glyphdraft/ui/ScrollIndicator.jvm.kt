package com.glyphdraft.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun VerticalScrollIndicator(state: ScrollState, modifier: Modifier) {
    VerticalScrollbar(adapter = rememberScrollbarAdapter(state), modifier = modifier)
}

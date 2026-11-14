package com.glyphdraft.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A draggable scrollbar on desktop; nothing on Android, where touch scrolling needs none. */
@Composable
internal expect fun VerticalScrollIndicator(state: ScrollState, modifier: Modifier)

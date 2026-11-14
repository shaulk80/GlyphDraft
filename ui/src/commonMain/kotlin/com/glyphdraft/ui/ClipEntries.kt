package com.glyphdraft.ui

import androidx.compose.ui.platform.ClipEntry

/** Builds a clipboard entry holding plain [text]; the entry type is platform-specific. */
internal expect fun plainTextClipEntry(text: String): ClipEntry

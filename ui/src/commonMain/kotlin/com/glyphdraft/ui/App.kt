package com.glyphdraft.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.glyphdraft.ui.state.AppController
import com.glyphdraft.ui.state.AppState
import com.glyphdraft.ui.state.Screen

/**
 * The whole UI, shared by the desktop and Android apps.
 * [onDarkThemeResolved] lets the platform match its system bars to the theme actually shown.
 */
@Composable
fun App(controller: AppController, version: String, onDarkThemeResolved: (Boolean) -> Unit = {}) {
    val state by controller.state.collectAsState()
    val dark = isDarkTheme(state.settings.theme)
    SideEffect { onDarkThemeResolved(dark) }

    GlyphDraftTheme(dark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppScaffold(state, controller, version)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(state: AppState, controller: AppController, version: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleOf(state.screen)) },
                navigationIcon = {
                    if (state.screen != Screen.Home) {
                        TextButton(onClick = controller::back, enabled = !state.busy) { Text("← Back") }
                    }
                },
                actions = {
                    if (state.screen == Screen.Home) {
                        TextButton(onClick = controller::openSettings, enabled = !state.busy) { Text("Settings") }
                    }
                },
            )
        },
    ) { padding ->
        val scrollState = rememberScrollState()
        Box(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                state.notice?.let { notice ->
                    CenteredColumn { NoticeBanner(notice, onDismiss = controller::dismissNotice) }
                }
                when (state.screen) {
                    Screen.Home -> HomeScreen(state, controller)
                    Screen.Result -> ResultScreen(state, controller)
                    Screen.Settings -> SettingsScreen(state, controller, version)
                }
            }
            if (scrollState.canScrollForward || scrollState.canScrollBackward) {
                VerticalScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}

private fun titleOf(screen: Screen): String =
    when (screen) {
        Screen.Home -> "GlyphDraft"
        Screen.Result -> "Result"
        Screen.Settings -> "Settings"
    }

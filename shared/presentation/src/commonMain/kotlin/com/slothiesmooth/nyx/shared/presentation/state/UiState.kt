package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.runtime.Stable

/**
 * Coarse rendering state a screen can be in. `Loading` overlays non-blocking progress; `Blocking`
 * shows a modal spinner; `Error` shows a dismissible dialog.
 */
@Stable
sealed interface UiState {
    data object Ready : UiState
    data object Loading : UiState
    data object Blocking : UiState

    data class Error(
        val title: String,
        val cause: Throwable?,
        val onExit: () -> Unit,
    ) : UiState
}

/**
 * Builds an [UiState.Error] from a throwable, keeping the exception as [UiState.Error.cause].
 */
fun Throwable.toErrorState(title: String, onExit: () -> Unit): UiState.Error =
    UiState.Error(title = title, cause = this, onExit = onExit)

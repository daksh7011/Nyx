package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Read-only view of a screen's state: coarse [uiState] plus a stream of one-shot [uiEvent]s.
 */
@Stable
interface ViewState {
    val uiState: UiState
    val uiEvent: Flow<UiEvent>
}

/**
 * Mutable [ViewState] base a screen's concrete state extends. [uiState] is Compose snapshot state
 * so writes trigger recomposition; [uiEvent] is a hot flow one-shot events are emitted onto.
 */
abstract class MutableViewState(initialState: UiState = UiState.Ready) : ViewState {
    final override var uiEvent: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    final override var uiState by mutableStateOf(initialState)
}

/**
 * Runs [onTry]; on a non-cancellation throwable, maps it to [UiState.Error] whose `onExit` resets
 * the state to [UiState.Ready]. Cancellation is rethrown-safe (swallowed here, never surfaced as
 * an error dialog).
 */
suspend fun <T : MutableViewState> T.tryCatch(
    title: String,
    onTry: suspend T.() -> Unit,
    onCatch: suspend T.(Throwable) -> Unit = { throwable ->
        if (!throwable.isCancellation()) {
            uiState = throwable.toErrorState(title) { uiState = UiState.Ready }
        }
    },
) {
    runCatching { onTry() }.onFailure { onCatch(it) }
}

/**
 * Emits a one-shot [UiEvent] to subscribers of [ViewState.uiEvent].
 */
suspend fun <T : MutableViewState> T.notify(event: UiEvent) {
    uiEvent.emit(event)
}

private fun Throwable.isCancellation(): Boolean {
    var current: Throwable? = this
    while (current != null && current !is CancellationException) {
        if (current == current.cause) return false
        current = current.cause
    }
    return current is CancellationException
}

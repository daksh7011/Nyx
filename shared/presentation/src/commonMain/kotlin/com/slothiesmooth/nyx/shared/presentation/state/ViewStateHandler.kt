package com.slothiesmooth.nyx.shared.presentation.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.filterNotNull

private val DialogPadding = 24.dp
private val CardPadding = 16.dp
private val SpinnerSize = 40.dp
private val SpinnerStroke = 3.dp

/**
 * Renders [content] and overlays the state-driven affordances (blocking spinner / loading slot /
 * error dialog) for [state], forwarding one-shot [UiEvent]s to [onEvent].
 */
@Composable
fun ViewStateHandler(
    state: ViewState,
    onEvent: suspend (UiEvent) -> Unit = {},
    blockingSlot: @Composable (UiState.Blocking) -> Unit = { ViewStateBlocking() },
    loadingSlot: @Composable (UiState.Loading) -> Unit = {},
    errorSlot: @Composable (UiState.Error) -> Unit = { ViewStateError(it) },
    content: @Composable () -> Unit,
) {
    LaunchedEffect(state) {
        state.uiEvent.filterNotNull().collect(onEvent)
    }

    content()

    StateOverlay(
        state = state,
        blockingSlot = blockingSlot,
        loadingSlot = loadingSlot,
        errorSlot = errorSlot,
    )
}

@Composable
@NonRestartableComposable
private fun StateOverlay(
    state: ViewState,
    blockingSlot: @Composable (UiState.Blocking) -> Unit,
    loadingSlot: @Composable (UiState.Loading) -> Unit,
    errorSlot: @Composable (UiState.Error) -> Unit,
) {
    when (val uiState = state.uiState) {
        is UiState.Blocking -> blockingSlot(uiState)
        is UiState.Loading -> loadingSlot(uiState)
        is UiState.Error -> errorSlot(uiState)
        else -> Unit
    }
}

/** Default modal blocking spinner. */
@Composable
fun ViewStateBlocking() {
    BasicAlertDialog(onDismissRequest = {}) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card {
                CircularProgressIndicator(
                    modifier = Modifier.padding(CardPadding).size(SpinnerSize),
                    strokeWidth = SpinnerStroke,
                )
            }
        }
    }
}

/** Default error dialog for [UiState.Error]. */
@Composable
fun ViewStateError(uiState: UiState.Error) {
    AlertDialog(
        modifier = Modifier.padding(DialogPadding),
        onDismissRequest = uiState.onExit,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
        title = { Text(text = uiState.title) },
        text = {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = uiState.cause?.message
                    ?: uiState.cause?.stackTraceToString()
                    ?: uiState.title,
            )
        },
        confirmButton = {
            TextButton(onClick = uiState.onExit) { Text("OK") }
        },
    )
}

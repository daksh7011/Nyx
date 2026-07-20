package com.slothiesmooth.nyx.shared.presentation.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class SampleState : MutableViewState()

class MutableViewStateTest {

    @Test
    fun `tryCatch maps a throwable to an error state`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Encrypt failed", onTry = { throw IllegalStateException("boom") })

        val error = state.uiState
        assertIs<UiState.Error>(error)
        assertEquals("Encrypt failed", error.title)
        assertEquals("boom", error.cause?.message)
    }

    @Test
    fun `error onExit resets state to ready`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Boom", onTry = { throw IllegalStateException("x") })
        val error = state.uiState
        assertIs<UiState.Error>(error)
        error.onExit()

        assertEquals(UiState.Ready, state.uiState)
    }

    @Test
    fun `tryCatch swallows cancellation and stays ready`() = runTest {
        val state = SampleState()

        state.tryCatch(title = "Boom", onTry = { throw CancellationException("cancelled") })

        assertEquals(UiState.Ready, state.uiState)
    }
}

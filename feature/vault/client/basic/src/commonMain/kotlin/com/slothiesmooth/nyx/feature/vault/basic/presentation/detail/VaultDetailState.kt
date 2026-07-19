package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.UiEvent
import com.slothiesmooth.nyx.shared.presentation.state.ViewState

/** Read-only detail state the screen observes; the thumbnail is already decoded and date formatted. */
interface VaultDetailState : ViewState {
    val name: String
    val createdLabel: String
    val thumbnail: ImageBitmap?
    val isArchived: Boolean
    val isLoading: Boolean
}

/** Mutable backing state owned by [VaultDetailViewModel] and reused by previews. */
class VaultDetailMutableState : MutableViewState(), VaultDetailState {
    override var name: String by mutableStateOf("")
    override var createdLabel: String by mutableStateOf("")
    override var thumbnail: ImageBitmap? by mutableStateOf(null)
    override var isArchived: Boolean by mutableStateOf(false)
    override var isLoading: Boolean by mutableStateOf(true)
}

/** One-shot events emitted by the detail screen. */
sealed interface VaultDetailUiEvent : UiEvent {
    /** The image no longer exists (deleted here or elsewhere) — the screen should pop back. */
    data object Closed : VaultDetailUiEvent
}

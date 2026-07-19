package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.presentation.state.MutableViewState
import com.slothiesmooth.nyx.shared.presentation.state.ViewState
import com.slothiesmooth.nyx.shared.presentation.text.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A single vault image, already decoded and formatted for direct rendering by the grid. */
@Immutable
data class VaultImageUi(
    val id: StegoImageId,
    val name: String,
    val createdLabel: UiText,
    val thumbnail: ImageBitmap?,
)

/** Read-only vault list state the screen observes; every field is render-ready. */
interface VaultState : ViewState {
    val active: ImmutableList<VaultImageUi>
    val archived: ImmutableList<VaultImageUi>
    val showArchived: Boolean
    val isLoading: Boolean
}

/** Mutable backing state owned by [VaultViewModel] and reused by previews. */
class VaultMutableState : MutableViewState(), VaultState {
    override var active: ImmutableList<VaultImageUi> by mutableStateOf(persistentListOf())
    override var archived: ImmutableList<VaultImageUi> by mutableStateOf(persistentListOf())
    override var showArchived: Boolean by mutableStateOf(false)
    override var isLoading: Boolean by mutableStateOf(true)
}

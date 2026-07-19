package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultImageUseCases
import com.slothiesmooth.nyx.feature.vault.basic.presentation.formatVaultDate
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.state.notify
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.combine

/**
 * Drives the detail screen for a single stego image: watches both the active and archived streams so
 * it reflects archive/restore/delete happening anywhere, maps the target image into render-ready
 * state, and exposes the share/archive/restore/delete actions. When the image disappears from both
 * streams (deleted here or elsewhere) the reactive collector is the single source that emits
 * [VaultDetailUiEvent.Closed] so the screen pops. All mapping and formatting stay out of the composables.
 *
 * Takes the shared [VaultImageUseCases] bundle plus [shareImage] directly (only the detail screen
 * shares, so it is not part of the bundle) so the constructor stays under the detekt parameter gate.
 */
class VaultDetailViewModel(
    private val useCases: VaultImageUseCases,
    private val shareImage: ShareVaultImageUseCase,
    private val clock: Clock,
) : BaseViewModel() {

    private val mutableState = VaultDetailMutableState()
    val state: VaultDetailState get() = mutableState
    private var currentId: StegoImageId? = null

    fun load(imageId: StegoImageId) {
        if (currentId == imageId) return
        currentId = imageId
        async("observe-detail", force = true) {
            combine(useCases.observeActive(), useCases.observeArchived()) { active, archived ->
                (active + archived).firstOrNull { it.id == imageId }
            }.collect { image ->
                if (image == null) mutableState.notify(VaultDetailUiEvent.Closed) else applyImage(image)
            }
        }
    }

    private suspend fun applyImage(image: VaultImage) {
        val thumbnail = decode(useCases.getImageBytes(image.id))
        withState {
            mutableState.name = image.name
            mutableState.createdLabel = formatVaultDate(image.createdAt, clock.zone())
            mutableState.isArchived = image.isArchived
            mutableState.thumbnail = thumbnail
            mutableState.isLoading = false
        }
    }

    private fun decode(bytes: AppResult<ByteArray>): ImageBitmap? =
        if (bytes is AppResult.Ok) runCatching { bytes.value.toImageBitmap() }.getOrNull() else null

    fun archive() {
        val id = currentId ?: return
        async("archive") { useCases.archive(id) }
    }

    fun restore() {
        val id = currentId ?: return
        async("restore") { useCases.restore(id) }
    }

    fun share() {
        val id = currentId ?: return
        async("share") { shareImage(id, mutableState.name) }
    }

    /**
     * Soft-deletes the image. This does not emit [VaultDetailUiEvent.Closed] directly: the delete makes
     * the image drop out of both observed streams, and the [load] collector fires the single `Closed`
     * event. Emitting here as well would double-fire and could double-pop the nav stack.
     */
    fun delete() {
        val id = currentId ?: return
        async("delete") { useCases.delete(id) }
    }
}

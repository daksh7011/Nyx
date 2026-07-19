package com.slothiesmooth.nyx.feature.vault.basic.presentation.detail

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
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
 * state, and exposes the share/archive/restore/delete actions. Emits [VaultDetailUiEvent.Closed] when
 * the image disappears so the screen pops. All mapping and formatting stay out of the composables.
 */
class VaultDetailViewModel(
    private val useCases: VaultImageUseCases,
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
        async("share") { useCases.share(id, mutableState.name) }
    }

    fun delete() {
        val id = currentId ?: return
        async("delete") {
            if (useCases.delete(id) is AppResult.Ok) mutableState.notify(VaultDetailUiEvent.Closed)
        }
    }
}

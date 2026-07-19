package com.slothiesmooth.nyx.feature.vault.basic.presentation.list

import androidx.compose.ui.graphics.ImageBitmap
import com.slothiesmooth.nyx.feature.vault.basic.presentation.VaultImageUseCases
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.time.Clock
import com.slothiesmooth.nyx.shared.presentation.image.toImageBitmap
import com.slothiesmooth.nyx.shared.presentation.viewmodel.BaseViewModel

/**
 * Drives the vault grid: collects the active and archived image streams, maps each into render-ready
 * rows off the main thread (image decoding + date formatting), and exposes archive/restore/delete
 * mutations. All logic lives here; [VaultContent] only renders [state].
 */
class VaultViewModel(
    private val useCases: VaultImageUseCases,
    private val clock: Clock,
) : BaseViewModel() {

    private val mutableState = VaultMutableState()
    val state: VaultState get() = mutableState

    private val decode: (ByteArray) -> ImageBitmap? = { bytes -> runCatching { bytes.toImageBitmap() }.getOrNull() }

    override fun doInit() {
        async("observe-active") {
            useCases.observeActive().collect { images ->
                val rows = buildVaultRows(images, useCases.getImageBytes, clock.zone(), decode)
                withState {
                    mutableState.active = rows
                    mutableState.isLoading = false
                }
            }
        }
        async("observe-archived") {
            useCases.observeArchived().collect { images ->
                val rows = buildVaultRows(images, useCases.getImageBytes, clock.zone(), decode)
                withState { mutableState.archived = rows }
            }
        }
    }

    fun toggleArchived() = withState { mutableState.showArchived = !mutableState.showArchived }

    fun archive(id: StegoImageId) {
        async("archive-${id.value}") { useCases.archive(id) }
    }

    fun restore(id: StegoImageId) {
        async("restore-${id.value}") { useCases.restore(id) }
    }

    fun delete(id: StegoImageId) {
        async("delete-${id.value}") { useCases.delete(id) }
    }
}

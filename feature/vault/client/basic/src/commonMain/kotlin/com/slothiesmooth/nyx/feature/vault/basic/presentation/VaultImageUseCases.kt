package com.slothiesmooth.nyx.feature.vault.basic.presentation

import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase

/**
 * Parameter object bundling the six vault use cases BOTH the list and detail view models depend on
 * (observe/read/archive/restore/delete). A parameter object is detekt's root-cause fix for the
 * `LongParameterList` gate: each view model otherwise takes six use cases plus a clock, which exceeds
 * the constructor threshold. Sharing use cases (`ShareVaultImageUseCase`) is intentionally NOT here —
 * only the detail view model uses it, so it is injected there directly rather than forced onto the
 * list view model as an unused collaborator.
 */
data class VaultImageUseCases(
    val observeActive: ObserveVaultImagesUseCase,
    val observeArchived: ObserveArchivedImagesUseCase,
    val getImageBytes: GetImageBytesUseCase,
    val archive: ArchiveImageUseCase,
    val restore: RestoreImageUseCase,
    val delete: DeleteImageUseCase,
)

package com.slothiesmooth.nyx.feature.vault.basic.presentation

import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ArchiveImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.DeleteImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.GetImageBytesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveArchivedImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ObserveVaultImagesUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.RestoreImageUseCase
import com.slothiesmooth.nyx.feature.vault.basic.domain.usecase.ShareVaultImageUseCase

/**
 * Parameter object bundling the vault use cases the list and detail view models depend on. Injected
 * as a single collaborator so each view model stays small; registered once in the feature's DI graph.
 */
data class VaultImageUseCases(
    val observeActive: ObserveVaultImagesUseCase,
    val observeArchived: ObserveArchivedImagesUseCase,
    val getImageBytes: GetImageBytesUseCase,
    val archive: ArchiveImageUseCase,
    val restore: RestoreImageUseCase,
    val delete: DeleteImageUseCase,
    val share: ShareVaultImageUseCase,
)

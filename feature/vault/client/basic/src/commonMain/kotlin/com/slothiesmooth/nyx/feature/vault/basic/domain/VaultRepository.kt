package com.slothiesmooth.nyx.feature.vault.basic.domain

import com.slothiesmooth.nyx.feature.vault.basic.domain.model.VaultImage
import com.slothiesmooth.nyx.shared.data.id.StegoImageId
import com.slothiesmooth.nyx.shared.data.result.AppResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    fun observeActive(): Flow<ImmutableList<VaultImage>>
    fun observeArchived(): Flow<ImmutableList<VaultImage>>
    suspend fun imageBytes(id: StegoImageId): AppResult<ByteArray>
    suspend fun archive(id: StegoImageId): AppResult<Unit>
    suspend fun restore(id: StegoImageId): AppResult<Unit>
    suspend fun softDelete(id: StegoImageId): AppResult<Unit>
}

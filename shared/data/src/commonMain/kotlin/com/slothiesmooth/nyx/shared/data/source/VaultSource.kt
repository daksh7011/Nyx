package com.slothiesmooth.nyx.shared.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Metadata store for stego images (the DB seam). Reads return `Flow`; writes are suspend.
 * `:client`'s `VaultSqlSource` implements this over SqlDelight; web uses an in-memory impl.
 */
interface VaultSource {
    fun observeActive(): Flow<List<StegoImageRecord>>
    fun observeArchived(): Flow<List<StegoImageRecord>>
    suspend fun getById(id: String): StegoImageRecord?
    suspend fun upsert(record: StegoImageRecord)
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: String)
    suspend fun softDelete(id: String, deletedAt: String)
    suspend fun purgeAll()
    suspend fun countActive(): Int
}

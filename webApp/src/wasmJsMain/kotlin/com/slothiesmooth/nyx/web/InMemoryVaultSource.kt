package com.slothiesmooth.nyx.web

import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Session-only [VaultSource] for wasm (no persistent vault on web in v1). Mirrors VaultSqlSource
 * semantics: active = not archived and not tombstoned, newest first.
 */
class InMemoryVaultSource : VaultSource {

    private val records = MutableStateFlow<PersistentList<StegoImageRecord>>(persistentListOf())

    override fun observeActive(): Flow<List<StegoImageRecord>> =
        records.map { list -> list.filter(::isActive).sortedByDescending { record -> record.createdAt } }

    override fun observeArchived(): Flow<List<StegoImageRecord>> =
        records.map { list -> list.filter(::isArchived).sortedByDescending { record -> record.createdAt } }

    override suspend fun getById(id: String): StegoImageRecord? =
        records.value.firstOrNull { record -> record.id == id }

    override suspend fun upsert(record: StegoImageRecord) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == record.id }
            if (index >= 0) list.replacingAt(index, record) else list.adding(record)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == id }
            if (index < 0) return@update list
            list.replacingAt(index, list[index].copy(isArchived = archived, updatedAt = updatedAt))
        }
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        records.update { list ->
            val index = list.indexOfFirst { existing -> existing.id == id }
            if (index < 0) list else list.replacingAt(index, list[index].copy(deletedAt = deletedAt))
        }
    }

    override suspend fun purgeAll() {
        records.value = persistentListOf()
    }

    override suspend fun countActive(): Int = records.value.count(::isActive)

    private fun isActive(record: StegoImageRecord): Boolean = !record.isArchived && record.deletedAt == null

    private fun isArchived(record: StegoImageRecord): Boolean = record.isArchived && record.deletedAt == null
}

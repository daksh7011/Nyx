package com.slothiesmooth.nyx.shared.testsupport

import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [VaultSource]; observation is `MutableStateFlow`-backed so active/archived views stay live. */
class FakeVaultSource : VaultSource {
    private val records = MutableStateFlow<List<StegoImageRecord>>(emptyList())

    /** Set to force the next [upsert] to throw — used to test rollback paths. */
    var failNextUpsert: Boolean = false

    private fun isActive(record: StegoImageRecord) = !record.isArchived && record.deletedAt == null
    private fun isArchived(record: StegoImageRecord) = record.isArchived && record.deletedAt == null

    override fun observeActive(): Flow<List<StegoImageRecord>> = records.map { list -> list.filter(::isActive) }
    override fun observeArchived(): Flow<List<StegoImageRecord>> = records.map { list -> list.filter(::isArchived) }

    override suspend fun getById(id: String): StegoImageRecord? = records.value.firstOrNull { it.id == id }

    override suspend fun upsert(record: StegoImageRecord) {
        if (failNextUpsert) {
            failNextUpsert = false
            error("forced upsert failure")
        }
        records.value = records.value.filterNot { it.id == record.id } + record
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        records.value = records.value.map { if (it.id == id) it.copy(isArchived = archived, updatedAt = updatedAt) else it }
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        records.value = records.value.map { if (it.id == id) it.copy(deletedAt = deletedAt) else it }
    }

    override suspend fun purgeAll() { records.value = emptyList() }

    override suspend fun countActive(): Int = records.value.count(::isActive)
}

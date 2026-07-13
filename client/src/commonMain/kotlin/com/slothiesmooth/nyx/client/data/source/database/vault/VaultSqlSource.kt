package com.slothiesmooth.nyx.client.data.source.database.vault

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.slothiesmooth.nyx.client.data.source.database.sqldelight.SqlDelightSource
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import com.slothiesmooth.nyx.client.data.sqldelight.StegoImageQueries
import com.slothiesmooth.nyx.client.data.sqldelight.Stego_image
import com.slothiesmooth.nyx.shared.data.source.StegoImageRecord
import com.slothiesmooth.nyx.shared.data.source.VaultSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

private const val ARCHIVED_TRUE = 1L
private const val ARCHIVED_FALSE = 0L

/**
 * [VaultSource] over SqlDelight. Reads observe the DB reactively; writes are suspend. [ioContext]
 * is the context query-mapping runs on (defaults to [Dispatchers.Default]; tests inject a test
 * dispatcher).
 */
class VaultSqlSource(
    private val source: SqlDelightSource,
    private val ioContext: CoroutineContext = Dispatchers.Default,
) : VaultSource {

    private suspend fun queries(): StegoImageQueries = database().stegoImageQueries

    private suspend fun database(): NyxDb = source.database.first()

    override fun observeActive(): Flow<List<StegoImageRecord>> =
        source.database.flatMapLatest { db ->
            db.stegoImageQueries.selectActive().asFlow().mapToList(ioContext)
        }.map { rows -> rows.map { it.toRecord() } }

    override fun observeArchived(): Flow<List<StegoImageRecord>> =
        source.database.flatMapLatest { db ->
            db.stegoImageQueries.selectArchived().asFlow().mapToList(ioContext)
        }.map { rows -> rows.map { it.toRecord() } }

    override suspend fun getById(id: String): StegoImageRecord? =
        queries().selectById(id).awaitAsOneOrNull()?.toRecord()

    override suspend fun upsert(record: StegoImageRecord) {
        queries().upsert(
            id = record.id,
            name = record.name,
            created_at = record.createdAt,
            updated_at = record.updatedAt,
            deleted_at = record.deletedAt,
            is_archived = if (record.isArchived) ARCHIVED_TRUE else ARCHIVED_FALSE,
        )
    }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: String) {
        queries().setArchived(
            is_archived = if (archived) ARCHIVED_TRUE else ARCHIVED_FALSE,
            updated_at = updatedAt,
            id = id,
        )
    }

    override suspend fun softDelete(id: String, deletedAt: String) {
        queries().softDelete(deleted_at = deletedAt, id = id)
    }

    override suspend fun purgeAll() {
        queries().purgeAll()
    }

    override suspend fun countActive(): Int = queries().countActive().awaitAsOne().toInt()
}

private fun Stego_image.toRecord(): StegoImageRecord = StegoImageRecord(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
    deletedAt = deleted_at,
    isArchived = is_archived != ARCHIVED_FALSE,
)

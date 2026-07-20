package com.slothiesmooth.nyx.client.data.source.database.sqldelight

import app.cash.sqldelight.db.SqlDriver
import com.slothiesmooth.nyx.client.data.sqldelight.NyxDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn

private const val REPLAY_LATEST = 1

/**
 * Lazily builds the single [NyxDb] over [driver] and shares it as a hot flow (replay 1) so every
 * source/repository observes the same database instance. The build runs `PRAGMA foreign_keys = ON`
 * once before emitting. [scope] is the outer application scope (injected via DI).
 */
class SqlDelightSource(
    private val driver: SqlDriver,
    scope: CoroutineScope,
) {
    val database: SharedFlow<NyxDb> = flow {
        driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON;", parameters = 0).await()
        emit(NyxDb(driver))
    }.shareIn(scope, SharingStarted.Lazily, replay = REPLAY_LATEST)
}

package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * wasmJs actual: there is no SqlDelight driver on wasm in Nyx v1 (the web vault is in-memory), so
 * any attempt to build a test DB on wasm is a programming error. No wasm test uses this.
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver =
    throw NotImplementedError("No SqlDelight driver on wasmJs in Nyx v1; the web vault is in-memory.")

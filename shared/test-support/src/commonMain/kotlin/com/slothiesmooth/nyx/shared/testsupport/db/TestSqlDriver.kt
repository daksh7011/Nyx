package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * Creates a fresh in-memory SQL driver with [schema] already created, for isolated tests. Callers
 * pass a synchronous schema, e.g. `NyxDb.Schema.synchronous()` (the database is generated with
 * `generateAsync = true`, so its `Schema` is async until adapted). Not available on wasmJs in v1.
 */
expect fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver

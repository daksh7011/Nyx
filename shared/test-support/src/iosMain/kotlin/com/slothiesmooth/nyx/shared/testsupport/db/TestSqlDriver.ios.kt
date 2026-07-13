package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS actual: an in-memory Native SQLite driver (the driver creates the schema itself).
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver =
    NativeSqliteDriver(
        schema = schema,
        name = "nyx-test.db",
        onConfiguration = { config -> config.copy(inMemory = true) },
    )

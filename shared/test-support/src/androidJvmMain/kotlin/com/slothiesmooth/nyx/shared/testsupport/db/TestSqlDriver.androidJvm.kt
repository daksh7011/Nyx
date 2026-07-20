package com.slothiesmooth.nyx.shared.testsupport.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * jvm + android-host actual: an in-memory JDBC SQLite driver with the schema created synchronously.
 */
actual fun createTestSqlDriver(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    schema.create(driver)
    return driver
}

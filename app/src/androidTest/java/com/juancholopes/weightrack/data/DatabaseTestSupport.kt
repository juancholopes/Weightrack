package com.juancholopes.weightrack.data

import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue

/** A fresh in-memory database with the production driver (S20-R46). */
fun newInMemoryDatabase(): WeightrackDatabase =
    Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        WeightrackDatabase::class.java,
    )
        .setDriver(AndroidSQLiteDriver())
        .build()

/** Runs a raw read and returns every row as column name to value. */
suspend fun WeightrackDatabase.rawRows(sql: String): List<Map<String, Any?>> =
    useReaderConnection { connection ->
        connection.usePrepared(sql) { statement -> statement.readAll() }
    }

/** Runs a raw write statement, outside of any DAO. */
suspend fun WeightrackDatabase.rawExec(sql: String) {
    useWriterConnection { connection ->
        connection.usePrepared(sql) { statement -> statement.step() }
    }
}

suspend fun WeightrackDatabase.countRows(table: String): Long =
    rawRows("SELECT COUNT(*) AS n FROM $table").single()["n"] as Long

fun SQLiteStatement.readAll(): List<Map<String, Any?>> {
    val rows = mutableListOf<Map<String, Any?>>()
    while (step()) {
        rows += (0 until getColumnCount()).associate { index ->
            getColumnName(index) to when {
                isNull(index) -> null
                getColumnType(index) == androidx.sqlite.SQLITE_DATA_INTEGER -> getLong(index)
                else -> getText(index)
            }
        }
    }
    return rows
}

/** Asserts that [block] is rejected by SQLite (constraint, foreign key or unique index). */
suspend fun assertSqliteRejects(block: suspend () -> Unit) {
    val error = runCatching { block() }.exceptionOrNull()
    assertTrue("Expected an SQLiteException, got $error", error is SQLiteException)
}

package com.juancholopes.weightrack.data

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WeightrackMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val dbFile = context.getDatabasePath(TEST_DB)

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = dbFile,
        driver = AndroidSQLiteDriver(),
        databaseClass = WeightrackDatabase::class,
    )

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    // S20-A36 / S20-R45
    @Test
    fun version1_createdFromExportedSchema_opensWithRoom() = runTest {
        helper.createDatabase(1).close()

        val db = Room.databaseBuilder(context, WeightrackDatabase::class.java, dbFile.absolutePath)
            .setDriver(AndroidSQLiteDriver())
            .build()
        try {
            assertEquals(0L, db.countRows("exercise"))
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}

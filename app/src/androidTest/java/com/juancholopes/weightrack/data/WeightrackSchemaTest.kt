package com.juancholopes.weightrack.data

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WeightrackSchemaTest {

    private lateinit var db: WeightrackDatabase

    @Before
    fun setUp() {
        db = newInMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private data class Column(val name: String, val type: String, val notNull: Boolean, val pk: Int)

    private suspend fun columnsOf(table: String) = db.rawRows("PRAGMA table_info($table)").map {
        Column(it["name"] as String, it["type"] as String, it["notnull"] == 1L, (it["pk"] as Long).toInt())
    }

    private suspend fun userTables() = db.rawRows("SELECT name FROM sqlite_master WHERE type = 'table'")
        .map { it["name"] as String }
        .filterNot { it == "room_master_table" || it == "android_metadata" || it.startsWith("sqlite_") }
        .toSet()

    // S20-A06 / S20-R19
    @Test
    fun foreignKeys_areEnabled() = runTest {
        assertEquals(1L, db.rawRows("PRAGMA foreign_keys").single()["foreign_keys"])
    }

    // S20-A07 / S20-R09, S20-R10, S20-R17, S20-R18
    @Test
    fun tables_matchSpec() = runTest {
        assertEquals(setOf("exercise", "exercise_muscle", "workout_session", "exercise_set"), userTables())
        assertEquals(
            listOf(
                Column("id", "INTEGER", true, 1),
                Column("name", "TEXT", true, 0),
                Column("normalized_name", "TEXT", true, 0),
                Column("equipment", "TEXT", true, 0),
                Column("notes", "TEXT", false, 0),
                Column("archived", "INTEGER", true, 0),
            ),
            columnsOf("exercise"),
        )
        assertEquals(
            listOf(
                Column("exercise_id", "INTEGER", true, 1),
                Column("muscle", "TEXT", true, 2),
                Column("is_primary", "INTEGER", true, 0),
            ),
            columnsOf("exercise_muscle"),
        )
        assertEquals(
            listOf(
                Column("id", "INTEGER", true, 1),
                Column("date", "TEXT", true, 0),
                Column("notes", "TEXT", false, 0),
            ),
            columnsOf("workout_session"),
        )
        assertEquals(
            listOf(
                Column("id", "INTEGER", true, 1),
                Column("session_id", "INTEGER", true, 0),
                Column("exercise_id", "INTEGER", true, 0),
                Column("set_order", "INTEGER", true, 0),
                Column("weight_grams", "INTEGER", true, 0),
                Column("reps", "INTEGER", true, 0),
            ),
            columnsOf("exercise_set"),
        )
        listOf("exercise", "workout_session", "exercise_set").forEach { table ->
            val sql = db.rawRows("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '$table'")
                .single()["sql"] as String
            assertTrue("$table must use AUTOINCREMENT: $sql", sql.contains("AUTOINCREMENT"))
        }
    }

    private data class IndexSpec(val name: String, val columns: List<String>, val unique: Boolean)

    private suspend fun indexesOf(table: String) = db.rawRows("PRAGMA index_list($table)").map { index ->
        val name = index["name"] as String
        val columns = db.rawRows("PRAGMA index_info('$name')")
            .sortedBy { it["seqno"] as Long }
            .map { it["name"] as String }
        IndexSpec(if (index["origin"] == "pk") "pk" else name, columns, index["unique"] == 1L)
    }.toSet()

    // S20-A08 / S20-R24 to S20-R27
    @Test
    fun indexes_matchSpec() = runTest {
        assertEquals(
            setOf(
                IndexSpec("index_exercise_normalized_name", listOf("normalized_name"), true),
                IndexSpec("index_exercise_archived_equipment", listOf("archived", "equipment"), false),
            ),
            indexesOf("exercise"),
        )
        assertEquals(
            setOf(
                IndexSpec("pk", listOf("exercise_id", "muscle"), true),
                IndexSpec("index_exercise_muscle_muscle_exercise_id", listOf("muscle", "exercise_id"), false),
            ),
            indexesOf("exercise_muscle"),
        )
        assertEquals(
            setOf(IndexSpec("index_workout_session_date_id", listOf("date", "id"), false)),
            indexesOf("workout_session"),
        )
        assertEquals(
            setOf(
                IndexSpec("index_exercise_set_session_id_set_order", listOf("session_id", "set_order"), true),
                IndexSpec("index_exercise_set_exercise_id_session_id", listOf("exercise_id", "session_id"), false),
            ),
            indexesOf("exercise_set"),
        )
    }

    private data class ForeignKeySpec(val from: String, val table: String, val to: String, val onDelete: String, val onUpdate: String)

    private suspend fun foreignKeysOf(table: String) = db.rawRows("PRAGMA foreign_key_list($table)").map {
        ForeignKeySpec(
            it["from"] as String, it["table"] as String, it["to"] as String,
            it["on_delete"] as String, it["on_update"] as String,
        )
    }.toSet()

    // S20-A08 / S20-R20 to S20-R23
    @Test
    fun foreignKeys_matchSpec() = runTest {
        assertEquals(
            setOf(ForeignKeySpec("exercise_id", "exercise", "id", "CASCADE", "RESTRICT")),
            foreignKeysOf("exercise_muscle"),
        )
        assertEquals(
            setOf(
                ForeignKeySpec("session_id", "workout_session", "id", "CASCADE", "RESTRICT"),
                ForeignKeySpec("exercise_id", "exercise", "id", "RESTRICT", "RESTRICT"),
            ),
            foreignKeysOf("exercise_set"),
        )
        assertEquals(emptySet<ForeignKeySpec>(), foreignKeysOf("exercise"))
        assertEquals(emptySet<ForeignKeySpec>(), foreignKeysOf("workout_session"))
        val deferred = db.rawRows("SELECT sql FROM sqlite_master WHERE type = 'table'")
            .mapNotNull { it["sql"] as String? }
            .filter { it.contains("DEFERRABLE", ignoreCase = true) }
        assertEquals(emptyList<String>(), deferred)
    }

    // S20-A09 / S20-R09
    @Test
    fun freshDatabase_hasNoMuscleTable_andNoRows() = runTest {
        val tables = userTables()
        assertTrue(tables.none { it == "muscle" || it == "equipment" })
        tables.forEach { assertEquals("rows in $it", 0L, db.countRows(it)) }
    }

    // S20-A18 / S20-R23
    @Test
    fun updatingReferencedId_isRestricted() = runTest {
        db.rawExec(
            "INSERT INTO exercise (name, normalized_name, equipment, notes, archived) " +
                "VALUES ('Press de banca', 'press de banca', 'BARBELL', NULL, 0)",
        )
        db.rawExec("INSERT INTO workout_session (date, notes) VALUES ('2026-10-10', NULL)")
        db.rawExec(
            "INSERT INTO exercise_set (session_id, exercise_id, set_order, weight_grams, reps) VALUES (1, 1, 1, 60000, 10)",
        )

        assertSqliteRejects { db.rawExec("UPDATE workout_session SET id = 99 WHERE id = 1") }
        assertSqliteRejects { db.rawExec("UPDATE exercise SET id = 99 WHERE id = 1") }
        assertEquals(1L, db.rawRows("SELECT id FROM workout_session").single()["id"])
        assertEquals(1L, db.rawRows("SELECT id FROM exercise").single()["id"])
    }
}

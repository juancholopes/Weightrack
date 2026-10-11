package com.juancholopes.weightrack.feature.catalog.data

import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.data.assertSqliteRejects
import com.juancholopes.weightrack.data.countRows
import com.juancholopes.weightrack.data.newInMemoryDatabase
import com.juancholopes.weightrack.data.rawExec
import com.juancholopes.weightrack.data.rawRows
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.data.ExerciseSetEntity
import com.juancholopes.weightrack.feature.session.data.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExerciseDaoTest {

    private lateinit var db: WeightrackDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = newInMemoryDatabase()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun exercise(name: String, equipment: Equipment = Equipment.BARBELL, archived: Boolean = false) =
        Exercise(id = 0, name = name, equipment = equipment, notes = null, archived = archived).toEntity()

    private fun muscle(muscle: Muscle, isPrimary: Boolean = true) =
        ExerciseMuscleEntity(exerciseId = 0, muscle = muscle, isPrimary = isPrimary)

    private suspend fun insert(name: String, equipment: Equipment = Equipment.BARBELL, archived: Boolean = false, vararg muscles: ExerciseMuscleEntity) =
        dao.insertWithMuscles(exercise(name, equipment, archived), muscles.toList())

    private suspend fun addSetFor(exerciseId: Long) {
        val sessionDao = db.sessionDao()
        sessionDao.insertSessionWithSets(
            WorkoutSessionEntity(id = 0, date = LocalDate.of(2026, 10, 10), notes = null),
            listOf(ExerciseSetEntity(id = 0, sessionId = 0, exerciseId = exerciseId, order = 1, weightGrams = 60_000, reps = 10)),
        )
    }

    // S20-A11 / S20-R13, S20-R14
    @Test
    fun storesEnumsAsNameText_andBooleansAsInteger() = runTest {
        insert("Press de banca", Equipment.BARBELL, archived = true, muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, isPrimary = false))

        val row = db.rawRows("SELECT equipment, typeof(equipment) AS te, archived, typeof(archived) AS ta FROM exercise").single()
        assertEquals("BARBELL", row["equipment"])
        assertEquals("text", row["te"])
        assertEquals(1L, row["archived"])
        assertEquals("integer", row["ta"])

        val muscles = db.rawRows(
            "SELECT muscle, typeof(muscle) AS tm, is_primary, typeof(is_primary) AS tp FROM exercise_muscle ORDER BY muscle",
        )
        assertEquals(listOf("CHEST", "TRICEPS"), muscles.map { it["muscle"] })
        assertEquals(listOf(1L, 0L), muscles.map { it["is_primary"] })
        muscles.forEach {
            assertEquals("text", it["tm"])
            assertEquals("integer", it["tp"])
        }
    }

    // S20-A16 / S20-R21, S20-R36
    @Test
    fun deleteIfHasNoSets_withoutSets_deletesExerciseAndMuscles() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, false))

        assertEquals(1, dao.deleteIfHasNoSets(id))
        assertEquals(0L, db.countRows("exercise"))
        assertEquals(0L, db.countRows("exercise_muscle"))
    }

    // S20-A17 / S20-R22
    @Test
    fun hardDelete_ofExerciseWithSets_isBlockedByRestrict() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))
        addSetFor(id)

        assertSqliteRejects { db.rawExec("DELETE FROM exercise WHERE id = $id") }
        assertEquals(1L, db.countRows("exercise"))
        assertEquals(1L, db.countRows("exercise_muscle"))
        assertEquals(1L, db.countRows("exercise_set"))
    }

    // S20-A17 / S20-R36
    @Test
    fun deleteIfHasNoSets_withSets_returnsZero() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))
        addSetFor(id)

        assertEquals(0, dao.deleteIfHasNoSets(id))
        assertEquals(0, dao.deleteIfHasNoSets(id + 100))
        assertEquals(1L, db.countRows("exercise"))
        assertEquals(1L, db.countRows("exercise_muscle"))
    }

    // S20-A19 / S20-R24, S20-R12, S10-R05
    @Test
    fun normalizedNameCollision_isRejected_evenWhenArchived() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))

        assertSqliteRejects { dao.insertExercise(exercise("press  DE banca")) }
        dao.setArchived(id, true)
        assertSqliteRejects { dao.insertExercise(exercise("press  DE banca")) }
        assertEquals(1L, db.countRows("exercise"))
    }

    // S20-A19 / S10-R05: accents count
    @Test
    fun accentedName_isDistinct() = runTest {
        insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))
        insert("Prés de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))

        assertEquals(2L, db.countRows("exercise"))
    }

    // S20-A21 / S20-R25
    @Test
    fun duplicateMuscle_isRejected() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))

        assertSqliteRejects { dao.insertMuscles(listOf(ExerciseMuscleEntity(id, Muscle.CHEST, isPrimary = false))) }
        assertEquals(1L, db.countRows("exercise_muscle"))
    }

    // S20-A29 / S20-R33
    @Test
    fun activeCatalog_filtersAndOrders() = runTest {
        insert("press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, false))
        insert("Aperturas", Equipment.DUMBBELL, false, muscle(Muscle.CHEST))
        insert("curl", Equipment.DUMBBELL, false, muscle(Muscle.BICEPS))
        insert("Fondos", Equipment.BODYWEIGHT, true, muscle(Muscle.CHEST))

        suspend fun names(muscle: Muscle? = null, equipment: Equipment? = null) =
            dao.observeActive(muscle, equipment).first().map { it.name }

        assertEquals(listOf("Aperturas", "curl", "press de banca"), names())
        assertEquals(listOf("Aperturas", "press de banca"), names(muscle = Muscle.CHEST))
        assertEquals(listOf("press de banca"), names(muscle = Muscle.TRICEPS))
        assertEquals(listOf("Aperturas", "curl"), names(equipment = Equipment.DUMBBELL))
        assertEquals(listOf("Aperturas"), names(Muscle.CHEST, Equipment.DUMBBELL))
        assertEquals(emptyList<String>(), names(equipment = Equipment.BODYWEIGHT))
    }

    // S20-A29 / S20-R34
    @Test
    fun archived_listedByName() = runTest {
        insert("Zancadas", Equipment.BODYWEIGHT, true, muscle(Muscle.QUADRICEPS))
        insert("banco", Equipment.BARBELL, true, muscle(Muscle.CHEST))
        insert("Activo", Equipment.BARBELL, false, muscle(Muscle.CHEST))

        assertEquals(listOf("banco", "Zancadas"), dao.observeArchived().first().map { it.name })
    }

    // S20-A30 / S20-R35
    @Test
    fun setArchived_togglesOnlyArchived() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, false))
        addSetFor(id)
        val before = dao.observeExercise(id).first()!!
        val musclesBefore = dao.observeMuscles(id).first()

        assertEquals(1, dao.setArchived(id, true))
        assertEquals(before.copy(archived = true), dao.observeExercise(id).first())
        assertEquals(musclesBefore, dao.observeMuscles(id).first())
        assertEquals(1L, db.countRows("exercise_set"))

        assertEquals(1, dao.setArchived(id, false))
        assertEquals(before, dao.observeExercise(id).first())
        assertEquals(0, dao.setArchived(id + 100, true))
    }

    // S20-A32 / S20-R38
    @Test
    fun insertWithMuscles_whenMuscleFails_rollsBack() = runTest {
        assertSqliteRejects {
            dao.insertWithMuscles(exercise("Press de banca"), listOf(muscle(Muscle.CHEST), muscle(Muscle.CHEST, false)))
        }
        assertEquals(0L, db.countRows("exercise"))
        assertEquals(0L, db.countRows("exercise_muscle"))
    }

    // S20-R38: editing replaces the muscles and keeps the id
    @Test
    fun updateWithMuscles_replacesMuscles_andKeepsId() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, false))
        val renamed = exercise("Press inclinado").copy(id = id)

        dao.updateWithMuscles(renamed, listOf(ExerciseMuscleEntity(id, Muscle.SHOULDERS, isPrimary = true)))

        assertEquals(renamed, dao.observeExercise(id).first())
        assertEquals(listOf(ExerciseMuscleEntity(id, Muscle.SHOULDERS, true)), dao.observeMuscles(id).first())
    }

    // S20-R38: a failing edit leaves nothing written
    @Test
    fun updateWithMuscles_whenMuscleFails_rollsBack() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))
        val original = dao.observeExercise(id).first()

        assertSqliteRejects {
            dao.updateWithMuscles(
                exercise("Press inclinado").copy(id = id),
                listOf(ExerciseMuscleEntity(id, Muscle.SHOULDERS, true), ExerciseMuscleEntity(id, Muscle.SHOULDERS, false)),
            )
        }
        assertEquals(original, dao.observeExercise(id).first())
        assertEquals(listOf(ExerciseMuscleEntity(id, Muscle.CHEST, true)), dao.observeMuscles(id).first())
    }

    // S20-R37, S20-R41: reads used inside transactions
    @Test
    fun countSetsAndFindByNormalizedName() = runTest {
        val id = insert("Press de banca", Equipment.BARBELL, false, muscle(Muscle.CHEST))
        assertEquals(0, dao.countSetsOfExercise(id))
        addSetFor(id)
        assertEquals(1, dao.countSetsOfExercise(id))
        assertEquals(id, dao.findByNormalizedName("press de banca")?.id)
        assertNull(dao.findByNormalizedName("press"))
    }
}

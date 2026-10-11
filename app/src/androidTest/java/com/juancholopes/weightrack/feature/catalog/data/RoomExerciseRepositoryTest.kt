package com.juancholopes.weightrack.feature.catalog.data

import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.data.countRows
import com.juancholopes.weightrack.data.newInMemoryDatabase
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.data.RoomSessionRepository
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class RoomExerciseRepositoryTest {

    private lateinit var db: WeightrackDatabase
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        db = newInMemoryDatabase()
        repository = RoomExerciseRepository(db, db.exerciseDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val chest = ExerciseMuscle(exerciseId = 0, muscle = Muscle.CHEST, isPrimary = true)
    private val triceps = ExerciseMuscle(exerciseId = 0, muscle = Muscle.TRICEPS, isPrimary = false)

    private fun exercise(name: String, equipment: Equipment = Equipment.BARBELL) =
        Exercise(id = 0, name = name, equipment = equipment, notes = null)

    private suspend fun create(name: String, vararg muscles: ExerciseMuscle = arrayOf(chest)): Long =
        (repository.create(exercise(name), muscles.toList()) as ExerciseWriteResult.Saved).id

    private suspend fun addSetFor(exerciseId: Long) {
        RoomSessionRepository(db.sessionDao()).createSession(
            WorkoutSession(id = 0, date = LocalDate.of(2026, 10, 10), notes = null),
            listOf(ExerciseSet(id = 0, sessionId = 0, exerciseId = exerciseId, order = 1, weightGrams = 60_000, reps = 10)),
        )
    }

    // S20-R17, S20-R38: unsaved ids (0) are replaced by the id the database assigns
    @Test
    fun create_savesExerciseWithMuscles() = runTest {
        val id = create("Press de banca", chest, triceps)

        assertEquals(exercise("Press de banca").copy(id = id), repository.observeExercise(id).first())
        assertEquals(
            listOf(chest.copy(exerciseId = id), triceps.copy(exerciseId = id)).toSet(),
            repository.observeMuscles(id).first().toSet(),
        )
    }

    // S20-A20 / S20-R41, S10-R21
    @Test
    fun create_withNameOfArchived_returnsNameTakenByArchived() = runTest {
        val id = create("Press de banca")
        db.exerciseDao().setArchived(id, true)

        val result = repository.create(exercise("press  DE banca"), listOf(chest))

        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN_BY_ARCHIVED), result)
        assertEquals(1L, db.countRows("exercise"))
    }

    // S20-A20 / S20-R41
    @Test
    fun create_withNameOfActive_returnsNameTaken() = runTest {
        create("Press de banca")

        val result = repository.create(exercise("PRESS DE BANCA"), listOf(chest))

        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN), result)
        assertEquals(1L, db.countRows("exercise"))
        assertEquals(1L, db.countRows("exercise_muscle"))
    }

    // S20-R38, S20-R41: editing keeps the id, replaces muscles and reports collisions
    @Test
    fun update_replacesMuscles_andReportsCollision() = runTest {
        val bench = create("Press de banca", chest, triceps)
        create("Sentadilla")

        val renamed = exercise("Press inclinado").copy(id = bench)
        assertEquals(ExerciseWriteResult.Saved(bench), repository.update(renamed, listOf(chest)))
        assertEquals(renamed, repository.observeExercise(bench).first())
        assertEquals(listOf(chest.copy(exerciseId = bench)), repository.observeMuscles(bench).first())

        val collision = repository.update(exercise("sentadilla").copy(id = bench), listOf(chest))
        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN), collision)
        assertEquals(renamed, repository.observeExercise(bench).first())
    }

    // S20-A31 / S20-R40, S10-R17
    @Test
    fun delete_withSets_archives() = runTest {
        val id = create("Press de banca", chest, triceps)
        addSetFor(id)

        val result = repository.delete(id)

        val archived = exercise("Press de banca").copy(id = id, archived = true)
        assertEquals(ExerciseDeletion.Archive(archived), result)
        assertEquals(archived, repository.observeExercise(id).first())
        assertEquals(2, repository.observeMuscles(id).first().size)
        assertEquals(1L, db.countRows("exercise_set"))
        assertEquals(listOf(archived), repository.observeArchived().first())
    }

    // S20-A31 / S20-R40, S10-R20
    @Test
    fun delete_withoutSets_deletesWithMuscles() = runTest {
        val id = create("Press de banca", chest, triceps)

        val result = repository.delete(id)

        val expectedMuscles = listOf(chest.copy(exerciseId = id), triceps.copy(exerciseId = id))
        val deletion = result as ExerciseDeletion.Delete
        assertEquals(exercise("Press de banca").copy(id = id), deletion.exercise)
        assertEquals(expectedMuscles.toSet(), deletion.muscles.toSet())
        assertNull(repository.observeExercise(id).first())
        assertEquals(0L, db.countRows("exercise_muscle"))
    }

    // S10-R19
    @Test
    fun unarchive_returnsExerciseToActiveCatalog() = runTest {
        val id = create("Press de banca")
        db.exerciseDao().setArchived(id, true)
        assertEquals(emptyList<Exercise>(), repository.observeActiveCatalog().first())

        repository.unarchive(id)

        assertEquals(listOf(exercise("Press de banca").copy(id = id)), repository.observeActiveCatalog().first())
        assertEquals(emptyList<Exercise>(), repository.observeArchived().first())
    }

    // S20-R33: filters reach the DAO
    @Test
    fun observeActiveCatalog_appliesFilters() = runTest {
        create("Press de banca", chest, triceps)
        repository.create(exercise("Curl", Equipment.DUMBBELL), listOf(chest.copy(muscle = Muscle.BICEPS)))

        assertEquals(listOf("Press de banca"), repository.observeActiveCatalog(muscle = Muscle.TRICEPS).first().map { it.name })
        assertEquals(listOf("Curl"), repository.observeActiveCatalog(equipment = Equipment.DUMBBELL).first().map { it.name })
    }
}

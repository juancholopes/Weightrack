package com.juancholopes.weightrack.feature.session.data

import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.data.countRows
import com.juancholopes.weightrack.data.newInMemoryDatabase
import com.juancholopes.weightrack.feature.catalog.data.RoomExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.ExerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LastPerformance
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class RoomSessionRepositoryTest {

    private lateinit var db: WeightrackDatabase
    private lateinit var repository: SessionRepository
    private var benchId = 0L
    private lateinit var bench: Exercise

    private val oct10: LocalDate = LocalDate.of(2026, 10, 10)

    @Before
    fun setUp() = runTest {
        db = newInMemoryDatabase()
        repository = RoomSessionRepository(db.sessionDao())
        val exercise = Exercise(id = 0, name = "Press de banca", equipment = Equipment.BARBELL, notes = null)
        val saved = RoomExerciseRepository(db, db.exerciseDao())
            .create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true))) as ExerciseWriteResult.Saved
        benchId = saved.id
        bench = exercise.copy(id = benchId)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun set(order: Int, grams: Int = 60_000, reps: Int = 10) =
        ExerciseSet(id = 0, sessionId = 0, exerciseId = benchId, order = order, weightGrams = grams, reps = reps)

    private suspend fun session(date: LocalDate = oct10, vararg sets: ExerciseSet): WorkoutSession {
        val id = repository.createSession(WorkoutSession(id = 0, date = date, notes = null), sets.toList())
        return WorkoutSession(id = id, date = date, notes = null)
    }

    // S20-R17, S20-R39
    @Test
    fun createSession_assignsIdsToSessionAndSets() = runTest {
        val created = session(oct10, set(1), set(2, grams = 62_500))

        assertEquals(created, repository.observeSession(created.id).first())
        val sets = repository.setsOfSession(created.id)
        assertEquals(listOf(created.id, created.id), sets.map { it.sessionId })
        assertEquals(listOf(60_000, 62_500), sets.map { it.weightGrams })
        assertEquals(listOf(listOf(created)), listOf(repository.observeHistory().first()))
    }

    // S20-R29, S10-R25, S10-R26
    @Test
    fun observeLastPerformance_mapsToDomain() = runTest {
        assertNull(repository.observeLastPerformance(benchId).first())
        val previous = session(oct10.minusDays(1), set(2), set(1))
        val current = session(oct10, set(1))

        val last = repository.observeLastPerformance(benchId).first()
        assertEquals(current, last?.session)

        val beforeCurrent = repository.observeLastPerformance(benchId, before = current).first()
        val previousSets = repository.setsOfSession(previous.id)
        assertEquals(LastPerformance(previous, previousSets), beforeCurrent)
        assertEquals(listOf(1, 2), beforeCurrent!!.sets.map { it.order })
    }

    // S20-R30, S10-R27
    @Test
    fun observeEvolution_mapsToDomain() = runTest {
        val first = session(oct10.minusDays(1), set(1))
        val second = session(oct10, set(1))
        val firstSet = repository.setsOfSession(first.id).single()
        val secondSet = repository.setsOfSession(second.id).single()

        assertEquals(
            ExerciseEvolution(listOf(ExerciseEvolution.Entry(first.date, firstSet), ExerciseEvolution.Entry(second.date, secondSet))),
            repository.observeEvolution(benchId).first(),
        )
    }

    // S20-R32, S10-R29
    @Test
    fun observeSessionSets_pairsEachSetWithItsExercise() = runTest {
        val created = session(oct10, set(2), set(1))

        val pairs = repository.observeSessionSets(created.id).first()

        assertEquals(listOf(1, 2), pairs.map { it.first.order })
        assertEquals(listOf(bench, bench), pairs.map { it.second })
    }

    // S10-R23, S10-R24: set and session edits
    @Test
    fun addUpdateAndDeleteSets_thenDeleteSession() = runTest {
        val created = session(oct10)
        val updatedSession = created.copy(notes = "Me sentí cansado")
        repository.updateSession(updatedSession)
        assertEquals(updatedSession, repository.observeSession(created.id).first())

        val setId = repository.addSet(set(1).copy(sessionId = created.id))
        val stored = repository.setsOfSession(created.id).single()
        assertEquals(setId, stored.id)

        repository.updateSet(stored.copy(weightGrams = 65_000, reps = 8))
        assertEquals(stored.copy(weightGrams = 65_000, reps = 8), repository.setsOfSession(created.id).single())

        repository.deleteSet(setId)
        assertEquals(emptyList<ExerciseSet>(), repository.setsOfSession(created.id))
        assertEquals(updatedSession, repository.observeSession(created.id).first())

        repository.addSet(set(1).copy(sessionId = created.id))
        repository.deleteSession(created.id)
        assertNull(repository.observeSession(created.id).first())
        assertEquals(0L, db.countRows("exercise_set"))
    }
}

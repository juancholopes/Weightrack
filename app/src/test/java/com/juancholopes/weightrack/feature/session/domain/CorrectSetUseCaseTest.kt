package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class CorrectSetUseCaseTest {

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)
    private val correctSet = CorrectSetUseCase(exercises, sessions)

    private val oct11 = LocalDate.of(2026, 10, 11)

    private suspend fun newExercise(archived: Boolean = false): Long {
        val exercise = Exercise(0, "Press de banca", Equipment.BARBELL, notes = null, archived = archived)
        val result = exercises.create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true)))
        return (result as ExerciseWriteResult.Saved).id
    }

    /** A session with two sets of [exerciseId]; returns the session id. */
    private suspend fun sessionWithTwoSets(exerciseId: Long): Long = sessions.createSession(
        WorkoutSession(0, oct11, null),
        listOf(
            ExerciseSet(0, 0, exerciseId, order = 1, weightGrams = 60_000, reps = 10),
            ExerciseSet(0, 0, exerciseId, order = 2, weightGrams = 62_500, reps = 8),
        ),
    )

    // SF02-A01 / SF02-R04, S40-A26: the set changes in place: same id, same `order`, same session
    @Test
    fun invoke_changesTheSetInPlace() = runTest {
        val bench = newExercise()
        val sessionId = sessionWithTwoSets(bench)
        val first = sessions.setsOfSession(sessionId).first()

        val result = correctSet(sessionId, first.id, weightGrams = 65_000, reps = 6)

        val corrected = first.copy(weightGrams = 65_000, reps = 6)
        assertEquals(CorrectSetResult.Saved(corrected), result)
        assertEquals(listOf(corrected, sessions.setsOfSession(sessionId)[1]), sessions.setsOfSession(sessionId))
        assertEquals(listOf(65_000, 62_500), sessions.setsOfSession(sessionId).map { it.weightGrams })
    }

    // SF02-A01 / SF02-R04, S40-A24: a rejected value writes nothing
    @Test
    fun invoke_rejectsInvalidValues_andKeepsTheSet() = runTest {
        val bench = newExercise()
        val sessionId = sessionWithTwoSets(bench)
        val before = sessions.setsOfSession(sessionId)
        val setId = before.first().id

        val tooHigh = correctSet(sessionId, setId, weightGrams = 1_000_250, reps = 10)
        val notMultiple = correctSet(sessionId, setId, weightGrams = 62_300, reps = 10)
        val zeroReps = correctSet(sessionId, setId, weightGrams = 60_000, reps = 0)
        val manyReps = correctSet(sessionId, setId, weightGrams = 60_000, reps = 1_000)

        assertEquals(CorrectSetResult.Rejected(ExerciseSetError.WEIGHT_TOO_HIGH), tooHigh)
        assertEquals(CorrectSetResult.Rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), notMultiple)
        assertEquals(CorrectSetResult.Rejected(ExerciseSetError.REPS_NOT_POSITIVE), zeroReps)
        assertEquals(CorrectSetResult.Rejected(ExerciseSetError.REPS_TOO_HIGH), manyReps)
        assertEquals(before, sessions.setsOfSession(sessionId))
    }

    // SF02-A12 / SF02-R16, S40-R29, S10-R11: the sets of an archived exercise can still be corrected
    @Test
    fun invoke_correctsASetOfAnArchivedExercise() = runTest {
        val bench = newExercise()
        val sessionId = sessionWithTwoSets(bench)
        exercises.delete(bench) // it has sets, so it is archived
        val first = sessions.setsOfSession(sessionId).first()

        val result = correctSet(sessionId, first.id, weightGrams = 70_000, reps = 5)

        assertTrue(result is CorrectSetResult.Saved)
        assertEquals(70_000, sessions.setsOfSession(sessionId).first().weightGrams)
    }

    // S30-R23: a set that does not exist is an unexpected failure, not a business rejection
    @Test
    fun invoke_forAMissingSet_throws() = runTest {
        val bench = newExercise()
        val sessionId = sessionWithTwoSets(bench)

        try {
            correctSet(sessionId, setId = 99, weightGrams = 60_000, reps = 10)
            fail("Expected an IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals(2, sessions.setsOfSession(sessionId).size)
        }
    }

    // SF02-A14 / S30-R23: a failing repository write is not swallowed by the use case
    @Test
    fun invoke_whenTheWriteFails_propagatesTheException_andKeepsTheSet() = runTest {
        val bench = newExercise()
        val sessionId = sessionWithTwoSets(bench)
        val first = sessions.setsOfSession(sessionId).first()
        sessions.failNextWrite()

        try {
            correctSet(sessionId, first.id, weightGrams = 65_000, reps = 6)
            fail("Expected the write failure")
        } catch (expected: IllegalStateException) {
            assertEquals(first, sessions.setsOfSession(sessionId).first())
        }
        assertTrue(correctSet(sessionId, first.id, weightGrams = 65_000, reps = 6) is CorrectSetResult.Saved)
    }
}

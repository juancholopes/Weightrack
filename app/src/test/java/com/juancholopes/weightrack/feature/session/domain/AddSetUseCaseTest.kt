package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddSetUseCaseTest {

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    private val oct11 = LocalDate.of(2026, 10, 11)
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC)
    private val addSet = AddSetUseCase(exercises, sessions, clock)

    private suspend fun newExercise(name: String = "Press de banca", archived: Boolean = false): Long {
        val exercise = Exercise(0, name, Equipment.BARBELL, notes = null, archived = archived)
        val result = exercises.create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true)))
        return (result as ExerciseWriteResult.Saved).id
    }

    private suspend fun allSessions() = sessions.observeHistory().first()

    // SF02-A01 / SF02-R05, S20-R39: without a session, the first set creates it with the date it was given
    @Test
    fun invoke_withoutSession_createsASingleSessionWithTheGivenDateAndTheSet() = runTest {
        val bench = newExercise()

        val result = addSet(sessionId = null, date = oct11, exerciseId = bench, weightGrams = 62_500, reps = 8)

        val session = allSessions().single()
        assertEquals(AddSetResult.Saved(session.id), result)
        assertEquals(WorkoutSession(session.id, oct11, null), session)
        val stored = sessions.setsOfSession(session.id).single()
        assertEquals(ExerciseSet(stored.id, session.id, bench, order = 1, weightGrams = 62_500, reps = 8), stored)
    }

    // SF02-A01 / SF02-R05, S10-R09: with a session, the set gets the next `order`, gaps allowed
    @Test
    fun invoke_withSession_addsTheSetWithTheNextOrder() = runTest {
        val bench = newExercise()
        val sessionId = sessions.createSession(
            WorkoutSession(0, oct11, null),
            listOf(
                ExerciseSet(0, 0, bench, order = 1, weightGrams = 60_000, reps = 10),
                ExerciseSet(0, 0, bench, order = 3, weightGrams = 60_000, reps = 10),
            ),
        )

        val result = addSet(sessionId = sessionId, date = oct11, exerciseId = bench, weightGrams = 65_000, reps = 6)

        assertEquals(AddSetResult.Saved(sessionId), result)
        val stored = sessions.setsOfSession(sessionId)
        assertEquals(listOf(1, 3, 4), stored.map { it.order })
        assertEquals(65_000, stored.last().weightGrams)
        assertEquals(1, allSessions().size)
    }

    // SF02-A01 / SF02-R04, S40-A24: 0 g and 1 000 000 g, 1 and 999 repetitions are accepted
    @Test
    fun invoke_acceptsTheLimits() = runTest {
        val bench = newExercise()

        assertTrue(addSet(null, oct11, bench, weightGrams = 0, reps = 1) is AddSetResult.Saved)
        val sessionId = allSessions().single().id
        assertTrue(addSet(sessionId, oct11, bench, weightGrams = 1_000_000, reps = 999) is AddSetResult.Saved)

        assertEquals(listOf(0 to 1, 1_000_000 to 999), sessions.setsOfSession(sessionId).map { it.weightGrams to it.reps })
    }

    // SF02-A01 / SF02-R04, S40-A24: a rejected value writes nothing and is never rounded
    @Test
    fun invoke_rejectsInvalidValues_andWritesNothing() = runTest {
        val bench = newExercise()
        val cases = listOf(
            Triple(62_300, 10, ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP),
            Triple(1_000_250, 10, ExerciseSetError.WEIGHT_TOO_HIGH),
            Triple(-250, 10, ExerciseSetError.WEIGHT_NEGATIVE),
            Triple(60_000, 0, ExerciseSetError.REPS_NOT_POSITIVE),
            Triple(60_000, 1_000, ExerciseSetError.REPS_TOO_HIGH),
        )

        for ((weight, reps, error) in cases) {
            val result = addSet(sessionId = null, date = oct11, exerciseId = bench, weightGrams = weight, reps = reps)
            assertEquals("$weight g, $reps reps", AddSetResult.RejectedSet(error), result)
        }

        assertEquals(emptyList<WorkoutSession>(), allSessions())
    }

    // SF02-A01 / SF02-R04: the same rules apply when the session already exists
    @Test
    fun invoke_withSession_rejectsInvalidValues_andWritesNothing() = runTest {
        val bench = newExercise()
        val sessionId = sessions.createSession(WorkoutSession(0, oct11, null))

        val result = addSet(sessionId, oct11, bench, weightGrams = 62_300, reps = 10)

        assertEquals(AddSetResult.RejectedSet(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP), result)
        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(sessionId))
    }

    // SF02-A01 / SF02-R04, S10-R11: an archived exercise gets no new sets, with or without a session
    @Test
    fun invoke_forAnArchivedExercise_isRejected_andWritesNothing() = runTest {
        val bench = newExercise(archived = true)
        val sessionId = sessions.createSession(WorkoutSession(0, oct11, null))

        val withoutSession = addSet(null, oct11, bench, weightGrams = 60_000, reps = 10)
        val withSession = addSet(sessionId, oct11, bench, weightGrams = 60_000, reps = 10)

        assertEquals(AddSetResult.RejectedSet(ExerciseSetError.EXERCISE_ARCHIVED), withoutSession)
        assertEquals(AddSetResult.RejectedSet(ExerciseSetError.EXERCISE_ARCHIVED), withSession)
        assertEquals(listOf(sessionId), allSessions().map { it.id })
        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(sessionId))
    }

    // SF02-A07 / SF02-R05, S10-R32: a session cannot be created in the future, and nothing is written
    @Test
    fun invoke_withoutSession_forTomorrow_isRejected_andWritesNothing() = runTest {
        val bench = newExercise()

        val result = addSet(null, oct11.plusDays(1), bench, weightGrams = 60_000, reps = 10)

        assertEquals(AddSetResult.RejectedSession(WorkoutSessionError.DATE_IN_FUTURE), result)
        assertEquals(emptyList<WorkoutSession>(), allSessions())
    }

    // SF02-A07 / SF02-R12: the clock is already on the next day, but the set goes to the date it was opened on
    @Test
    fun invoke_withoutSession_afterMidnight_createsTheSessionWithTheOpeningDate() = runTest {
        val bench = newExercise()
        val nextDay = AddSetUseCase(exercises, sessions, Clock.fixed(Instant.parse("2026-10-12T00:30:00Z"), ZoneOffset.UTC))

        val result = nextDay(sessionId = null, date = oct11, exerciseId = bench, weightGrams = 60_000, reps = 10)

        assertTrue(result is AddSetResult.Saved)
        assertEquals(oct11, allSessions().single().date)
    }

    // S30-R23: a missing exercise is not a business rejection but an unexpected failure
    @Test
    fun invoke_forAMissingExercise_throws() = runTest {
        try {
            addSet(null, oct11, exerciseId = 42, weightGrams = 60_000, reps = 10)
            fail("Expected an IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals(emptyList<WorkoutSession>(), allSessions())
        }
    }

    // SF02-A14 / S30-R23: a failing repository write is not swallowed by the use case
    @Test
    fun invoke_whenTheWriteFails_propagatesTheException_andWritesNothing() = runTest {
        val bench = newExercise()
        sessions.failNextWrite()

        try {
            addSet(null, oct11, bench, weightGrams = 60_000, reps = 10)
            fail("Expected the write failure")
        } catch (expected: IllegalStateException) {
            assertEquals(emptyList<WorkoutSession>(), allSessions())
        }
        assertTrue(addSet(null, oct11, bench, weightGrams = 60_000, reps = 10) is AddSetResult.Saved)
    }
}

package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LatestSessionPeak
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract of [SessionRepository] (S20-R42) as Room implements it (Spec 31, S31-R06). Runs against the
 * fake in `src/test` and against Room in `src/androidTest`.
 */
abstract class SessionRepositoryContract : RepositoryContract() {

    private lateinit var bench: Exercise
    private lateinit var squat: Exercise

    @Before
    fun createExercises() = runTest {
        bench = createExercise("Press de banca")
        squat = createExercise("Sentadilla", Equipment.BARBELL, muscle(Muscle.QUADRICEPS))
    }

    // S20-R17, S20-R39
    @Test
    fun createSession_savesTheSessionAndItsSetsUnderNewIds() = runTest {
        val id = sessions.createSession(
            WorkoutSession(id = 0, date = oct10, notes = "Pecho"),
            listOf(set(bench.id, order = 1), set(bench.id, order = 2, weightGrams = 62_500)),
        )

        assertEquals(WorkoutSession(id, oct10, "Pecho"), sessions.observeSession(id).first())
        val stored = sessions.setsOfSession(id)
        assertEquals(listOf(id, id), stored.map { it.sessionId })
        assertEquals(listOf(60_000, 62_500), stored.map { it.weightGrams })
        assertTrue(stored.all { it.id > 0 })
        assertEquals(2, stored.map { it.id }.toSet().size)
    }

    // S20-R17, S10-R14
    @Test
    fun createSession_idsGrowInCreationOrder_evenAfterDeletingTheLatest() = runTest {
        val first = createSession()
        val second = createSession()
        sessions.deleteSession(second.id)

        val third = createSession()

        assertTrue(second.id > first.id)
        assertTrue(third.id > second.id)
    }

    // S20-R22, S20-R39
    @Test
    fun createSession_withSetOfMissingExercise_fails_andWritesNothing() = runTest {
        assertFails { createSession(oct10, set(bench.id, order = 1), set(exerciseId = 42, order = 2)) }

        assertEquals(emptyList<WorkoutSession>(), sessions.observeHistory().first())
    }

    // S20-R26, S20-R39
    @Test
    fun createSession_withRepeatedOrder_fails_andWritesNothing() = runTest {
        assertFails { createSession(oct10, set(bench.id, order = 1), set(squat.id, order = 1)) }

        assertEquals(emptyList<WorkoutSession>(), sessions.observeHistory().first())
    }

    // S20-R17, S20-R37
    @Test
    fun addSet_returnsANewId_andSetsOfSessionComeByOrder() = runTest {
        val session = createSession(oct10, set(bench.id, order = 2))

        val id = sessions.addSet(set(squat.id, order = 1, sessionId = session.id))

        val stored = sessions.setsOfSession(session.id)
        assertEquals(listOf(1, 2), stored.map { it.order })
        assertEquals(id, stored.first().id)
    }

    // S20-R20, S20-R22
    @Test
    fun addSet_toMissingSessionOrExercise_fails() = runTest {
        val session = createSession()

        assertFails { sessions.addSet(set(bench.id, order = 1, sessionId = 42)) }
        assertFails { sessions.addSet(set(exerciseId = 42, order = 1, sessionId = session.id)) }

        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(session.id))
    }

    // S20-R26, S10-R09
    @Test
    fun addSet_withOrderTakenInItsSession_fails_butTheSameOrderInAnotherSessionIsSaved() = runTest {
        val yesterday = createSession(oct10.minusDays(1), set(bench.id, order = 1))
        val today = createSession(oct10)

        assertFails { sessions.addSet(set(squat.id, order = 1, sessionId = yesterday.id)) }
        sessions.addSet(set(squat.id, order = 1, sessionId = today.id))

        assertEquals(listOf(bench.id), sessions.setsOfSession(yesterday.id).map { it.exerciseId })
        assertEquals(listOf(squat.id), sessions.setsOfSession(today.id).map { it.exerciseId })
    }

    @Test
    fun updateSet_changesTheStoredSet() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        val stored = sessions.setsOfSession(session.id).single()
        val corrected = stored.copy(exerciseId = squat.id, weightGrams = 100_000, reps = 5)

        sessions.updateSet(corrected)

        assertEquals(listOf(corrected), sessions.setsOfSession(session.id))
    }

    // S20-R22
    @Test
    fun updateSet_toMissingExercise_fails_andKeepsTheSet() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        val stored = sessions.setsOfSession(session.id).single()

        assertFails { sessions.updateSet(stored.copy(exerciseId = 42)) }

        assertEquals(listOf(stored), sessions.setsOfSession(session.id))
    }

    @Test
    fun updateSession_changesDateAndNotes() = runTest {
        val session = createSession()
        val edited = session.copy(date = oct10.minusDays(2), notes = "Me sentí cansado")

        sessions.updateSession(edited)

        assertEquals(edited, sessions.observeSession(session.id).first())
    }

    @Test
    fun writesOnMissingRows_doNothing() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        val stored = sessions.setsOfSession(session.id).single()

        sessions.updateSession(WorkoutSession(id = 42, date = oct10, notes = "Otra"))
        sessions.deleteSession(42)
        sessions.updateSet(stored.copy(id = 42, order = 2))
        sessions.deleteSet(42)

        assertEquals(listOf(session), sessions.observeHistory().first())
        assertEquals(listOf(stored), sessions.setsOfSession(session.id))
    }

    // S20-R20, S10-R23
    @Test
    fun deleteSession_deletesItsSets_andKeepsTheExercises() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1), set(squat.id, order = 2))

        sessions.deleteSession(session.id)

        assertNull(sessions.observeSession(session.id).first())
        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(session.id))
        assertEquals(listOf(bench, squat), exercises.observeActiveCatalog().first())
    }

    // S10-R24
    @Test
    fun deleteSet_keepsTheSessionAndItsOtherSets() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1), set(bench.id, order = 2), set(bench.id, order = 3))
        val second = sessions.setsOfSession(session.id)[1]

        sessions.deleteSet(second.id)

        assertEquals(listOf(1, 3), sessions.setsOfSession(session.id).map { it.order })
        assertEquals(session, sessions.observeSession(session.id).first())
    }

    // S20-R31, S10-R28
    @Test
    fun observeHistory_ordersByDateDescending_thenIdDescending() = runTest {
        val older = createSession(oct10.minusDays(3))
        val first = createSession(oct10)
        val second = createSession(oct10)

        assertEquals(listOf(second, first, older), sessions.observeHistory().first())
    }

    // S20-R32, S10-R29
    @Test
    fun observeSessionSets_comeByOrder_withTheirExercise_includingArchived() = runTest {
        val session = createSession(oct10, set(squat.id, order = 3), set(bench.id, order = 1))
        exercises.delete(bench.id) // it has sets, so it is archived

        val pairs = sessions.observeSessionSets(session.id).first()

        assertEquals(listOf(1, 3), pairs.map { it.first.order })
        assertEquals(listOf(bench.copy(archived = true), squat), pairs.map { it.second })
    }

    // S20-R29, S10-R25, S10-R14
    @Test
    fun observeLastPerformance_returnsAllSetsOfTheMostRecentSessionWithTheExercise() = runTest {
        createSession(oct10.minusDays(1), set(bench.id, order = 1, weightGrams = 50_000))
        createSession(oct10, set(bench.id, order = 1, weightGrams = 55_000))
        val latest = createSession(
            oct10,
            set(bench.id, order = 3, weightGrams = 62_500),
            set(squat.id, order = 1),
            set(bench.id, order = 2, weightGrams = 60_000),
        )

        val last = sessions.observeLastPerformance(bench.id).first()

        assertEquals(latest, last?.session)
        assertEquals(listOf(2, 3), last?.sets?.map { it.order })
        assertEquals(listOf(60_000, 62_500), last?.sets?.map { it.weightGrams })
    }

    // S20-R29, S10-R26, S10-R18
    @Test
    fun observeLastPerformance_beforeASession_excludesItAndLaterOnes_andIncludesArchivedExercise() = runTest {
        val previous = createSession(oct10.minusDays(1), set(bench.id, order = 1, weightGrams = 50_000))
        val current = createSession(oct10, set(bench.id, order = 1, weightGrams = 55_000))
        createSession(oct10, set(bench.id, order = 1, weightGrams = 57_500))
        createSession(oct10.plusDays(1), set(bench.id, order = 1, weightGrams = 60_000))
        exercises.delete(bench.id) // it has sets, so it is archived

        val last = sessions.observeLastPerformance(bench.id, before = current).first()

        assertEquals(previous, last?.session)
        assertEquals(listOf(50_000), last?.sets?.map { it.weightGrams })
    }

    // S20-R29, S10-R25
    @Test
    fun observeLastPerformance_withoutSetsOfTheExercise_isNull() = runTest {
        createSession(oct10, set(squat.id, order = 1))

        assertNull(sessions.observeLastPerformance(bench.id).first())
    }

    // S20-R30, S10-R27
    @Test
    fun observeEvolution_ordersByDateThenSessionIdThenOrder() = runTest {
        createSession(oct10, set(bench.id, order = 2, weightGrams = 62_500), set(bench.id, order = 1, weightGrams = 60_000))
        val yesterday = createSession(oct10.minusDays(1), set(bench.id, order = 1, weightGrams = 50_000))
        createSession(oct10, set(squat.id, order = 1), set(bench.id, order = 2, weightGrams = 65_000))

        val entries = sessions.observeEvolution(bench.id).first().entries

        assertEquals(listOf(50_000, 60_000, 62_500, 65_000), entries.map { it.set.weightGrams })
        assertEquals(listOf(yesterday.date, oct10, oct10, oct10), entries.map { it.date })
    }

    // SF02-A02 / SF02-R17, S20-A42 / S20-R49, S10-R14: two sessions on the same day, the one with the higher id counts
    @Test
    fun observeLatestSessionPeaks_sameDaySessions_returnsTheHigherIdAndItsHeaviestWeight() = runTest {
        createSession(oct10, set(bench.id, order = 1, weightGrams = 60_000))
        val latest = createSession(oct10, set(bench.id, order = 1, weightGrams = 62_500))

        val peaks = sessions.observeLatestSessionPeaks().first()

        assertEquals(listOf(LatestSessionPeak(bench.id, latest.id, oct10, 62_500)), peaks)
    }

    // SF02-A02 / SF02-R17, S20-R49: the heaviest set of the latest session, not its last set nor an older session's
    @Test
    fun observeLatestSessionPeaks_usesTheHeaviestSetOfTheLatestSession_perExercise() = runTest {
        createSession(oct10.minusDays(2), set(bench.id, order = 1, weightGrams = 100_000))
        val latest = createSession(
            oct10,
            set(bench.id, order = 1, weightGrams = 65_000),
            set(squat.id, order = 2, weightGrams = 80_000),
            set(bench.id, order = 3, weightGrams = 60_000),
        )

        val peaks = sessions.observeLatestSessionPeaks().first().sortedBy { it.exerciseId }

        assertEquals(
            listOf(
                LatestSessionPeak(bench.id, latest.id, oct10, 65_000),
                LatestSessionPeak(squat.id, latest.id, oct10, 80_000),
            ),
            peaks,
        )
    }

    // SF02-A02 / SF02-R17, S20-R49: an exercise without sets has no row, and archived exercises keep theirs
    @Test
    fun observeLatestSessionPeaks_skipsExercisesWithoutSets_andIncludesArchivedOnes() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1, weightGrams = 0))
        exercises.delete(bench.id) // it has sets, so it is archived

        val peaks = sessions.observeLatestSessionPeaks().first()

        assertEquals(listOf(LatestSessionPeak(bench.id, session.id, oct10, 0)), peaks)
        assertTrue(peaks.none { it.exerciseId == squat.id })
    }

    // SF02-R17, S20-R49: the subscription starts before the write, so the new value is a second emission
    @Test
    fun observeLatestSessionPeaks_emitsAgainAfterASetIsAdded() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1, weightGrams = 60_000))
        val peaks = async(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeLatestSessionPeaks().first { list -> list.any { it.heaviestWeightGrams == 62_500 } }
        }

        sessions.addSet(set(bench.id, order = 2, weightGrams = 62_500, sessionId = session.id))

        assertEquals(listOf(LatestSessionPeak(bench.id, session.id, oct10, 62_500)), peaks.await())
    }

    // S20-A33: the subscription starts before the write, so the new value is a second emission
    @Test
    fun observeHistory_emitsAgainAfterCreateSession() = runTest {
        val history = async(start = CoroutineStart.UNDISPATCHED) {
            sessions.observeHistory().first { it.isNotEmpty() }
        }

        val session = createSession()

        assertEquals(listOf(session), history.await())
    }
}

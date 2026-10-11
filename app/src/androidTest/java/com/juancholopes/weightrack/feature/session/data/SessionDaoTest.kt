package com.juancholopes.weightrack.feature.session.data

import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.data.assertSqliteRejects
import com.juancholopes.weightrack.data.countRows
import com.juancholopes.weightrack.data.newInMemoryDatabase
import com.juancholopes.weightrack.data.rawRows
import com.juancholopes.weightrack.feature.catalog.data.ExerciseDao
import com.juancholopes.weightrack.feature.catalog.data.toEntity
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SessionDaoTest {

    private lateinit var db: WeightrackDatabase
    private lateinit var dao: SessionDao
    private lateinit var exerciseDao: ExerciseDao

    private val oct10: LocalDate = LocalDate.of(2026, 10, 10)

    @Before
    fun setUp() {
        db = newInMemoryDatabase()
        dao = db.sessionDao()
        exerciseDao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newExercise(name: String = "Press de banca", archived: Boolean = false): Long =
        exerciseDao.insertWithMuscles(
            Exercise(id = 0, name = name, equipment = Equipment.BARBELL, notes = null, archived = archived).toEntity(),
            emptyList(),
        )

    private suspend fun newSession(date: LocalDate = oct10, notes: String? = null): Long =
        dao.insertSession(WorkoutSessionEntity(id = 0, date = date, notes = notes))

    private suspend fun newSet(sessionId: Long, exerciseId: Long, order: Int, grams: Int = 60_000, reps: Int = 10): Long =
        dao.insertSet(ExerciseSetEntity(id = 0, sessionId = sessionId, exerciseId = exerciseId, order = order, weightGrams = grams, reps = reps))

    // S20-A12 / S20-R15
    @Test
    fun storesDateAsIsoText() = runTest {
        val id = newSession(oct10, notes = "Me sentí cansado")

        val row = db.rawRows("SELECT date, typeof(date) AS t FROM workout_session").single()
        assertEquals("2026-10-10", row["date"])
        assertEquals("text", row["t"])
        assertEquals(WorkoutSessionEntity(id, oct10, "Me sentí cansado"), dao.observeSession(id).first())
    }

    // S20-A13 / S20-R17, S10-R14
    @Test
    fun sessionIds_growInCreationOrder_evenAfterDeletingLatest() = runTest {
        val first = newSession()
        val second = newSession()
        val third = newSession()
        assertTrue(first < second && second < third)

        dao.deleteSession(third)
        val fourth = newSession()

        assertTrue("$fourth must be greater than $third", fourth > third)
    }

    // S20-A14 / S20-R20, S20-R22, S10-R10
    @Test
    fun insertSet_withMissingSessionOrExercise_fails() = runTest {
        val exerciseId = newExercise()
        val sessionId = newSession()

        assertSqliteRejects { newSet(sessionId = sessionId + 100, exerciseId = exerciseId, order = 1) }
        assertSqliteRejects { newSet(sessionId = sessionId, exerciseId = exerciseId + 100, order = 1) }
        assertEquals(0L, db.countRows("exercise_set"))
    }

    // S20-A15 / S20-R20, S10-R23
    @Test
    fun deleteSession_cascadesToSets_andKeepsExercises() = runTest {
        val exerciseId = newExercise()
        val sessionId = newSession()
        val otherSession = newSession()
        newSet(sessionId, exerciseId, 1)
        newSet(sessionId, exerciseId, 2)
        newSet(otherSession, exerciseId, 1)
        val exerciseBefore = exerciseDao.observeExercise(exerciseId).first()

        assertEquals(1, dao.deleteSession(sessionId))

        assertEquals(1L, db.countRows("workout_session"))
        assertEquals(listOf(otherSession), db.rawRows("SELECT session_id FROM exercise_set").map { it["session_id"] })
        assertEquals(exerciseBefore, exerciseDao.observeExercise(exerciseId).first())
    }

    // S20-A22 / S20-R26
    @Test
    fun duplicateSetOrder_inSameSession_isRejected() = runTest {
        val exerciseId = newExercise()
        val sessionId = newSession()
        val otherSession = newSession()
        newSet(sessionId, exerciseId, 1)

        assertSqliteRejects { newSet(sessionId, exerciseId, 1) }
        newSet(otherSession, exerciseId, 1)
        assertEquals(2L, db.countRows("exercise_set"))
    }

    // S20-A23 / S20-R16, S20-R28
    @Test
    fun weightGrams_roundTrip_includingZero() = runTest {
        val exerciseId = newExercise()
        val sessionId = newSession()
        listOf(0, 62_500, 1_000_000).forEachIndexed { index, grams -> newSet(sessionId, exerciseId, index + 1, grams) }

        assertEquals(listOf(0, 62_500, 1_000_000), dao.setsOfSession(sessionId).map { it.weightGrams })
        val rows = db.rawRows("SELECT weight_grams, typeof(weight_grams) AS t FROM exercise_set ORDER BY set_order")
        assertEquals(listOf(0L, 62_500L, 1_000_000L), rows.map { it["weight_grams"] })
        rows.forEach { assertEquals("integer", it["t"]) }
    }

    // S20-A24 / S20-R29, S10-R25
    @Test
    fun lastPerformance_sameDaySessions_returnsHigherId() = runTest {
        val exerciseId = newExercise()
        val older = newSession(oct10.minusDays(1))
        val morning = newSession(oct10)
        val evening = newSession(oct10)
        newSet(older, exerciseId, 1)
        newSet(morning, exerciseId, 1)
        newSet(evening, exerciseId, 3, grams = 70_000)
        newSet(evening, exerciseId, 1, grams = 60_000)

        val rows = dao.observeLastPerformance(exerciseId, null, null).first()

        assertEquals(listOf(evening, evening), rows.map { it.session.id })
        assertEquals(listOf(1, 3), rows.map { it.set.order })
        assertEquals(listOf(60_000, 70_000), rows.map { it.set.weightGrams })
        assertEquals(oct10, rows.first().session.date)
    }

    // S20-A25 / S20-R29, S10-R26
    @Test
    fun lastPerformance_beforeSession_excludesInProgressAndLater() = runTest {
        val exerciseId = newExercise()
        val previousDay = newSession(oct10.minusDays(1))
        val sameDayEarlier = newSession(oct10)
        val inProgress = newSession(oct10)
        val sameDayLater = newSession(oct10)
        val nextDay = newSession(oct10.plusDays(1))
        listOf(previousDay, sameDayEarlier, inProgress, sameDayLater, nextDay).forEach { newSet(it, exerciseId, 1) }

        val fromInProgress = dao.observeLastPerformance(exerciseId, oct10, inProgress).first()
        assertEquals(listOf(sameDayEarlier), fromInProgress.map { it.session.id })

        val fromEarliest = dao.observeLastPerformance(exerciseId, oct10, sameDayEarlier).first()
        assertEquals(listOf(previousDay), fromEarliest.map { it.session.id })
    }

    // S20-A25 / S20-R29
    @Test
    fun lastPerformance_withoutSets_isEmpty() = runTest {
        val exerciseId = newExercise()
        val other = newExercise("Sentadilla")
        newSet(newSession(), other, 1)

        assertEquals(emptyList<SetWithSessionRow>(), dao.observeLastPerformance(exerciseId, null, null).first())
    }

    // S20-A25 / S10-R18
    @Test
    fun lastPerformance_includesArchivedExercise() = runTest {
        val exerciseId = newExercise()
        val sessionId = newSession()
        newSet(sessionId, exerciseId, 1)
        exerciseDao.setArchived(exerciseId, true)

        assertEquals(listOf(sessionId), dao.observeLastPerformance(exerciseId, null, null).first().map { it.session.id })
    }

    // S20-A26 / S20-R30
    @Test
    fun evolution_ordersByDateSessionIdAndOrder() = runTest {
        val exerciseId = newExercise()
        val later = newSession(oct10)
        val earlierSameDay = newSession(oct10.minusDays(2))
        val laterSameDay = newSession(oct10.minusDays(2))
        newSet(later, exerciseId, 1)
        newSet(laterSameDay, exerciseId, 2)
        newSet(laterSameDay, exerciseId, 1)
        newSet(earlierSameDay, exerciseId, 1)

        val rows = dao.observeEvolution(exerciseId).first()

        assertEquals(
            listOf(earlierSameDay to 1, laterSameDay to 1, laterSameDay to 2, later to 1),
            rows.map { it.session.id to it.set.order },
        )
    }

    // S20-A27 / S20-R31
    @Test
    fun history_ordersByDateDescThenIdDesc() = runTest {
        val old = newSession(oct10.minusDays(3))
        val first = newSession(oct10)
        val second = newSession(oct10)
        val middle = newSession(oct10.minusDays(1))

        assertEquals(listOf(second, first, middle, old), dao.observeHistory().first().map { it.id })
    }

    // S20-A28 / S20-R32
    @Test
    fun sessionSets_orderedWithExercise_includingArchived() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        val sessionId = newSession()
        newSet(sessionId, squat, 5)
        newSet(sessionId, bench, 1)
        newSet(sessionId, bench, 3)
        exerciseDao.setArchived(squat, true)

        val rows = dao.observeSessionSets(sessionId).first()

        assertEquals(listOf(1, 3, 5), rows.map { it.set.order })
        assertEquals(listOf("Press de banca", "Press de banca", "Sentadilla"), rows.map { it.exercise.name })
        assertEquals(listOf(false, false, true), rows.map { it.exercise.archived })
    }

    // S20-A32 / S20-R39
    @Test
    fun insertSessionWithSets_whenOneSetFails_insertsNothing() = runTest {
        val exerciseId = newExercise()

        assertSqliteRejects {
            dao.insertSessionWithSets(
                WorkoutSessionEntity(id = 0, date = oct10, notes = null),
                listOf(
                    ExerciseSetEntity(0, 0, exerciseId, 1, 60_000, 10),
                    ExerciseSetEntity(0, 0, exerciseId + 100, 2, 60_000, 10),
                ),
            )
        }
        assertEquals(0L, db.countRows("workout_session"))
        assertEquals(0L, db.countRows("exercise_set"))
    }

    // S20-R39
    @Test
    fun insertSessionWithSets_assignsSessionIdToEverySet() = runTest {
        val exerciseId = newExercise()

        val sessionId = dao.insertSessionWithSets(
            WorkoutSessionEntity(id = 0, date = oct10, notes = null),
            listOf(ExerciseSetEntity(0, 0, exerciseId, 1, 60_000, 10), ExerciseSetEntity(0, 0, exerciseId, 2, 62_500, 8)),
        )

        assertEquals(listOf(sessionId, sessionId), dao.setsOfSession(sessionId).map { it.sessionId })
    }

    // S20-A33 / S20-R37
    @Test
    fun observeHistory_emitsAfterInsert() = runTest {
        val firstEmission = CompletableDeferred<Unit>()
        val emissions = async(Dispatchers.Default) {
            dao.observeHistory()
                .onEach { firstEmission.complete(Unit) }
                .take(2)
                .toList()
        }
        withContext(Dispatchers.Default) { withTimeout(5_000) { firstEmission.await() } }

        val id = newSession()

        val result = withContext(Dispatchers.Default) { withTimeout(5_000) { emissions.await() } }
        assertEquals(listOf(emptyList(), listOf(id)), result.map { list -> list.map { it.id } })
    }
}

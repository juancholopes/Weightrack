package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.session.domain.ExerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LastPerformance
import com.juancholopes.weightrack.feature.session.domain.LatestSessionPeak
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.domain.exerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.latestSessionPeaks
import com.juancholopes.weightrack.feature.session.domain.lastPerformance
import com.juancholopes.weightrack.feature.session.domain.sessionHistory
import com.juancholopes.weightrack.feature.session.domain.sessionSets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory [SessionRepository] for JVM tests (Spec 31, S31-R05). It honors the same contract as
 * `RoomSessionRepository`, proven by `SessionRepositoryContract`, and answers reads with the domain
 * functions of Spec 10. Share its [InMemoryStore] with a [FakeExerciseRepository] to have exercises.
 */
class FakeSessionRepository(private val store: InMemoryStore = InMemoryStore()) : SessionRepository {

    private val nextWriteFailure = AtomicReference<Exception?>(null)
    private val nextReadFailure = AtomicReference<Exception?>(null)

    /**
     * Makes the next write (`createSession`, `updateSession`, `deleteSession`, `addSet`, `updateSet` or
     * `deleteSet`) throw [error] without writing anything, as an unexpected database failure would (S31-R08,
     * Spec F02 SF02-R22). Later writes work.
     */
    fun failNextWrite(error: Exception = IllegalStateException("Simulated write failure")) {
        nextWriteFailure.set(error)
    }

    /**
     * Makes the next read (any `observe...` flow or `setsOfSession`) throw [error], as an unexpected database failure
     * would (S31-R08, Spec F03 SF03-R08). A flow throws when it is collected, not when it is created, so a `catch`
     * on the flow sees it. Later reads work, and writes never use it.
     */
    fun failNextRead(error: Exception = IllegalStateException("Simulated read failure")) {
        nextReadFailure.set(error)
    }

    private fun throwIfReadFails() {
        nextReadFailure.getAndSet(null)?.let { throw it }
    }

    private fun <T> Flow<T>.failingIfAsked(): Flow<T> = onStart { throwIfReadFails() }

    private fun throwIfWriteFails() {
        nextWriteFailure.getAndSet(null)?.let { throw it }
    }

    override fun observeHistory(): Flow<List<WorkoutSession>> =
        store.observe { sessionHistory(it.sessions) }.failingIfAsked()

    override fun observeSession(id: Long): Flow<WorkoutSession?> =
        store.observe { tables -> tables.sessions.firstOrNull { it.id == id } }.failingIfAsked()

    override fun observeSessionSets(sessionId: Long): Flow<List<Pair<ExerciseSet, Exercise>>> =
        store.observe { sessionSets(sessionId, it.sets, it.exercises) }.failingIfAsked()

    override fun observeLastPerformance(exerciseId: Long, before: WorkoutSession?): Flow<LastPerformance?> =
        store.observe { lastPerformance(exerciseId, it.sessions, it.sets, before) }.failingIfAsked()

    override fun observeEvolution(exerciseId: Long): Flow<ExerciseEvolution> =
        store.observe { exerciseEvolution(exerciseId, it.sessions, it.sets) }.failingIfAsked()

    /** SF02-R17, S20-R49. */
    override fun observeLatestSessionPeaks(): Flow<List<LatestSessionPeak>> =
        store.observe { latestSessionPeaks(it.sessions, it.sets) }.failingIfAsked()

    override suspend fun setsOfSession(sessionId: Long): List<ExerciseSet> {
        throwIfReadFails()
        return store.read { tables -> tables.sets.filter { it.sessionId == sessionId }.sortedBy { it.order } }
    }

    /** S20-R39: the session and all its sets, or nothing. */
    override suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet>): Long {
        throwIfWriteFails()
        return store.write { tables ->
            val sessionId = tables.lastSessionId + 1
            val newSets = sets.mapIndexed { index, set ->
                set.copy(id = tables.lastSetId + 1 + index, sessionId = sessionId)
            }
            val next = tables.copy(
                sessions = tables.sessions + session.copy(id = sessionId),
                sets = tables.sets + newSets,
                lastSessionId = sessionId,
                lastSetId = tables.lastSetId + newSets.size,
            )
            next to sessionId
        }
    }

    override suspend fun updateSession(session: WorkoutSession) {
        throwIfWriteFails()
        store.write { tables ->
            tables.copy(sessions = tables.sessions.map { if (it.id == session.id) session else it }) to Unit
        }
    }

    /** S20-R20: deleting a session deletes its sets. */
    override suspend fun deleteSession(sessionId: Long) {
        throwIfWriteFails()
        store.write { tables ->
            tables.copy(
                sessions = tables.sessions.filter { it.id != sessionId },
                sets = tables.sets.filter { it.sessionId != sessionId },
            ) to Unit
        }
    }

    override suspend fun addSet(set: ExerciseSet): Long {
        throwIfWriteFails()
        return store.write { tables ->
            val id = tables.lastSetId + 1
            tables.copy(sets = tables.sets + set.copy(id = id), lastSetId = id) to id
        }
    }

    override suspend fun updateSet(set: ExerciseSet) {
        throwIfWriteFails()
        store.write { tables ->
            tables.copy(sets = tables.sets.map { if (it.id == set.id) set else it }) to Unit
        }
    }

    override suspend fun deleteSet(setId: Long) {
        throwIfWriteFails()
        store.write { tables -> tables.copy(sets = tables.sets.filter { it.id != setId }) to Unit }
    }
}

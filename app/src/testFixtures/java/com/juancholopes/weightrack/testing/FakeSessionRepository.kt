package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.session.domain.ExerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LastPerformance
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.domain.exerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.lastPerformance
import com.juancholopes.weightrack.feature.session.domain.sessionHistory
import com.juancholopes.weightrack.feature.session.domain.sessionSets
import kotlinx.coroutines.flow.Flow

/**
 * In-memory [SessionRepository] for JVM tests (Spec 31, S31-R05). It honors the same contract as
 * `RoomSessionRepository`, proven by `SessionRepositoryContract`, and answers reads with the domain
 * functions of Spec 10. Share its [InMemoryStore] with a [FakeExerciseRepository] to have exercises.
 */
class FakeSessionRepository(private val store: InMemoryStore = InMemoryStore()) : SessionRepository {

    override fun observeHistory(): Flow<List<WorkoutSession>> =
        store.observe { sessionHistory(it.sessions) }

    override fun observeSession(id: Long): Flow<WorkoutSession?> =
        store.observe { tables -> tables.sessions.firstOrNull { it.id == id } }

    override fun observeSessionSets(sessionId: Long): Flow<List<Pair<ExerciseSet, Exercise>>> =
        store.observe { sessionSets(sessionId, it.sets, it.exercises) }

    override fun observeLastPerformance(exerciseId: Long, before: WorkoutSession?): Flow<LastPerformance?> =
        store.observe { lastPerformance(exerciseId, it.sessions, it.sets, before) }

    override fun observeEvolution(exerciseId: Long): Flow<ExerciseEvolution> =
        store.observe { exerciseEvolution(exerciseId, it.sessions, it.sets) }

    override suspend fun setsOfSession(sessionId: Long): List<ExerciseSet> =
        store.read { tables -> tables.sets.filter { it.sessionId == sessionId }.sortedBy { it.order } }

    /** S20-R39: the session and all its sets, or nothing. */
    override suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet>): Long =
        store.write { tables ->
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

    override suspend fun updateSession(session: WorkoutSession) {
        store.write { tables ->
            tables.copy(sessions = tables.sessions.map { if (it.id == session.id) session else it }) to Unit
        }
    }

    /** S20-R20: deleting a session deletes its sets. */
    override suspend fun deleteSession(sessionId: Long) {
        store.write { tables ->
            tables.copy(
                sessions = tables.sessions.filter { it.id != sessionId },
                sets = tables.sets.filter { it.sessionId != sessionId },
            ) to Unit
        }
    }

    override suspend fun addSet(set: ExerciseSet): Long =
        store.write { tables ->
            val id = tables.lastSetId + 1
            tables.copy(sets = tables.sets + set.copy(id = id), lastSetId = id) to id
        }

    override suspend fun updateSet(set: ExerciseSet) {
        store.write { tables ->
            tables.copy(sets = tables.sets.map { if (it.id == set.id) set else it }) to Unit
        }
    }

    override suspend fun deleteSet(setId: Long) {
        store.write { tables -> tables.copy(sets = tables.sets.filter { it.id != setId }) to Unit }
    }
}

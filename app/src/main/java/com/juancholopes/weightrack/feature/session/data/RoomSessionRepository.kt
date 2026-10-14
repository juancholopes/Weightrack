package com.juancholopes.weightrack.feature.session.data

import com.juancholopes.weightrack.feature.catalog.data.toDomain
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.session.domain.ExerciseEvolution
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LastPerformance
import com.juancholopes.weightrack.feature.session.domain.LatestSessionPeak
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room implementation of [SessionRepository] (S20-R43). No entity or row leaves this class. */
class RoomSessionRepository(private val dao: SessionDao) : SessionRepository {

    override fun observeHistory(): Flow<List<WorkoutSession>> =
        dao.observeHistory().map { list -> list.map { it.toDomain() } }

    override fun observeSession(id: Long): Flow<WorkoutSession?> =
        dao.observeSession(id).map { it?.toDomain() }

    override fun observeSessionSets(sessionId: Long): Flow<List<Pair<ExerciseSet, Exercise>>> =
        dao.observeSessionSets(sessionId).map { rows -> rows.map { it.set.toDomain() to it.exercise.toDomain() } }

    override fun observeLastPerformance(exerciseId: Long, before: WorkoutSession?): Flow<LastPerformance?> =
        dao.observeLastPerformance(exerciseId, before?.date, before?.id).map { rows ->
            rows.firstOrNull()?.let { first ->
                LastPerformance(first.session.toDomain(), rows.map { it.set.toDomain() })
            }
        }

    override fun observeEvolution(exerciseId: Long): Flow<ExerciseEvolution> =
        dao.observeEvolution(exerciseId).map { rows ->
            ExerciseEvolution(rows.map { ExerciseEvolution.Entry(it.session.date, it.set.toDomain()) })
        }

    override fun observeLatestSessionPeaks(): Flow<List<LatestSessionPeak>> =
        dao.observeLatestSessionPeaks().map { rows ->
            rows.map { LatestSessionPeak(it.exerciseId, it.sessionId, it.date, it.heaviestWeightGrams) }
        }

    override suspend fun setsOfSession(sessionId: Long): List<ExerciseSet> =
        dao.setsOfSession(sessionId).map { it.toDomain() }

    override suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet>): Long =
        dao.insertSessionWithSets(session.toEntity(), sets.map { it.toEntity() })

    override suspend fun updateSession(session: WorkoutSession) {
        dao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    override suspend fun addSet(set: ExerciseSet): Long = dao.insertSet(set.toEntity().copy(id = 0))

    override suspend fun updateSet(set: ExerciseSet) {
        dao.updateSet(set.toEntity())
    }

    override suspend fun deleteSet(setId: Long) {
        dao.deleteSet(setId)
    }
}

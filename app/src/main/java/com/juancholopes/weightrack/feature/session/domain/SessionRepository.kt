package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Session persistence contract (S20-R42). A session or set that is not saved yet carries `id = 0`
 * (and `sessionId = 0` in its sets); the repository uses the id the database assigns.
 */
interface SessionRepository {
    fun observeHistory(): Flow<List<WorkoutSession>>
    fun observeSession(id: Long): Flow<WorkoutSession?>
    fun observeSessionSets(sessionId: Long): Flow<List<Pair<ExerciseSet, Exercise>>>
    fun observeLastPerformance(exerciseId: Long, before: WorkoutSession? = null): Flow<LastPerformance?>
    fun observeEvolution(exerciseId: Long): Flow<ExerciseEvolution>
    suspend fun setsOfSession(sessionId: Long): List<ExerciseSet>
    suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet> = emptyList()): Long
    suspend fun updateSession(session: WorkoutSession)
    suspend fun deleteSession(sessionId: Long)
    suspend fun addSet(set: ExerciseSet): Long
    suspend fun updateSet(set: ExerciseSet)
    suspend fun deleteSet(setId: Long)
}

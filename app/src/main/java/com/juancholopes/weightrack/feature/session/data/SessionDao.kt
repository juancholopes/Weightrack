package com.juancholopes.weightrack.feature.session.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {

    /**
     * S20-R29: the sets of [exerciseId] in the most recent session containing it. With [beforeId],
     * only sessions earlier than ([beforeDate], [beforeId]) count. Empty means "no última vez".
     */
    @Query(
        """
        SELECT es.*, ws.id AS ws_id, ws.date AS ws_date, ws.notes AS ws_notes
        FROM exercise_set es
        JOIN workout_session ws ON ws.id = es.session_id
        WHERE es.exercise_id = :exerciseId
          AND es.session_id = (
            SELECT ws2.id
            FROM workout_session ws2
            WHERE EXISTS (
                SELECT 1 FROM exercise_set es2
                WHERE es2.session_id = ws2.id AND es2.exercise_id = :exerciseId)
              AND (:beforeId IS NULL
                   OR ws2.date < :beforeDate
                   OR (ws2.date = :beforeDate AND ws2.id < :beforeId))
            ORDER BY ws2.date DESC, ws2.id DESC
            LIMIT 1)
        ORDER BY es.set_order ASC
        """,
    )
    fun observeLastPerformance(exerciseId: Long, beforeDate: LocalDate?, beforeId: Long?): Flow<List<SetWithSessionRow>>

    /** S20-R30. */
    @Query(
        """
        SELECT es.*, ws.id AS ws_id, ws.date AS ws_date, ws.notes AS ws_notes
        FROM exercise_set es
        JOIN workout_session ws ON ws.id = es.session_id
        WHERE es.exercise_id = :exerciseId
        ORDER BY ws.date ASC, ws.id ASC, es.set_order ASC
        """,
    )
    fun observeEvolution(exerciseId: Long): Flow<List<SetWithSessionRow>>

    /** S20-R31. */
    @Query("SELECT * FROM workout_session ORDER BY date DESC, id DESC")
    fun observeHistory(): Flow<List<WorkoutSessionEntity>>

    /** S20-R32. */
    @Query(
        """
        SELECT es.*,
               e.id AS ex_id, e.name AS ex_name, e.normalized_name AS ex_normalized_name,
               e.equipment AS ex_equipment, e.notes AS ex_notes, e.archived AS ex_archived
        FROM exercise_set es
        JOIN exercise e ON e.id = es.exercise_id
        WHERE es.session_id = :sessionId
        ORDER BY es.set_order ASC
        """,
    )
    fun observeSessionSets(sessionId: Long): Flow<List<SetWithExerciseRow>>

    @Query("SELECT * FROM workout_session WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM exercise_set WHERE session_id = :sessionId ORDER BY set_order ASC")
    suspend fun setsOfSession(sessionId: Long): List<ExerciseSetEntity>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity): Int

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long): Int

    @Insert
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    @Update
    suspend fun updateSet(set: ExerciseSetEntity): Int

    @Query("DELETE FROM exercise_set WHERE id = :id")
    suspend fun deleteSet(id: Long): Int

    /** S20-R39: the session and all its sets, or nothing. Returns the new session id. */
    @Transaction
    suspend fun insertSessionWithSets(session: WorkoutSessionEntity, sets: List<ExerciseSetEntity>): Long {
        val id = insertSession(session.copy(id = 0))
        insertSets(sets.map { it.copy(id = 0, sessionId = id) })
        return id
    }
}

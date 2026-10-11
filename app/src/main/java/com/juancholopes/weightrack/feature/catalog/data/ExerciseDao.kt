package com.juancholopes.weightrack.feature.catalog.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    /** S20-R33: active catalog sorted by name, with optional filters that combine. */
    @Query(
        """
        SELECT e.* FROM exercise e
        WHERE e.archived = 0
          AND (:equipment IS NULL OR e.equipment = :equipment)
          AND (:muscle IS NULL OR EXISTS (
                SELECT 1 FROM exercise_muscle m
                WHERE m.exercise_id = e.id AND m.muscle = :muscle))
        ORDER BY e.normalized_name ASC
        """,
    )
    fun observeActive(muscle: Muscle?, equipment: Equipment?): Flow<List<ExerciseEntity>>

    /** S20-R34. */
    @Query("SELECT * FROM exercise WHERE archived = 1 ORDER BY normalized_name ASC")
    fun observeArchived(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    fun observeExercise(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercise_muscle WHERE exercise_id = :exerciseId")
    fun observeMuscles(exerciseId: Long): Flow<List<ExerciseMuscleEntity>>

    /** S20-R40: read inside the delete-or-archive transaction. */
    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun findById(id: Long): ExerciseEntity?

    /** S20-R40: read inside the delete-or-archive transaction. */
    @Query("SELECT * FROM exercise_muscle WHERE exercise_id = :exerciseId")
    suspend fun musclesOf(exerciseId: Long): List<ExerciseMuscleEntity>

    /** Has sets? Decides between deleting and archiving (S10-R17). */
    @Query("SELECT COUNT(*) FROM exercise_set WHERE exercise_id = :exerciseId")
    suspend fun countSetsOfExercise(exerciseId: Long): Int

    /** S20-R41: finds the exercise behind a name collision. */
    @Query("SELECT * FROM exercise WHERE normalized_name = :normalizedName")
    suspend fun findByNormalizedName(normalizedName: String): ExerciseEntity?

    /** S20-R35: changes only `archived`; returns the number of changed rows. */
    @Query("UPDATE exercise SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean): Int

    /** S20-R36: deletes only when the exercise has no sets; muscles go by cascade. */
    @Query(
        """
        DELETE FROM exercise
        WHERE id = :id
          AND NOT EXISTS (SELECT 1 FROM exercise_set WHERE exercise_id = :id)
        """,
    )
    suspend fun deleteIfHasNoSets(id: Long): Int

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Query("DELETE FROM exercise_muscle WHERE exercise_id = :exerciseId")
    suspend fun deleteMuscles(exerciseId: Long)

    /** S20-R38: inserts the exercise and its muscles atomically; returns the new id. */
    @Transaction
    suspend fun insertWithMuscles(exercise: ExerciseEntity, muscles: List<ExerciseMuscleEntity>): Long {
        val id = insertExercise(exercise.copy(id = 0))
        insertMuscles(muscles.map { it.copy(exerciseId = id) })
        return id
    }

    /** S20-R38: updates the row and replaces its muscles atomically. */
    @Transaction
    suspend fun updateWithMuscles(exercise: ExerciseEntity, muscles: List<ExerciseMuscleEntity>) {
        updateExercise(exercise)
        deleteMuscles(exercise.id)
        insertMuscles(muscles.map { it.copy(exerciseId = exercise.id) })
    }
}

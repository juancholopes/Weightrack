package com.juancholopes.weightrack.feature.catalog.domain

import kotlinx.coroutines.flow.Flow

/**
 * Catalog persistence contract (S20-R42). An exercise or muscle that is not saved yet carries
 * `id = 0` / `exerciseId = 0`; the repository uses the id the database assigns.
 */
interface ExerciseRepository {
    fun observeActiveCatalog(muscle: Muscle? = null, equipment: Equipment? = null): Flow<List<Exercise>>
    fun observeArchived(): Flow<List<Exercise>>
    fun observeExercise(id: Long): Flow<Exercise?>
    fun observeMuscles(exerciseId: Long): Flow<List<ExerciseMuscle>>

    /**
     * Whether at least one set references the exercise, updated when its sets change (S20-R48, Spec F01
     * SF01-R11). The editor uses it to offer archiving or deleting; the final decision stays in [delete].
     */
    fun observeHasSets(exerciseId: Long): Flow<Boolean>
    suspend fun create(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult
    suspend fun update(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult
    suspend fun delete(exerciseId: Long): ExerciseDeletion
    suspend fun unarchive(exerciseId: Long)
}

/** Outcome of creating or editing an exercise (S20-R41). */
sealed interface ExerciseWriteResult {
    data class Saved(val id: Long) : ExerciseWriteResult
    data class Rejected(val error: ExerciseError) : ExerciseWriteResult
}

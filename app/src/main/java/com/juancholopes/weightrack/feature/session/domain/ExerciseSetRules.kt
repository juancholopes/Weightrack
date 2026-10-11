package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Exercise

const val WEIGHT_STEP_GRAMS = 250
const val WEIGHT_MAX_GRAMS = 1_000_000
const val REPS_MAX = 999

enum class ExerciseSetError {
    WEIGHT_NEGATIVE,
    WEIGHT_NOT_MULTIPLE_OF_STEP,
    WEIGHT_TOO_HIGH,
    REPS_NOT_POSITIVE,
    REPS_TOO_HIGH,
    EXERCISE_ARCHIVED,
}

sealed interface ExerciseSetValidation {
    data class Valid(val set: ExerciseSet) : ExerciseSetValidation
    data class Invalid(val error: ExerciseSetError) : ExerciseSetValidation
}

/** S10-R09: a new set gets the highest `order` of its session plus 1; gaps are allowed. */
fun nextSetOrder(sessionId: Long, sets: List<ExerciseSet>): Int =
    (sets.filter { it.sessionId == sessionId }.maxOfOrNull { it.order } ?: 0) + 1

/**
 * Validates a new set of [exercise] in session [sessionId] (S10-R01 to S10-R03, S10-R09, S10-R11,
 * S10-R15). [sets] holds the sets already stored; [id] is a not-yet-persisted id.
 */
fun validateNewSet(
    id: Long,
    sessionId: Long,
    exercise: Exercise,
    sets: List<ExerciseSet>,
    weightGrams: Int,
    reps: Int,
): ExerciseSetValidation {
    val error = if (exercise.archived) ExerciseSetError.EXERCISE_ARCHIVED else valueError(weightGrams, reps)
    if (error != null) return ExerciseSetValidation.Invalid(error)
    val set = ExerciseSet(id, sessionId, exercise.id, nextSetOrder(sessionId, sets), weightGrams, reps)
    return ExerciseSetValidation.Valid(set)
}

/**
 * Validates the correction of an existing [set] (S10-R01 to S10-R03, S10-R11, S10-R15). Sets of an
 * archived exercise can be corrected, but a set cannot be moved to an archived [exercise].
 */
fun validateSetCorrection(
    set: ExerciseSet,
    exercise: Exercise,
    weightGrams: Int,
    reps: Int,
): ExerciseSetValidation {
    val movesToArchived = exercise.archived && exercise.id != set.exerciseId
    val error = if (movesToArchived) ExerciseSetError.EXERCISE_ARCHIVED else valueError(weightGrams, reps)
    if (error != null) return ExerciseSetValidation.Invalid(error)
    return ExerciseSetValidation.Valid(set.copy(exerciseId = exercise.id, weightGrams = weightGrams, reps = reps))
}

private fun valueError(weightGrams: Int, reps: Int): ExerciseSetError? = when {
    weightGrams < 0 -> ExerciseSetError.WEIGHT_NEGATIVE
    weightGrams % WEIGHT_STEP_GRAMS != 0 -> ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP
    weightGrams > WEIGHT_MAX_GRAMS -> ExerciseSetError.WEIGHT_TOO_HIGH
    reps <= 0 -> ExerciseSetError.REPS_NOT_POSITIVE
    reps > REPS_MAX -> ExerciseSetError.REPS_TOO_HIGH
    else -> null
}

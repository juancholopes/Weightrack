package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import kotlinx.coroutines.flow.first

/**
 * Changes the weight and the repetitions of a stored set in place (SF02-R04, S40-R28); its exercise, session and
 * `order` do not change (D15 of Spec 40). The sets of an archived exercise can be corrected (S10-R11).
 */
class CorrectSetUseCase(
    private val exercises: ExerciseRepository,
    private val sessions: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, setId: Long, weightGrams: Int, reps: Int): CorrectSetResult {
        val set = checkNotNull(sessions.setsOfSession(sessionId).firstOrNull { it.id == setId }) {
            "Set $setId does not exist in session $sessionId"
        }
        val exercise = checkNotNull(exercises.observeExercise(set.exerciseId).first()) {
            "Exercise ${set.exerciseId} does not exist"
        }
        return when (val validation = validateSetCorrection(set, exercise, weightGrams, reps)) {
            is ExerciseSetValidation.Invalid -> CorrectSetResult.Rejected(validation.error)
            is ExerciseSetValidation.Valid -> {
                sessions.updateSet(validation.set)
                CorrectSetResult.Saved(validation.set)
            }
        }
    }
}

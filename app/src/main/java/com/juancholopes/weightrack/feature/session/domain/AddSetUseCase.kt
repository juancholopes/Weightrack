package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/**
 * Adds a set to a session (SF02-R04, SF02-R05). With a [sessionId] the set goes to that session with the next
 * `order` (S10-R09). With `null` there is no session yet: it validates a new session dated [date] together with
 * the set and writes both with `createSession(session, listOf(set))` (S20-R39, Spec 40 D3), so that opening the
 * logger never leaves an empty session. Nothing is written when anything is rejected.
 */
class AddSetUseCase(
    private val exercises: ExerciseRepository,
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        sessionId: Long?,
        date: LocalDate,
        exerciseId: Long,
        weightGrams: Int,
        reps: Int,
    ): AddSetResult {
        val exercise = checkNotNull(exercises.observeExercise(exerciseId).first()) {
            "Exercise $exerciseId does not exist"
        }
        val existingSets = if (sessionId == null) emptyList() else sessions.setsOfSession(sessionId)
        val set = when (
            val validation = validateNewSet(NEW_ID, sessionId ?: NEW_ID, exercise, existingSets, weightGrams, reps)
        ) {
            is ExerciseSetValidation.Invalid -> return AddSetResult.RejectedSet(validation.error)
            is ExerciseSetValidation.Valid -> validation.set
        }
        if (sessionId != null) {
            sessions.addSet(set)
            return AddSetResult.Saved(sessionId)
        }
        return when (val validation = validateSession(NEW_ID, date, notes = null, clock = clock)) {
            is WorkoutSessionValidation.Invalid -> AddSetResult.RejectedSession(validation.error)
            is WorkoutSessionValidation.Valid ->
                AddSetResult.Saved(sessions.createSession(validation.session, listOf(set)))
        }
    }

    private companion object {
        /** A session or set that is not saved yet carries `id = 0` (S20-R42). */
        const val NEW_ID = 0L
    }
}

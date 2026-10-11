package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.NOTES_MAX_LENGTH
import com.juancholopes.weightrack.feature.catalog.domain.normalizeNotes
import java.time.Clock
import java.time.LocalDate

enum class WorkoutSessionError {
    DATE_IN_FUTURE,
    NOTES_TOO_LONG,
}

sealed interface WorkoutSessionValidation {
    data class Valid(val session: WorkoutSession) : WorkoutSessionValidation
    data class Invalid(val error: WorkoutSessionError) : WorkoutSessionValidation
}

/**
 * Validates the creation or edition of a session (S10-R12, S10-R13, S10-R15, S10-R32). Today's date
 * comes from the injected [clock].
 */
fun validateSession(id: Long, date: LocalDate, notes: String?, clock: Clock): WorkoutSessionValidation {
    val normalizedNotes = normalizeNotes(notes)
    val error = when {
        date.isAfter(LocalDate.now(clock)) -> WorkoutSessionError.DATE_IN_FUTURE
        normalizedNotes != null && normalizedNotes.length > NOTES_MAX_LENGTH -> WorkoutSessionError.NOTES_TOO_LONG
        else -> null
    }
    if (error != null) return WorkoutSessionValidation.Invalid(error)
    return WorkoutSessionValidation.Valid(WorkoutSession(id, date, normalizedNotes))
}

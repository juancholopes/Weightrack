package com.juancholopes.weightrack.feature.session.domain

import java.time.Clock
import java.time.LocalDate

/**
 * Changes the date and the note of a session (SF02-R04, S40-R30). The note is normalized by the domain
 * (S10-R12: blank becomes `null`) and validated before anything is written (S10-R15, S10-R32).
 */
class UpdateSessionUseCase(
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(sessionId: Long, date: LocalDate, notes: String?): SessionWriteResult =
        when (val validation = validateSession(sessionId, date, notes, clock)) {
            is WorkoutSessionValidation.Invalid -> SessionWriteResult.Rejected(validation.error)
            is WorkoutSessionValidation.Valid -> {
                sessions.updateSession(validation.session)
                SessionWriteResult.Saved(sessionId)
            }
        }
}

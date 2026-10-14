package com.juancholopes.weightrack.feature.session.domain

import java.time.Clock
import java.time.LocalDate

/**
 * "Empezar otra sesión": creates a session dated today with no note (SF02-R04, S40-F09, S40-R22). Today comes from
 * the injected [clock] (S30-R36).
 */
class CreateSessionUseCase(
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): SessionWriteResult =
        when (val validation = validateSession(id = 0, date = LocalDate.now(clock), notes = null, clock = clock)) {
            is WorkoutSessionValidation.Invalid -> SessionWriteResult.Rejected(validation.error)
            is WorkoutSessionValidation.Valid -> SessionWriteResult.Saved(sessions.createSession(validation.session))
        }
}

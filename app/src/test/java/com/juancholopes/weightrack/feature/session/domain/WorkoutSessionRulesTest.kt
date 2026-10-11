package com.juancholopes.weightrack.feature.session.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class WorkoutSessionRulesTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC)

    private fun errorOf(result: WorkoutSessionValidation) = (result as WorkoutSessionValidation.Invalid).error

    private fun validOf(result: WorkoutSessionValidation) = (result as WorkoutSessionValidation.Valid).session

    // S10-A22 / S10-R32
    @Test
    fun sessionDatedAfterTodayIsRejected() {
        assertEquals(
            WorkoutSessionError.DATE_IN_FUTURE,
            errorOf(validateSession(id = 0, date = today.plusDays(1), notes = null, clock = clock)),
        )
    }

    @Test
    fun sessionDatedTodayOrBeforeIsAccepted() {
        assertEquals(today, validOf(validateSession(id = 0, date = today, notes = null, clock = clock)).date)
        validOf(validateSession(id = 0, date = today.minusDays(30), notes = null, clock = clock))
    }

    // S10-A10 / S10-R12
    @Test
    fun sessionNotesAreTrimmedAndBlankNotesBecomeNull() {
        assertEquals("me sentí cansado", validOf(validateSession(0, today, "  me sentí cansado ", clock)).notes)
        assertNull(validOf(validateSession(0, today, "   ", clock)).notes)
    }

    // S10-R15
    @Test
    fun sessionNotesLongerThan500CharactersAreRejected() {
        validOf(validateSession(0, today, "a".repeat(500), clock))
        assertEquals(WorkoutSessionError.NOTES_TOO_LONG, errorOf(validateSession(0, today, "a".repeat(501), clock)))
    }
}

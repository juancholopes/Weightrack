package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.testing.FakeSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UpdateSessionUseCaseTest {

    private val sessions = FakeSessionRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC)
    private val updateSession = UpdateSessionUseCase(sessions, clock)

    private val oct11 = LocalDate.of(2026, 10, 11)
    private val oct8 = LocalDate.of(2026, 10, 8)

    private suspend fun newSession(notes: String? = null): Long = sessions.createSession(WorkoutSession(0, oct11, notes))

    private suspend fun stored(id: Long): WorkoutSession? = sessions.observeSession(id).first()

    // SF02-A01 / SF02-R04, S40-R30: changes the date and the note, trimmed
    @Test
    fun invoke_savesTheDateAndTheTrimmedNote() = runTest {
        val id = newSession()

        val result = updateSession(id, oct8, "  Me sentí con energía ")

        assertEquals(SessionWriteResult.Saved(id), result)
        assertEquals(WorkoutSession(id, oct8, "Me sentí con energía"), stored(id))
    }

    // SF02-A01 / SF02-R04, S40-A28, S10-R12: "   " is saved as no note
    @Test
    fun invoke_withABlankNote_savesNull() = runTest {
        val id = newSession(notes = "Antes")

        val result = updateSession(id, oct11, "   ")

        assertEquals(SessionWriteResult.Saved(id), result)
        assertNull(stored(id)?.notes)
    }

    // SF02-A01 / SF02-R04, S40-A28, S10-R32: tomorrow is rejected and nothing is written; today is accepted
    @Test
    fun invoke_forTomorrow_isRejected_andWritesNothing() = runTest {
        val id = newSession(notes = "Antes")

        val rejected = updateSession(id, oct11.plusDays(1), "Después")
        val accepted = updateSession(id, oct11, "Hoy")

        assertEquals(SessionWriteResult.Rejected(WorkoutSessionError.DATE_IN_FUTURE), rejected)
        assertEquals(SessionWriteResult.Saved(id), accepted)
        assertEquals(WorkoutSession(id, oct11, "Hoy"), stored(id))
    }

    // SF02-A01 / SF02-R04, S40-A28, S10-R15: 501 characters are rejected and nothing is written; 500 are accepted
    @Test
    fun invoke_withANoteOf501Characters_isRejected_andWritesNothing() = runTest {
        val id = newSession(notes = "Antes")

        val rejected = updateSession(id, oct11, "a".repeat(501))
        assertEquals(SessionWriteResult.Rejected(WorkoutSessionError.NOTES_TOO_LONG), rejected)
        assertEquals(WorkoutSession(id, oct11, "Antes"), stored(id))

        val accepted = updateSession(id, oct11, "a".repeat(500))
        assertEquals(SessionWriteResult.Saved(id), accepted)
    }
}

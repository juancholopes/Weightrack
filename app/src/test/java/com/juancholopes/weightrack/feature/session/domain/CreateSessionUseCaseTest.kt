package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.testing.FakeSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CreateSessionUseCaseTest {

    private val sessions = FakeSessionRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-11T23:30:00Z"), ZoneOffset.UTC)
    private val createSession = CreateSessionUseCase(sessions, clock)

    // SF02-A01 / SF02-R04, S40-R22: creates a session dated today by the injected clock, with no note
    @Test
    fun invoke_createsASessionDatedToday() = runTest {
        val result = createSession()

        val session = sessions.observeHistory().first().single()
        assertEquals(SessionWriteResult.Saved(session.id), result)
        assertEquals(WorkoutSession(session.id, LocalDate.of(2026, 10, 11), null), session)
        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(session.id))
    }

    // SF02-A04 / S40-R22: another session the same day is created only by calling it again, with a higher id
    @Test
    fun invoke_calledTwice_createsTwoSessionsOfTheSameDay() = runTest {
        createSession()
        createSession()

        val history = sessions.observeHistory().first()
        assertEquals(2, history.size)
        assertEquals(setOf(LocalDate.of(2026, 10, 11)), history.map { it.date }.toSet())
        assertEquals(true, history[0].id > history[1].id)
    }
}

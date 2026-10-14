package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.UpdateSessionUseCase
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessions = FakeSessionRepository()
    private val oct11 = LocalDate.of(2026, 10, 11)
    private val oct8 = LocalDate.of(2026, 10, 8)
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC)
    private val updateSession = UpdateSessionUseCase(sessions, clock)

    private suspend fun newSession(date: LocalDate = oct11, notes: String? = null): Long =
        sessions.createSession(WorkoutSession(0, date, notes))

    private suspend fun stored(id: Long): WorkoutSession? = sessions.observeSession(id).first()

    // SF02-A13 / SF02-R19, S42-R25: before the first data the form is loading
    @Test
    fun uiState_beforeTheFirstData_isLoading() = runTest {
        val id = newSession()
        val viewModel = SessionEditorViewModel(id, sessions, updateSession, clock)

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(oct11, viewModel.uiState.value.today)
    }

    // SF02-A13 / SF02-R19, S42-R21: the form starts with the date and the note of the session, without changes
    @Test
    fun uiState_startsWithTheStoredDateAndNote_withoutUnsavedChanges() = runTest {
        val id = newSession(oct8, "Me sentí con energía")

        val state = newEditor(id).uiState.value

        assertFalse(state.isLoading)
        assertEquals(oct8, state.date)
        assertEquals(oct11, state.today)
        assertEquals("Me sentí con energía", state.notes)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.showDatePicker)
        assertNull(state.fieldError)
    }

    // SF02-R19: a session without a note starts with an empty field
    @Test
    fun uiState_forASessionWithoutANote_hasAnEmptyNote() = runTest {
        val id = newSession(notes = null)

        assertEquals("", newEditor(id).uiState.value.notes)
    }

    // SF02-A13 / SF02-R19, S40-R40: the form has changes while it differs from the stored values, and not once restored
    @Test
    fun hasUnsavedChanges_isTrueOnlyWhileTheValuesDifferFromTheStoredOnes() = runTest {
        val id = newSession(oct8, "Antes")
        val viewModel = newEditor(id)

        viewModel.onNotesChange("Después")
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        viewModel.onNotesChange("Antes")
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onDateSelected(oct11)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        viewModel.onDateSelected(oct8)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // SF02-A13 / SF02-R19: the date picker is a state of the screen; choosing a date closes it and changes the date
    @Test
    fun theDatePicker_opensAndClosesWithTheStateOfTheScreen() = runTest {
        val id = newSession(oct11)
        val viewModel = newEditor(id)

        viewModel.onDateClick()
        assertTrue(viewModel.uiState.value.showDatePicker)
        viewModel.onDatePickerDismiss()
        assertFalse(viewModel.uiState.value.showDatePicker)
        assertEquals(oct11, viewModel.uiState.value.date)

        viewModel.onDateClick()
        viewModel.onDateSelected(oct8)

        assertFalse(viewModel.uiState.value.showDatePicker)
        assertEquals(oct8, viewModel.uiState.value.date)
    }

    // SF02-A13 / SF02-R19, S40-R30: saving changes the date and the note, trimmed, and sets the effect
    @Test
    fun onSaveClick_savesTheDateAndTheTrimmedNote_andSetsSaved() = runTest {
        val id = newSession(oct11)
        val viewModel = newEditor(id)
        viewModel.onDateSelected(oct8)
        viewModel.onNotesChange("  Sesión olvidada ")

        viewModel.onSaveClick()

        assertEquals(WorkoutSession(id, oct8, "Sesión olvidada"), stored(id))
        val state = viewModel.uiState.value
        assertTrue(state.saved)
        assertFalse(state.isSaving)
        assertFalse(state.hasUnsavedChanges)
        assertNull(state.fieldError)

        viewModel.onSavedHandled()
        assertFalse(viewModel.uiState.value.saved)
    }

    // SF02-A13 / SF02-R19, S40-A28, S10-R12: "   " is saved as no note
    @Test
    fun onSaveClick_withABlankNote_savesNull() = runTest {
        val id = newSession(oct11, "Antes")
        val viewModel = newEditor(id)
        viewModel.onNotesChange("   ")

        viewModel.onSaveClick()

        assertNull(stored(id)?.notes)
        assertTrue(viewModel.uiState.value.saved)
    }

    // SF02-A13 / SF02-R19, S42-A17, S40-A28: tomorrow is rejected on save, shows the error and writes nothing
    @Test
    fun onSaveClick_forTomorrow_showsTheDateError_andWritesNothing() = runTest {
        val id = newSession(oct11, "Antes")
        val viewModel = newEditor(id)
        viewModel.onDateSelected(oct11.plusDays(1))
        assertNull(viewModel.uiState.value.fieldError)

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals(WorkoutSessionError.DATE_IN_FUTURE, state.fieldError)
        assertFalse(state.saved)
        assertEquals(oct11.plusDays(1), state.date)
        assertEquals(WorkoutSession(id, oct11, "Antes"), stored(id))
    }

    // SF02-A13 / S40-A28, S10-R15: a note of 501 characters is rejected; one of 500 is saved
    @Test
    fun onSaveClick_withANoteOf501Characters_showsTheNotesError_andWritesNothing() = runTest {
        val id = newSession(oct11, "Antes")
        val viewModel = newEditor(id)
        viewModel.onNotesChange("a".repeat(501))

        viewModel.onSaveClick()

        assertEquals(WorkoutSessionError.NOTES_TOO_LONG, viewModel.uiState.value.fieldError)
        assertEquals("a".repeat(501), viewModel.uiState.value.notes)
        assertEquals("Antes", stored(id)?.notes)

        viewModel.onNotesChange("a".repeat(500))
        viewModel.onSaveClick()

        assertEquals("a".repeat(500), stored(id)?.notes)
        assertTrue(viewModel.uiState.value.saved)
    }

    // SF02-A13 / S40-R05: after a failed attempt the error follows the corrections
    @Test
    fun fieldError_afterAFailedSave_updatesWhileTheUserCorrects() = runTest {
        val id = newSession(oct11)
        val viewModel = newEditor(id)
        viewModel.onDateSelected(oct11.plusDays(2))
        viewModel.onSaveClick()
        assertEquals(WorkoutSessionError.DATE_IN_FUTURE, viewModel.uiState.value.fieldError)

        viewModel.onNotesChange("a".repeat(501))
        assertEquals(WorkoutSessionError.DATE_IN_FUTURE, viewModel.uiState.value.fieldError)

        viewModel.onDateSelected(oct11)
        assertEquals(WorkoutSessionError.NOTES_TOO_LONG, viewModel.uiState.value.fieldError)

        viewModel.onNotesChange("Bien")
        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF02-A13 / S40-R05: no error shows before the first attempt, even with an invalid value
    @Test
    fun fieldError_beforeAnySaveAttempt_isNull() = runTest {
        val id = newSession(oct11)
        val viewModel = newEditor(id)

        viewModel.onNotesChange("a".repeat(501))

        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF02-R19, S40-R40, S42-R06: back with changes asks to discard; "Seguir editando" closes the dialog
    @Test
    fun onDiscardRequest_opensTheDiscardDialog_andOnDialogDismissClosesIt() = runTest {
        val id = newSession(oct11)
        val viewModel = newEditor(id)
        viewModel.onNotesChange("Algo")

        viewModel.onDiscardRequest()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.onDialogDismiss()
        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals("Algo", viewModel.uiState.value.notes)
    }

    // SF02-A14 / SF02-R22, S40-A38, S30-R23: the write fails, the generic message shows, what was typed stays, and a retry saves
    @Test
    fun onSaveClick_whenTheWriteFails_showsGenericError_keepsWhatWasTyped_andARetrySaves() = runTest {
        val id = newSession(oct11, "Antes")
        val viewModel = newEditor(id)
        viewModel.onDateSelected(oct8)
        viewModel.onNotesChange("Después")
        sessions.failNextWrite()

        viewModel.onSaveClick()

        val failed = viewModel.uiState.value
        assertTrue(failed.genericError)
        assertFalse(failed.saved)
        assertFalse(failed.isSaving)
        assertEquals(oct8, failed.date)
        assertEquals("Después", failed.notes)
        assertTrue(failed.hasUnsavedChanges)
        assertEquals(WorkoutSession(id, oct11, "Antes"), stored(id))

        viewModel.onGenericErrorHandled()
        assertFalse(viewModel.uiState.value.genericError)
        viewModel.onSaveClick()

        assertEquals(WorkoutSession(id, oct8, "Después"), stored(id))
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.genericError)
    }

    // SF02-R19, S40-R07: two taps while the first write runs save once, and nothing saves after it ended
    @Test
    fun onSaveClick_twiceInARow_writesOnce() = runTest {
        val id = newSession(oct11, "Antes")
        val gate = CompletableDeferred<Unit>()
        val gated = GatedSessions(sessions, gate)
        val viewModel = newEditor(id, sessionRepository = gated, update = UpdateSessionUseCase(gated, clock))
        viewModel.onNotesChange("Después")

        viewModel.onSaveClick()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.onSaveClick()
        gate.complete(Unit)

        assertEquals(1, gated.updates)
        assertTrue(viewModel.uiState.value.saved)
        viewModel.onSaveClick()
        assertEquals(1, gated.updates)
    }

    /** Builds the ViewModel and starts an empty collector, since `uiState` comes from `stateIn` (S31-R11). */
    private fun TestScope.newEditor(
        sessionId: Long,
        sessionRepository: SessionRepository = sessions,
        update: UpdateSessionUseCase = updateSession,
    ): SessionEditorViewModel {
        val viewModel = SessionEditorViewModel(sessionId, sessionRepository, update, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Holds `updateSession` until [gate] opens, to tap twice while the first write is still running. */
    private class GatedSessions(
        private val inner: SessionRepository,
        private val gate: CompletableDeferred<Unit>,
    ) : SessionRepository by inner {
        var updates = 0
            private set

        override suspend fun updateSession(session: WorkoutSession) {
            updates++
            gate.await()
            inner.updateSession(session)
        }
    }
}

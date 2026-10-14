package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.normalizeNotes
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.SessionWriteResult
import com.juancholopes.weightrack.feature.session.domain.UpdateSessionUseCase
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionValidation
import com.juancholopes.weightrack.feature.session.domain.validateSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Everything the form remembers besides what the database holds (S30-R26: private and changed with `update`). */
private data class FormState(
    /** The date the user picked; `null` follows the stored one. */
    val date: LocalDate? = null,
    /** The note the user typed; `null` follows the stored one. */
    val notes: String? = null,
    /** Becomes `true` after the first failed save; from then on the error follows the corrections (S40-R05). */
    val checked: Boolean = false,
    /** A rejection the use case reported; cleared by the next edit. */
    val rejection: WorkoutSessionError? = null,
    val showDatePicker: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val genericError: Boolean = false,
) {
    /** One write at a time, and none once the form already ended saved (S40-R07). */
    val canWrite: Boolean get() = !isSaving && !saved
}

/**
 * Edits the date and the note of a session, S42-P05 (SF02-R19, SF02-R22). It reads nothing until its state is
 * collected (S30-R26) and saves through [UpdateSessionUseCase] (S30-R20), which rejects a future date and a long
 * note before writing.
 */
class SessionEditorViewModel(
    private val sessionId: Long,
    sessions: SessionRepository,
    private val updateSession: UpdateSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val form = MutableStateFlow(FormState())

    /** Private so that events can read the stored values without depending on `uiState` being up to date. */
    private val stored: StateFlow<WorkoutSession?> = sessions.observeSession(sessionId)
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val uiState: StateFlow<SessionEditorUiState> = combine(form, stored) { formState, session ->
        toUiState(formState, session)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        toUiState(form.value, stored.value),
    )

    /** The date field was tapped: opens the `DatePickerDialog`. */
    fun onDateClick() {
        form.update { it.copy(showDatePicker = true) }
    }

    fun onDatePickerDismiss() {
        form.update { it.copy(showDatePicker = false) }
    }

    /**
     * The user chose [date] in the picker, which disables future days (S10-R32). A future date that arrives anyway is
     * kept and rejected on save (S40-R30).
     */
    fun onDateSelected(date: LocalDate) {
        edit { it.copy(date = date, showDatePicker = false) }
    }

    fun onNotesChange(notes: String) {
        edit { it.copy(notes = notes) }
    }

    /** "Guardar": validates and writes through the use case; a rejection writes nothing and keeps the form (S40-R05). */
    fun onSaveClick() {
        val session = stored.value ?: return
        val current = form.value
        if (!current.canWrite) return
        val date = current.date ?: session.date
        val notes = current.notes ?: session.notes.orEmpty()
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = updateSession(sessionId, date, notes)) {
                    is SessionWriteResult.Saved -> form.update { it.copy(saved = true) }
                    is SessionWriteResult.Rejected -> form.update { it.copy(checked = true, rejection = result.error) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                form.update { it.copy(genericError = true) }
            } finally {
                form.update { it.copy(isSaving = false) }
            }
        }
    }

    /** Back with unsaved changes: the screen asks "¿Descartar los cambios?" (S40-R40). */
    fun onDiscardRequest() {
        form.update { it.copy(showDiscardDialog = true) }
    }

    fun onDialogDismiss() {
        form.update { it.copy(showDiscardDialog = false) }
    }

    fun onSavedHandled() {
        form.update { it.copy(saved = false) }
    }

    fun onGenericErrorHandled() {
        form.update { it.copy(genericError = false) }
    }

    private fun edit(change: (FormState) -> FormState) {
        form.update { change(it).copy(rejection = null) }
    }

    private fun toUiState(formState: FormState, session: WorkoutSession?): SessionEditorUiState {
        val today = LocalDate.now(clock)
        if (session == null) return SessionEditorUiState(date = today, today = today)
        val date = formState.date ?: session.date
        val notes = formState.notes ?: session.notes.orEmpty()
        return SessionEditorUiState(
            isLoading = false,
            date = date,
            today = today,
            notes = notes,
            fieldError = fieldErrorOf(formState, date, notes),
            showDatePicker = formState.showDatePicker,
            showDiscardDialog = formState.showDiscardDialog,
            hasUnsavedChanges = date != session.date || normalizeNotes(notes) != session.notes,
            isSaving = formState.isSaving,
            saved = formState.saved,
            genericError = formState.genericError,
        )
    }

    /** S40-R05: no error before the first failed attempt; after it, the error follows the corrections. */
    private fun fieldErrorOf(formState: FormState, date: LocalDate, notes: String): WorkoutSessionError? = when {
        formState.rejection != null -> formState.rejection
        formState.checked -> (validateSession(sessionId, date, notes, clock) as? WorkoutSessionValidation.Invalid)?.error
        else -> null
    }

    companion object {
        fun factory(sessionId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SessionEditorViewModel(
                    sessionId = sessionId,
                    sessions = appContainer.sessionRepository,
                    updateSession = appContainer.updateSessionUseCase,
                    clock = appContainer.clock,
                )
            }
        }
    }
}

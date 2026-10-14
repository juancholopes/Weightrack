package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import java.time.LocalDate

/**
 * State of the session data form, S42-P05 (SF02-R19). The screen shows the form only when [isLoading] is `false`
 * (S42-R25). Fields marked "effect" are one-shot (S30-R28): the screen runs them and calls the matching
 * `on...Handled()`.
 */
data class SessionEditorUiState(
    val isLoading: Boolean = true,
    /** The date in the form: the stored one until the user picks another. */
    val date: LocalDate,
    /** Today by the clock: the date picker disables every day after it (`selectableDates`, S10-R32). */
    val today: LocalDate,
    /** The note as typed; empty when the session has none. Saving trims it, and a blank note is saved as none (S10-R12). */
    val notes: String = "",
    /**
     * The reason the form was rejected; [WorkoutSessionError.DATE_IN_FUTURE] belongs next to the date and
     * [WorkoutSessionError.NOTES_TOO_LONG] next to the note. It appears after the first failed save and then follows
     * the corrections (S40-R05).
     */
    val fieldError: WorkoutSessionError? = null,
    /** The `DatePickerDialog` is open. */
    val showDatePicker: Boolean = false,
    /** "¿Descartar los cambios?" is open (S40-R40). The screen leaves when the user confirms. */
    val showDiscardDialog: Boolean = false,
    /** The date or the normalized note differ from the stored ones; the only case where the screen's back handler is active (S42-R06). */
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    /** Effect: the session was saved; the screen navigates back after receiving it (S30-R35). */
    val saved: Boolean = false,
    /** Effect: an unexpected failure happened; what was typed stays and "Guardar" retries (S30-R23). */
    val genericError: Boolean = false,
)

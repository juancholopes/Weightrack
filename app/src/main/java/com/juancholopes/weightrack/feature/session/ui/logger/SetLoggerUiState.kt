package com.juancholopes.weightrack.feature.session.ui.logger

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import java.time.LocalDate

/** A stored set of this exercise in the session. [number] is its position in the list, from 1 (S40-R23), not its `order`. */
data class SetRowUi(val setId: Long, val number: Int, val weightGrams: Int, val reps: Int)

/** The set that was just saved, for the live-region announcement "Serie 3 guardada: 60 kilogramos, 10 repeticiones." (S40-R27). */
data class SavedSet(val number: Int, val weightGrams: Int, val reps: Int)

/** What the logger shows of the exercise: the fixed note is read from the database and follows its edits (SF02-R13, D13). */
data class LoggerExerciseUi(
    val id: Long,
    val name: String,
    val equipment: Equipment,
    val notes: String?,
    val archived: Boolean,
)

/**
 * The weight (in grams) or the repetitions in the panel (SF02-R15). [value] is `null` until the user fixes it: the
 * number then reads "—" and "Fija el peso" (Spec 42, "Sin valor propuesto"). [window] is the range of the slider,
 * which does not move while dragging. [canDecrease] and [canIncrease] are `false` at the limits, where the
 * button is disabled (S42-R19). Both buttons start from the center of the window while there is no value.
 */
data class ValueInput(
    val value: Int?,
    val window: ValueWindow,
    val canDecrease: Boolean,
    val canIncrease: Boolean,
) {
    val isFixed: Boolean get() = value != null
}

/** Which of the two values a typing dialog writes. */
enum class ValueField { WEIGHT, REPS }

/**
 * The "Escribe el peso" or "Escribe las repeticiones" dialog. [initialText] is what the field starts with (the
 * weight with a comma, no unit; empty when not fixed) and the text the user types stays in the dialog. [error]
 * is the reason the last confirmed text was rejected; the dialog stays open and the value does not change.
 */
data class TypingDialog(val field: ValueField, val initialText: String, val error: TypedValueError? = null)

/** Confirmations the logger opens by itself (S40-R42). */
sealed interface LoggerDialog {
    /** "¿Eliminar esta serie?": asks to confirm deleting the set number [setNumber]. */
    data class DeleteSet(val setNumber: Int) : LoggerDialog

    /** "¿Descartar los cambios?" (S40-R40). The screen leaves when the user confirms. */
    data object Discard : LoggerDialog
}

/** Why "Guardar serie" did not save; the message goes next to the panel (S40-R05). */
sealed interface LoggerFieldError {
    /** "Fija el peso para guardar la serie." */
    data object WeightMissing : LoggerFieldError

    /** "Fija las repeticiones para guardar la serie." */
    data object RepsMissing : LoggerFieldError

    /** The domain rejected the set; there is one text per [ExerciseSetError] (S30-R24). */
    data class RejectedSet(val error: ExerciseSetError) : LoggerFieldError

    /** The domain rejected the session that the first set would create. */
    data class RejectedSession(val error: WorkoutSessionError) : LoggerFieldError
}

/**
 * State of the set logger, S42-P02 (SF02-R12 to SF02-R16). The screen shows its content only when [isLoading] is
 * `false` (S42-R25). Fields marked "effect" are one-shot (S30-R28): the screen runs them and calls the matching
 * `on...Handled()`.
 */
data class SetLoggerUiState(
    val isLoading: Boolean = true,
    /** `null` while loading, or when the exercise no longer exists. */
    val exercise: LoggerExerciseUi? = null,
    /** The day the screen was opened, from the clock; it does not change after midnight (SF02-R12). */
    val today: LocalDate,
    /** The date of the session the sets go to, always visible (S40-R23); [today] while there is no session yet. */
    val sessionDate: LocalDate,
    /** The session the logger writes to; `null` until the first set creates it when opened with `sessionId = 0L`. */
    val resolvedSessionId: Long? = null,
    /** The sets of this exercise in the resolved session, by `order` (the spec calls them "today sets"). */
    val todaySets: List<SetRowUi> = emptyList(),
    val weight: ValueInput = notFixed(weightWindow(WEIGHT_DEFAULT_REFERENCE_GRAMS)),
    val reps: ValueInput = notFixed(repsWindow(REPS_DEFAULT_REFERENCE)),
    /** The set being corrected; `null` while the panel adds a new set (S42-R17). */
    val editingSetId: Long? = null,
    /** The values differ from the initial ones; the proposed values count as initial (S40-R40). */
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val fieldError: LoggerFieldError? = null,
    val typingDialog: TypingDialog? = null,
    val confirmDialog: LoggerDialog? = null,
    /** Effect: a set was saved; the live region announces it (S40-R27). No snackbar (S40-R43). */
    val savedAnnouncement: SavedSet? = null,
    /** Effect: an unexpected failure happened; what was typed stays and "Guardar serie" retries (SF02-R22, S30-R23). */
    val genericError: Boolean = false,
) {
    /**
     * `true` when the exercise is archived and no set is being corrected: the panel offers no new set and shows
     * "Este ejercicio está archivado. Desarchívalo en Archivados para registrar series." (SF02-R16, S40-R29).
     */
    val showsArchivedNotice: Boolean get() = exercise?.archived == true && editingSetId == null
}

private fun notFixed(window: ValueWindow) = ValueInput(value = null, window = window, canDecrease = true, canIncrease = true)

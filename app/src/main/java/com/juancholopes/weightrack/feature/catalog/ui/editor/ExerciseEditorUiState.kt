package com.juancholopes.weightrack.feature.catalog.ui.editor

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.Muscle

/** A muscle chosen in the form; the list keeps the order in which the user chose them (S40-R16). */
data class MuscleChoice(val muscle: Muscle, val isPrimary: Boolean)

/** The archived exercise that already holds the name typed when creating (SF01-R05, S40-R17). */
data class ArchivedConflict(val exerciseId: Long, val name: String)

/** The exercise ended archived and the shell shows a notice; [name] is the stored name (SF01-R06, SF01-R07). */
data class ArchivedResult(val exerciseId: Long, val name: String, val wasDeleteRequest: Boolean)

/** Dialogs the editor opens by itself; the "archived name" dialog is [ExerciseEditorUiState.archivedConflict]. */
sealed interface EditorDialog {
    /** Asks to confirm deleting the exercise named [exerciseName] (SF01-R06, S40-F06). */
    data class Delete(val exerciseName: String) : EditorDialog

    /** Asks to discard the unsaved changes (SF01-R08, S42-R06). */
    data object Discard : EditorDialog
}

/**
 * State of the exercise editor, S42-P03 (SF01-R01). Fields marked "effect" are one-shot (S30-R28): the
 * screen runs them and calls the matching `on...Handled()`.
 */
data class ExerciseEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val equipment: Equipment? = null,
    val muscles: List<MuscleChoice> = emptyList(),
    val notes: String = "",
    /** `null` while creating or loading, so the screen offers neither archive nor delete (SF01-R06). */
    val hasSets: Boolean? = null,
    val fieldError: ExerciseError? = null,
    val archivedConflict: ArchivedConflict? = null,
    val confirmDialog: EditorDialog? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    /** Effect: the screen moves focus to the field with [fieldError] (SF01-R04, S40-R47). */
    val focusOnError: Boolean = false,
    /** Effect: the exercise was saved, or unarchived from [archivedConflict] (SF01-R07). */
    val savedExerciseId: Long? = null,
    /** Effect: archive or delete ended archived (SF01-R07). */
    val archivedResult: ArchivedResult? = null,
    /** Effect: the exercise was deleted; holds its name (SF01-R07). */
    val deletedResult: String? = null,
    /** Effect: an unexpected failure happened; the form keeps what was typed (SF01-R09, S30-R23). */
    val genericError: Boolean = false,
)

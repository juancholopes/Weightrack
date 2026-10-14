package com.juancholopes.weightrack.feature.catalog.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseValidation
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.domain.nameKey
import com.juancholopes.weightrack.feature.catalog.domain.normalizeExerciseName
import com.juancholopes.weightrack.feature.catalog.domain.validateExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val NEW_EXERCISE_ID = 0L
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What the form shows and edits: the user's values, or the stored ones until the first edit. */
private data class FormValues(
    val name: String = "",
    val equipment: Equipment? = null,
    val muscles: List<MuscleChoice> = emptyList(),
    val notes: String = "",
) {
    /** The order in which muscles were chosen is not a change (SF01-R08). */
    fun isSameAs(other: FormValues): Boolean =
        name == other.name && equipment == other.equipment && notes == other.notes &&
            muscles.toSet() == other.muscles.toSet()
}

/** The values the form starts from, and whether the exercise has sets; for a new exercise, the proposed values. */
private data class StoredExercise(val values: FormValues, val hasSets: Boolean?)

/** Everything the editor remembers besides what the database holds (S30-R26: private and changed with `update`). */
private data class FormState(
    /** `null` until the first edit: the form follows the database (SF01 plan, "ViewModel del editor"). */
    val values: FormValues? = null,
    /** The catalog read by the last failed save; `null` before the first failed attempt (SF01-R04). */
    val checkedAgainst: List<Exercise>? = null,
    /** A rejection the repository reported after the domain accepted the form (S20-R41); cleared by the next edit. */
    val rejection: ExerciseError? = null,
    val archivedConflict: ArchivedConflict? = null,
    val confirmDialog: EditorDialog? = null,
    val isSaving: Boolean = false,
    val focusOnError: Boolean = false,
    val savedExerciseId: Long? = null,
    val archivedResult: ArchivedResult? = null,
    val deletedResult: String? = null,
    val genericError: Boolean = false,
) {
    /** One write at a time, and none once the editor already ended with a result (SF01-R09, S40-R07). */
    val canWrite: Boolean
        get() = !isSaving && savedExerciseId == null && archivedResult == null && deletedResult == null
}

/**
 * Create and edit form of an exercise, S42-P03 (SF01-R02 to SF01-R09). [exerciseId] is `0L` when creating
 * and [prefillName] is the proposed name; a new editor reads nothing until it saves (S30-R26).
 */
class ExerciseEditorViewModel(
    private val exerciseId: Long,
    prefillName: String,
    private val exercises: ExerciseRepository,
) : ViewModel() {

    private val isNew = exerciseId == NEW_EXERCISE_ID

    private val form = MutableStateFlow(FormState())

    private val proposed = StoredExercise(FormValues(name = prefillName), hasSets = null)

    /** Private so that events can read the stored values without depending on `uiState` being up to date. */
    private val stored: StateFlow<StoredExercise?> = (if (isNew) flowOf(proposed) else observeStored())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), proposed.takeIf { isNew })

    val uiState: StateFlow<ExerciseEditorUiState> =
        combine(form, stored) { formState, storedExercise -> toUiState(formState, storedExercise) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                toUiState(form.value, stored.value),
            )

    fun onNameChange(name: String) {
        edit { it.copy(name = name) }
    }

    fun onEquipmentSelect(equipment: Equipment) {
        edit { it.copy(equipment = equipment) }
    }

    /** S40-R16: the first muscle chosen is primary; a muscle is never chosen twice. */
    fun onMuscleToggle(muscle: Muscle) {
        edit { values ->
            val chosen = values.muscles
            val next = if (chosen.any { it.muscle == muscle }) {
                chosen.filterNot { it.muscle == muscle }
            } else {
                chosen + MuscleChoice(muscle, isPrimary = chosen.isEmpty())
            }
            values.copy(muscles = next)
        }
    }

    fun onPrimaryChange(muscle: Muscle, isPrimary: Boolean) {
        edit { values ->
            values.copy(muscles = values.muscles.map { if (it.muscle == muscle) it.copy(isPrimary = isPrimary) else it })
        }
    }

    fun onNotesChange(notes: String) {
        edit { it.copy(notes = notes) }
    }

    /** SF01-R02, SF01-R03: validates with the domain and saves; a rejection writes nothing (SF01-R04). */
    fun onSaveClick() {
        write {
            val values = currentValues() ?: return@write
            val catalog = exercises.observeActiveCatalog().first() + exercises.observeArchived().first()
            when (val validation = values.validate(catalog)) {
                is ExerciseValidation.Invalid -> rejectSave(values, validation.error, catalog)
                is ExerciseValidation.Valid -> persist(validation)
            }
        }
    }

    /** SF01-R05: unarchives the exercise that holds the typed name and ends as a save with its id. */
    fun onUnarchiveConflictClick() {
        val conflict = form.value.archivedConflict ?: return
        write {
            exercises.unarchive(conflict.exerciseId)
            form.update { it.copy(archivedConflict = null, savedExerciseId = conflict.exerciseId) }
        }
    }

    fun onUseOtherNameClick() {
        form.update { it.copy(archivedConflict = null) }
    }

    /** "Archivar ejercicio": acts at once; the domain decides the outcome (SF01-R06, S20-R40). */
    fun onArchiveClick() {
        archiveOrDelete(wasDeleteRequest = false)
    }

    fun onDeleteClick() {
        val name = stored.value?.values?.name ?: return
        form.update { it.copy(confirmDialog = EditorDialog.Delete(name)) }
    }

    fun onDeleteConfirm() {
        form.update { it.copy(confirmDialog = null) }
        archiveOrDelete(wasDeleteRequest = true)
    }

    fun onDiscardRequest() {
        form.update { it.copy(confirmDialog = EditorDialog.Discard) }
    }

    fun onDialogDismiss() {
        form.update { it.copy(confirmDialog = null) }
    }

    fun onErrorFocusHandled() {
        form.update { it.copy(focusOnError = false) }
    }

    fun onSavedHandled() {
        form.update { it.copy(savedExerciseId = null) }
    }

    fun onArchivedHandled() {
        form.update { it.copy(archivedResult = null) }
    }

    fun onDeletedHandled() {
        form.update { it.copy(deletedResult = null) }
    }

    fun onGenericErrorHandled() {
        form.update { it.copy(genericError = false) }
    }

    private fun observeStored(): Flow<StoredExercise> = combine(
        exercises.observeExercise(exerciseId).filterNotNull(),
        exercises.observeMuscles(exerciseId),
        exercises.observeHasSets(exerciseId),
    ) { exercise, muscles, hasSets ->
        val values = FormValues(
            name = exercise.name,
            equipment = exercise.equipment,
            muscles = muscles.map { MuscleChoice(it.muscle, it.isPrimary) },
            notes = exercise.notes.orEmpty(),
        )
        StoredExercise(values, hasSets)
    }

    private fun toUiState(formState: FormState, storedExercise: StoredExercise?): ExerciseEditorUiState {
        if (storedExercise == null) return ExerciseEditorUiState(isLoading = true, isNew = isNew)
        val values = formState.values ?: storedExercise.values
        return ExerciseEditorUiState(
            isLoading = false,
            isNew = isNew,
            name = values.name,
            equipment = values.equipment,
            muscles = values.muscles,
            notes = values.notes,
            hasSets = storedExercise.hasSets,
            fieldError = fieldErrorOf(formState, values),
            archivedConflict = formState.archivedConflict,
            confirmDialog = formState.confirmDialog,
            hasUnsavedChanges = formState.values?.isSameAs(storedExercise.values) == false,
            isSaving = formState.isSaving,
            focusOnError = formState.focusOnError,
            savedExerciseId = formState.savedExerciseId,
            archivedResult = formState.archivedResult,
            deletedResult = formState.deletedResult,
            genericError = formState.genericError,
        )
    }

    /**
     * SF01-R04: no error before the first failed attempt; after it, the error follows the corrections by
     * validating again against the catalog that attempt read (D4). While the archived-name dialog is open,
     * the dialog is the feedback.
     */
    private fun fieldErrorOf(formState: FormState, values: FormValues): ExerciseError? {
        val checkedAgainst = formState.checkedAgainst
        return when {
            formState.archivedConflict != null -> null
            formState.rejection != null -> formState.rejection
            checkedAgainst != null -> (values.validate(checkedAgainst) as? ExerciseValidation.Invalid)?.error
            else -> null
        }
    }

    private fun FormValues.validate(existing: List<Exercise>): ExerciseValidation = validateExercise(
        id = exerciseId,
        name = name,
        equipment = equipment,
        muscles = muscles.map { ExerciseMuscle(exerciseId, it.muscle, it.isPrimary) },
        notes = notes,
        existing = existing,
    )

    private fun currentValues(): FormValues? = form.value.values ?: stored.value?.values

    private fun edit(change: (FormValues) -> FormValues) {
        val base = currentValues() ?: return
        form.update { it.copy(values = change(it.values ?: base), rejection = null) }
    }

    private fun rejectSave(values: FormValues, error: ExerciseError, catalog: List<Exercise>) {
        val conflict = archivedConflictFor(values, error, catalog)
        form.update { it.copy(checkedAgainst = catalog, archivedConflict = conflict, focusOnError = conflict == null) }
    }

    /** SF01-R05, S40-R17: only when creating; an edit to an archived name gets the error and no offer. */
    private fun archivedConflictFor(values: FormValues, error: ExerciseError, catalog: List<Exercise>): ArchivedConflict? {
        if (!isNew || error != ExerciseError.NAME_TAKEN_BY_ARCHIVED) return null
        val key = nameKey(normalizeExerciseName(values.name))
        val holder = catalog.firstOrNull { it.archived && nameKey(it.name) == key } ?: return null
        return ArchivedConflict(holder.id, holder.name)
    }

    private suspend fun persist(valid: ExerciseValidation.Valid) {
        val result = if (isNew) {
            exercises.create(valid.exercise, valid.muscles)
        } else {
            exercises.update(valid.exercise, valid.muscles)
        }
        when (result) {
            is ExerciseWriteResult.Saved -> form.update { it.copy(savedExerciseId = result.id) }
            is ExerciseWriteResult.Rejected -> form.update { it.copy(rejection = result.error, focusOnError = true) }
        }
    }

    /** Both actions call `delete`; the domain decides between archiving and deleting (S20-R40, S40-F06). */
    private fun archiveOrDelete(wasDeleteRequest: Boolean) {
        write {
            when (val deletion = exercises.delete(exerciseId)) {
                is ExerciseDeletion.Archive -> form.update {
                    it.copy(archivedResult = ArchivedResult(exerciseId, deletion.exercise.name, wasDeleteRequest))
                }
                is ExerciseDeletion.Delete -> form.update { it.copy(deletedResult = deletion.exercise.name) }
            }
        }
    }

    /**
     * Runs one write at a time (SF01-R09). An unexpected exception becomes [ExerciseEditorUiState.genericError]
     * and keeps the form (S30-R23); cancellation is never swallowed.
     */
    private fun write(operation: suspend () -> Unit) {
        if (!form.value.canWrite) return
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                operation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                form.update { it.copy(genericError = true) }
            } finally {
                form.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        fun factory(exerciseId: Long, prefillName: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExerciseEditorViewModel(exerciseId, prefillName, appContainer.exerciseRepository) }
        }
    }
}

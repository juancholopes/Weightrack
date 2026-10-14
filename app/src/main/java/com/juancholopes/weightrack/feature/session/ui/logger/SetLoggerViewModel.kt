package com.juancholopes.weightrack.feature.session.ui.logger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.session.domain.AddSetResult
import com.juancholopes.weightrack.feature.session.domain.AddSetUseCase
import com.juancholopes.weightrack.feature.session.domain.CorrectSetResult
import com.juancholopes.weightrack.feature.session.domain.CorrectSetUseCase
import com.juancholopes.weightrack.feature.session.domain.REPS_MAX
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WEIGHT_MAX_GRAMS
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

private const val NEW_SESSION = 0L
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The weight and the repetitions of a stored set, which the panel proposes for the next one. */
private data class Values(val weightGrams: Int, val reps: Int)

/** The session the logger works on. [date] is `null` when the session is not in the history (yet). */
private data class ResolvedSession(val id: Long?, val date: LocalDate?) {
    /** What `observeLastPerformance` needs to exclude this session and the later ones (S10-R26). */
    val asBefore: WorkoutSession? get() = if (id != null && date != null) WorkoutSession(id, date, notes = null) else null
}

/** What the database says; the ViewModel decides with it as well as showing it. */
private data class LoggerData(
    val exercise: Exercise?,
    val session: ResolvedSession,
    val sets: List<SetRowUi>,
    val lastPerformanceFirstSet: Values?,
)

/** The sets and the last performance of one resolved session. */
private data class SessionData(val session: ResolvedSession, val sets: List<SetRowUi>, val lastFirstSet: Values?)

/** Everything the logger remembers besides what the database holds (S30-R26: private and changed with `update`). */
private data class FormState(
    /** The session the first set created, so that the later sets go to it (SF02-R12). */
    val createdSessionId: Long? = null,
    val editingSetId: Long? = null,
    /** The weight the user fixed in this panel; `null` follows the initial one. */
    val weight: Int? = null,
    val reps: Int? = null,
    /** Where the slider window is centered; `null` follows the initial value. It only moves when the value changes. */
    val weightReference: Int? = null,
    val repsReference: Int? = null,
    val fieldError: LoggerFieldError? = null,
    val typingDialog: TypingDialog? = null,
    val confirmDialog: LoggerDialog? = null,
    val isSaving: Boolean = false,
    val savedAnnouncement: SavedSet? = null,
    val genericError: Boolean = false,
) {
    /** The panel goes back to a new set (or to [editingSetId]) with the proposed values (S42-R17). */
    fun resetPanel(editingSetId: Long?): FormState = copy(
        editingSetId = editingSetId,
        weight = null,
        reps = null,
        weightReference = null,
        repsReference = null,
        fieldError = null,
        typingDialog = null,
        confirmDialog = null,
    )
}

/** The panel as it is now: initial values, what the user changed and the slider windows. */
private class Panel(
    val editingSet: SetRowUi?,
    val initialWeight: Int?,
    val initialReps: Int?,
    val weight: Int?,
    val reps: Int?,
    val weightReference: Int,
    val repsReference: Int,
)

/**
 * Logs, corrects and deletes the sets of one exercise in one session, S42-P02 (SF02-R12 to SF02-R16, SF02-R22).
 * [sessionId] is `0L` when opened from a card: the logger then works on the most recent session of the day it was
 * opened, and the first set creates it if there is none (S42-R11). Any other id is used as it is and never creates a
 * session. It reads nothing until its state is collected (S30-R26) and writes only through use cases (S30-R20).
 */
class SetLoggerViewModel(
    private val exerciseId: Long,
    private val sessionId: Long,
    exercises: ExerciseRepository,
    private val sessions: SessionRepository,
    private val addSet: AddSetUseCase,
    private val correctSet: CorrectSetUseCase,
    clock: Clock,
) : ViewModel() {

    /** The day the screen was opened: it does not change if midnight passes (SF02-R12). */
    private val openingDate: LocalDate = LocalDate.now(clock)

    private val form = MutableStateFlow(FormState())

    /** Private so that events can read the data without depending on `uiState` being up to date (as in F01). */
    private val data: StateFlow<LoggerData?> = combine(
        exercises.observeExercise(exerciseId),
        observeResolvedSession().flatMapLatest { session -> observeSessionData(session) },
    ) { exercise, sessionData ->
        LoggerData(exercise, sessionData.session, sessionData.sets, sessionData.lastFirstSet)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val uiState: StateFlow<SetLoggerUiState> = combine(data, form) { loggerData, formState ->
        toUiState(loggerData, formState)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        toUiState(data.value, form.value),
    )

    // --- Weight ---

    fun onWeightDecrease() {
        stepWeightBy(-1)
    }

    fun onWeightIncrease() {
        stepWeightBy(+1)
    }

    /** The slider moved: only a stop of its window is a value, and the window stays where it is (S42-R18). */
    fun onWeightSlide(grams: Int) {
        val panel = currentPanel() ?: return
        val snapped = weightWindow(panel.weightReference).snap(grams)
        form.update { it.copy(weight = snapped, weightReference = panel.weightReference, fieldError = null) }
    }

    /** The slider was released: the window recenters on the new value. */
    fun onWeightSlideFinished() {
        val weight = currentPanel()?.weight ?: return
        form.update { it.copy(weightReference = weight) }
    }

    /** The big number was tapped: opens "Escribe el peso". */
    fun onWeightClick() {
        val panel = currentPanel() ?: return
        form.update {
            it.copy(typingDialog = TypingDialog(ValueField.WEIGHT, weightInputText(panel.weight)), confirmDialog = null)
        }
    }

    // --- Repetitions ---

    fun onRepsDecrease() {
        stepRepsBy(-1)
    }

    fun onRepsIncrease() {
        stepRepsBy(+1)
    }

    fun onRepsSlide(reps: Int) {
        val panel = currentPanel() ?: return
        val snapped = repsWindow(panel.repsReference).snap(reps)
        form.update { it.copy(reps = snapped, repsReference = panel.repsReference, fieldError = null) }
    }

    fun onRepsSlideFinished() {
        val reps = currentPanel()?.reps ?: return
        form.update { it.copy(repsReference = reps) }
    }

    fun onRepsClick() {
        val panel = currentPanel() ?: return
        form.update {
            it.copy(typingDialog = TypingDialog(ValueField.REPS, repsInputText(panel.reps)), confirmDialog = null)
        }
    }

    // --- Typing dialog ---

    /** The user edited the text: the error of the last attempt no longer applies. */
    fun onTypedTextChange() {
        form.update { state -> state.copy(typingDialog = state.typingDialog?.copy(error = null)) }
    }

    /**
     * "Fijar peso" or "Fijar repeticiones": a valid text sets the value, closes the dialog and recenters the window;
     * an invalid one keeps the dialog open with its error and changes nothing (S42-R19, S40-R08).
     */
    fun onTypedValueConfirm(text: String) {
        val dialog = form.value.typingDialog ?: return
        when (val parsed = if (dialog.field == ValueField.WEIGHT) parseWeightInput(text) else parseRepsInput(text)) {
            is ParsedValue.Invalid -> form.update { state ->
                state.copy(typingDialog = state.typingDialog?.copy(error = parsed.error))
            }
            is ParsedValue.Valid -> form.update { state ->
                val fixed = if (dialog.field == ValueField.WEIGHT) {
                    state.copy(weight = parsed.value, weightReference = parsed.value)
                } else {
                    state.copy(reps = parsed.value, repsReference = parsed.value)
                }
                fixed.copy(typingDialog = null, fieldError = null)
            }
        }
    }

    fun onTypingDismiss() {
        form.update { it.copy(typingDialog = null) }
    }

    // --- Sets ---

    /** Tapping a set puts the panel in correction mode with its values (S42-R17). */
    fun onSetClick(setId: Long) {
        val loggerData = data.value ?: return
        if (form.value.isSaving || loggerData.sets.none { it.setId == setId }) return
        form.update { it.resetPanel(editingSetId = setId) }
    }

    /** "Cancelar corrección": back to a new set with the proposed values; nothing is written. */
    fun onCancelCorrectionClick() {
        form.update { it.resetPanel(editingSetId = null) }
    }

    /**
     * "Guardar serie" or "Guardar cambios". A value that is not fixed writes nothing and asks for it (S40-R26); the
     * use cases validate the rest and a rejection is a field error. An archived exercise gets no new sets (S40-R29).
     */
    fun onSaveClick() {
        val loggerData = data.value ?: return
        val exercise = loggerData.exercise ?: return
        if (form.value.isSaving) return
        val panel = panelOf(loggerData, form.value)
        val editing = panel.editingSet
        if (editing == null && exercise.archived) return
        val weight = panel.weight
        val reps = panel.reps
        when {
            weight == null -> form.update { it.copy(fieldError = LoggerFieldError.WeightMissing) }
            reps == null -> form.update { it.copy(fieldError = LoggerFieldError.RepsMissing) }
            editing != null -> saveCorrection(loggerData, editing, weight, reps)
            else -> saveNewSet(loggerData, weight, reps)
        }
    }

    /** "Eliminar serie": asks to confirm; only while a set is being corrected. */
    fun onDeleteSetClick() {
        val editing = currentPanel()?.editingSet ?: return
        form.update { it.copy(confirmDialog = LoggerDialog.DeleteSet(editing.number)) }
    }

    /** Deletes the set directly (ADR-30-05); the session stays even if it has no sets left (S10-R24). */
    fun onDeleteSetConfirm() {
        if (form.value.confirmDialog !is LoggerDialog.DeleteSet) return
        val editing = currentPanel()?.editingSet
        form.update { it.copy(confirmDialog = null) }
        if (editing == null) return
        write {
            sessions.deleteSet(editing.setId)
            form.update { it.resetPanel(editingSetId = null) }
        }
    }

    // --- Dialogs and effects ---

    /** Back with unsaved changes: the screen asks "¿Descartar los cambios?" (S40-R40). */
    fun onDiscardRequest() {
        form.update { it.copy(confirmDialog = LoggerDialog.Discard) }
    }

    fun onDialogDismiss() {
        form.update { it.copy(confirmDialog = null) }
    }

    fun onSavedAnnouncementHandled() {
        form.update { it.copy(savedAnnouncement = null) }
    }

    fun onGenericErrorHandled() {
        form.update { it.copy(genericError = false) }
    }

    // --- Reads ---

    /** SF02-R12: the session of the opening day (the one with the higher `id`), the one created, or the one given. */
    private fun observeResolvedSession(): Flow<ResolvedSession> = combine(
        sessions.observeHistory(),
        form.map { it.createdSessionId }.distinctUntilChanged(),
    ) { history, createdSessionId ->
        val id = when {
            sessionId != NEW_SESSION -> sessionId
            createdSessionId != null -> createdSessionId
            else -> history.firstOrNull { it.date == openingDate }?.id
        }
        val date = history.firstOrNull { it.id == id }?.date ?: openingDate.takeIf { id != null && id == createdSessionId }
        ResolvedSession(id, date)
    }.distinctUntilChanged()

    /** SF02-R14: the sets of this exercise in the session and the last performance before it, to propose values. */
    private fun observeSessionData(session: ResolvedSession): Flow<SessionData> = combine(
        session.id?.let { sessions.observeSessionSets(it) } ?: flowOf(emptyList()),
        sessions.observeLastPerformance(exerciseId, session.asBefore),
    ) { pairs, lastPerformance ->
        val rows = pairs
            .filter { (set, _) -> set.exerciseId == exerciseId }
            .mapIndexed { index, (set, _) -> SetRowUi(set.id, number = index + 1, weightGrams = set.weightGrams, reps = set.reps) }
        val lastFirst = lastPerformance?.sets?.firstOrNull()?.let { Values(it.weightGrams, it.reps) }
        SessionData(session, rows, lastFirst)
    }

    // --- The panel ---

    private fun currentPanel(): Panel? = data.value?.let { panelOf(it, form.value) }

    /**
     * SF02-R14, SF02-R16: the initial values are those of the set being corrected, or else the last set of this
     * session, or else the first set of the last performance (D5). A bodyweight exercise without any of them starts
     * with 0 kg of additional weight, fixed (S40-R26).
     */
    private fun panelOf(loggerData: LoggerData, formState: FormState): Panel {
        val editingSet = formState.editingSetId?.let { id -> loggerData.sets.firstOrNull { it.setId == id } }
        val proposal = loggerData.sets.lastOrNull()?.let { Values(it.weightGrams, it.reps) } ?: loggerData.lastPerformanceFirstSet
        val bodyweightDefault = if (loggerData.exercise?.equipment == Equipment.BODYWEIGHT) 0 else null
        val initialWeight = editingSet?.weightGrams ?: proposal?.weightGrams ?: bodyweightDefault
        val initialReps = editingSet?.reps ?: proposal?.reps
        return Panel(
            editingSet = editingSet,
            initialWeight = initialWeight,
            initialReps = initialReps,
            weight = formState.weight ?: initialWeight,
            reps = formState.reps ?: initialReps,
            weightReference = formState.weightReference ?: initialWeight ?: WEIGHT_DEFAULT_REFERENCE_GRAMS,
            repsReference = formState.repsReference ?: initialReps ?: REPS_DEFAULT_REFERENCE,
        )
    }

    private fun toUiState(loggerData: LoggerData?, formState: FormState): SetLoggerUiState {
        if (loggerData == null) return SetLoggerUiState(today = openingDate, sessionDate = openingDate)
        val panel = panelOf(loggerData, formState)
        val weightFrom = panel.weight ?: weightCenter(panel.weightReference)
        val repsFrom = panel.reps ?: repsCenter(panel.repsReference)
        return SetLoggerUiState(
            isLoading = false,
            exercise = loggerData.exercise?.let { LoggerExerciseUi(it.id, it.name, it.equipment, it.notes, it.archived) },
            today = openingDate,
            sessionDate = loggerData.session.date ?: openingDate,
            resolvedSessionId = loggerData.session.id,
            todaySets = loggerData.sets,
            weight = ValueInput(
                value = panel.weight,
                window = weightWindow(panel.weightReference),
                canDecrease = weightFrom > 0,
                canIncrease = weightFrom < WEIGHT_MAX_GRAMS,
            ),
            reps = ValueInput(
                value = panel.reps,
                window = repsWindow(panel.repsReference),
                canDecrease = repsFrom > REPS_MIN,
                canIncrease = repsFrom < REPS_MAX,
            ),
            editingSetId = panel.editingSet?.setId,
            hasUnsavedChanges = panel.weight != panel.initialWeight || panel.reps != panel.initialReps,
            isSaving = formState.isSaving,
            fieldError = formState.fieldError,
            typingDialog = formState.typingDialog,
            confirmDialog = formState.confirmDialog,
            savedAnnouncement = formState.savedAnnouncement,
            genericError = formState.genericError,
        )
    }

    // --- Writes ---

    /** "−" and "+" of the weight: 0,25 kg, from the center of the window while there is no value (Spec 42). */
    private fun stepWeightBy(direction: Int) {
        val panel = currentPanel() ?: return
        val from = panel.weight ?: weightCenter(panel.weightReference)
        val next = stepWeight(from, direction)
        if (next == from && panel.weight != null) return
        form.update { it.copy(weight = next, weightReference = next, fieldError = null) }
    }

    private fun stepRepsBy(direction: Int) {
        val panel = currentPanel() ?: return
        val from = panel.reps ?: repsCenter(panel.repsReference)
        val next = stepReps(from, direction)
        if (next == from && panel.reps != null) return
        form.update { it.copy(reps = next, repsReference = next, fieldError = null) }
    }

    /** SF02-R05: the use case creates the session with the first set; later sets go to it (S42-R11). */
    private fun saveNewSet(loggerData: LoggerData, weight: Int, reps: Int) {
        val targetSession = if (sessionId != NEW_SESSION) sessionId else form.value.createdSessionId ?: loggerData.session.id
        val number = loggerData.sets.size + 1
        write {
            when (val result = addSet(targetSession, openingDate, exerciseId, weight, reps)) {
                is AddSetResult.Saved -> form.update {
                    it.copy(
                        createdSessionId = if (sessionId == NEW_SESSION) result.sessionId else it.createdSessionId,
                        weight = weight,
                        reps = reps,
                        savedAnnouncement = SavedSet(number, weight, reps),
                    )
                }
                is AddSetResult.RejectedSet -> form.update {
                    it.copy(fieldError = LoggerFieldError.RejectedSet(result.error))
                }
                is AddSetResult.RejectedSession -> form.update {
                    it.copy(fieldError = LoggerFieldError.RejectedSession(result.error))
                }
            }
        }
    }

    private fun saveCorrection(loggerData: LoggerData, editing: SetRowUi, weight: Int, reps: Int) {
        val targetSession = checkNotNull(loggerData.session.id) { "A set is being corrected, so the session exists" }
        write {
            when (val result = correctSet(targetSession, editing.setId, weight, reps)) {
                is CorrectSetResult.Saved -> form.update { it.resetPanel(editingSetId = null) }
                is CorrectSetResult.Rejected -> form.update {
                    it.copy(fieldError = LoggerFieldError.RejectedSet(result.error))
                }
            }
        }
    }

    /**
     * Runs one write at a time (S40-R07). An unexpected exception becomes [SetLoggerUiState.genericError] and keeps
     * what was typed (S30-R23); cancellation is never swallowed.
     */
    private fun write(operation: suspend () -> Unit) {
        if (form.value.isSaving) return
        form.update { it.copy(isSaving = true, fieldError = null) }
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
        /** [sessionId] is `0L` when opened from a card (S42-R11). */
        fun factory(exerciseId: Long, sessionId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SetLoggerViewModel(
                    exerciseId = exerciseId,
                    sessionId = sessionId,
                    exercises = appContainer.exerciseRepository,
                    sessions = appContainer.sessionRepository,
                    addSet = appContainer.addSetUseCase,
                    correctSet = appContainer.correctSetUseCase,
                    clock = appContainer.clock,
                )
            }
        }
    }
}

package com.juancholopes.weightrack.feature.catalog.ui.editor

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import com.juancholopes.weightrack.testing.MainDispatcherRule
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    // SF01-A03 / SF01-R02
    @Test
    fun onSaveClick_withValidForm_createsTheNormalizedExercise_andSetsSavedExerciseId() = runTest {
        val viewModel = newEditor()

        viewModel.onNameChange("  Press   de  banca ")
        viewModel.onEquipmentSelect(Equipment.BARBELL)
        viewModel.onMuscleToggle(Muscle.CHEST)
        viewModel.onSaveClick()

        val saved = exercises.observeActiveCatalog().first().single()
        assertEquals(Exercise(saved.id, "Press de banca", Equipment.BARBELL, notes = null), saved)
        assertEquals(listOf(ExerciseMuscle(saved.id, Muscle.CHEST, isPrimary = true)), exercises.observeMuscles(saved.id).first())
        assertEquals(saved.id, viewModel.uiState.value.savedExerciseId)
        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF01-A03 / SF01-R04
    @Test
    fun onSaveClick_withoutEquipment_showsOnlyTheEquipmentError_andWritesNothing() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("Press de banca")
        viewModel.onMuscleToggle(Muscle.CHEST)

        viewModel.onSaveClick()

        assertRejected(viewModel, ExerciseError.EQUIPMENT_MISSING)
        assertEquals("Press de banca", viewModel.uiState.value.name)
    }

    // SF01-A03 / SF01-R04
    @Test
    fun onSaveClick_withoutMuscles_showsOnlyTheMusclesError_andWritesNothing() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("Press de banca")
        viewModel.onEquipmentSelect(Equipment.BARBELL)

        viewModel.onSaveClick()

        assertRejected(viewModel, ExerciseError.MUSCLES_MISSING)
    }

    // SF01-A03 / SF01-R04
    @Test
    fun onSaveClick_withBlankName_showsOnlyTheNameError_andWritesNothing() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("   ")
        viewModel.onEquipmentSelect(Equipment.BARBELL)
        viewModel.onMuscleToggle(Muscle.CHEST)

        viewModel.onSaveClick()

        assertRejected(viewModel, ExerciseError.NAME_BLANK)
        assertEquals("   ", viewModel.uiState.value.name)
    }

    // SF01-R04, S40-R05: after a failed attempt the error follows the corrections
    @Test
    fun fieldError_afterAFailedSave_updatesWhileTheUserCorrects() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("Press de banca")
        viewModel.onSaveClick()
        assertEquals(ExerciseError.EQUIPMENT_MISSING, viewModel.uiState.value.fieldError)

        viewModel.onEquipmentSelect(Equipment.BARBELL)
        assertEquals(ExerciseError.MUSCLES_MISSING, viewModel.uiState.value.fieldError)

        viewModel.onMuscleToggle(Muscle.CHEST)
        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF01-R04: the error is not shown before the first attempt
    @Test
    fun fieldError_beforeAnySaveAttempt_isNull() = runTest {
        val viewModel = newEditor()

        viewModel.onNameChange("Press de banca")

        assertNull(viewModel.uiState.value.fieldError)
        assertFalse(viewModel.uiState.value.focusOnError)
    }

    // SF01-R04, S40-R47: focus goes to the field with the error once
    @Test
    fun onErrorFocusHandled_clearsTheFocusRequest_andKeepsTheError() = runTest {
        val viewModel = newEditor()
        viewModel.onSaveClick()
        assertTrue(viewModel.uiState.value.focusOnError)

        viewModel.onErrorFocusHandled()

        assertFalse(viewModel.uiState.value.focusOnError)
        assertEquals(ExerciseError.EQUIPMENT_MISSING, viewModel.uiState.value.fieldError)
    }

    // SF01-A04 / SF01-R02, S40-A13 / S40-R16
    @Test
    fun onMuscleToggle_firstChoiceIsPrimary_andAMuscleIsNeverChosenTwice() = runTest {
        val viewModel = newEditor()

        viewModel.onMuscleToggle(Muscle.CHEST)
        viewModel.onMuscleToggle(Muscle.TRICEPS)
        assertEquals(
            listOf(MuscleChoice(Muscle.CHEST, isPrimary = true), MuscleChoice(Muscle.TRICEPS, isPrimary = false)),
            viewModel.uiState.value.muscles,
        )

        viewModel.onMuscleToggle(Muscle.CHEST)
        assertEquals(listOf(MuscleChoice(Muscle.TRICEPS, isPrimary = false)), viewModel.uiState.value.muscles)

        viewModel.onPrimaryChange(Muscle.TRICEPS, isPrimary = true)
        assertEquals(listOf(MuscleChoice(Muscle.TRICEPS, isPrimary = true)), viewModel.uiState.value.muscles)
    }

    // SF01-A05 / SF01-R05, S40-A14
    @Test
    fun onSaveClick_withNameOfAnActiveExercise_showsNameTaken_andWritesNothing() = runTest {
        val bench = createExercise("Press de banca")
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "press DE banca")

        viewModel.onSaveClick()

        assertEquals(ExerciseError.NAME_TAKEN, viewModel.uiState.value.fieldError)
        assertNull(viewModel.uiState.value.archivedConflict)
        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
        assertNull(viewModel.uiState.value.savedExerciseId)
    }

    // SF01-A06 / SF01-R05, S40-A15
    @Test
    fun onUnarchiveConflictClick_unarchivesTheExerciseUnchanged_andSetsSavedExerciseIdToIt() = runTest {
        val bench = createExercise("Press de banca", Equipment.BARBELL, notes = "Agarre medio", archived = true)
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "Press de banca", equipment = Equipment.DUMBBELL)

        viewModel.onSaveClick()
        assertEquals(ArchivedConflict(bench.id, "Press de banca"), viewModel.uiState.value.archivedConflict)
        assertNull(viewModel.uiState.value.fieldError)

        viewModel.onUnarchiveConflictClick()

        assertEquals(listOf(bench.copy(archived = false)), exercises.observeActiveCatalog().first())
        assertEquals(listOf(Muscle.CHEST), exercises.observeMuscles(bench.id).first().map { it.muscle })
        assertEquals(bench.id, viewModel.uiState.value.savedExerciseId)
        assertNull(viewModel.uiState.value.archivedConflict)
    }

    // SF01-A06 / SF01-R05, S40-A15
    @Test
    fun onUseOtherNameClick_closesTheDialog_andKeepsTheForm() = runTest {
        createExercise("Press de banca", archived = true)
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "Press de banca", equipment = Equipment.DUMBBELL)
        viewModel.onSaveClick()

        viewModel.onUseOtherNameClick()

        val state = viewModel.uiState.value
        assertNull(state.archivedConflict)
        assertEquals("Press de banca", state.name)
        assertEquals(Equipment.DUMBBELL, state.equipment)
        assertEquals(listOf(MuscleChoice(Muscle.CHEST, isPrimary = true)), state.muscles)
        assertEquals(ExerciseError.NAME_TAKEN_BY_ARCHIVED, state.fieldError)
        assertEquals(1, exercises.observeArchived().first().size)
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())
    }

    // SF01-A06 / SF01-R05, S40-A15: editing never offers to unarchive
    @Test
    fun onSaveClick_whenEditingToTheNameOfAnArchivedExercise_showsOnlyTheError() = runTest {
        createExercise("Press de banca", archived = true)
        val squat = createExercise("Sentadilla")
        val viewModel = newEditor(exerciseId = squat.id)

        viewModel.onNameChange("Press de banca")
        viewModel.onSaveClick()

        assertEquals(ExerciseError.NAME_TAKEN_BY_ARCHIVED, viewModel.uiState.value.fieldError)
        assertNull(viewModel.uiState.value.archivedConflict)
        assertEquals(squat, exercises.observeExercise(squat.id).first())
    }

    // SF01-A07 / SF01-R06, SF01-R07, S40-A16
    @Test
    fun onArchiveClick_withSets_archivesWithoutDialog_andSetsArchivedResult() = runTest {
        val bench = createExercise("Press de banca")
        addSet(bench.id)
        val viewModel = newEditor(exerciseId = bench.id)
        assertEquals(true, viewModel.uiState.value.hasSets)

        viewModel.onArchiveClick()

        assertNull(viewModel.uiState.value.confirmDialog)
        assertEquals(ArchivedResult(bench.id, "Press de banca", wasDeleteRequest = false), viewModel.uiState.value.archivedResult)
        assertEquals(bench.copy(archived = true), exercises.observeExercise(bench.id).first())
    }

    // SF01-A07 / SF01-R06, SF01-R07, S40-A16
    @Test
    fun onDeleteClick_withoutSets_asksFirst_andDeletesOnlyAfterConfirming() = runTest {
        val row = createExercise("Remo con mancuerna", Equipment.DUMBBELL)
        val viewModel = newEditor(exerciseId = row.id)
        assertEquals(false, viewModel.uiState.value.hasSets)

        viewModel.onDeleteClick()

        assertEquals(EditorDialog.Delete("Remo con mancuerna"), viewModel.uiState.value.confirmDialog)
        assertEquals(row, exercises.observeExercise(row.id).first())

        viewModel.onDeleteConfirm()

        assertNull(exercises.observeExercise(row.id).first())
        assertEquals("Remo con mancuerna", viewModel.uiState.value.deletedResult)
        assertNull(viewModel.uiState.value.confirmDialog)
    }

    // SF01-A07 / SF01-R06: cancelling the dialog changes nothing
    @Test
    fun onDialogDismiss_afterDeleteClick_closesTheDialog_andWritesNothing() = runTest {
        val row = createExercise("Remo con mancuerna", Equipment.DUMBBELL)
        val viewModel = newEditor(exerciseId = row.id)
        viewModel.onDeleteClick()

        viewModel.onDialogDismiss()

        assertNull(viewModel.uiState.value.confirmDialog)
        assertEquals(row, exercises.observeExercise(row.id).first())
    }

    // SF01-A07 / SF01-R06, S40-F06: the exercise gained sets before the confirmation
    @Test
    fun onDeleteConfirm_whenTheExerciseGainedSets_archivesIt_withWasDeleteRequest() = runTest {
        val bench = createExercise("Press de banca")
        val viewModel = newEditor(exerciseId = bench.id)
        viewModel.onDeleteClick()
        addSet(bench.id)

        viewModel.onDeleteConfirm()

        assertEquals(ArchivedResult(bench.id, "Press de banca", wasDeleteRequest = true), viewModel.uiState.value.archivedResult)
        assertNull(viewModel.uiState.value.deletedResult)
        assertEquals(bench.copy(archived = true), exercises.observeExercise(bench.id).first())
    }

    // SF01-A08 / SF01-R03, SF01-R08
    @Test
    fun uiState_inEditMode_loadsTheStoredValues_withoutUnsavedChanges() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            notes = "Agarre medio",
            muscles = listOf(Muscle.TRICEPS to false, Muscle.CHEST to true),
        )

        val state = newEditor(exerciseId = bench.id).uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isNew)
        assertEquals("Press de banca", state.name)
        assertEquals(Equipment.BARBELL, state.equipment)
        assertEquals("Agarre medio", state.notes)
        assertEquals(
            setOf(MuscleChoice(Muscle.CHEST, isPrimary = true), MuscleChoice(Muscle.TRICEPS, isPrimary = false)),
            state.muscles.toSet(),
        )
        assertEquals(false, state.hasSets)
        assertFalse(state.hasUnsavedChanges)
    }

    // SF01-A08 / SF01-R03, S40-R18
    @Test
    fun onSaveClick_inEditMode_keepsTheIdTheSetsAndTheArchivedState() = runTest {
        val bench = createExercise("Press de banca", archived = true)
        addSet(bench.id)
        val viewModel = newEditor(exerciseId = bench.id)

        viewModel.onNameChange("Press plano")
        viewModel.onSaveClick()

        assertEquals(bench.id, viewModel.uiState.value.savedExerciseId)
        assertEquals(bench.copy(name = "Press plano"), exercises.observeExercise(bench.id).first())
        assertTrue(exercises.observeHasSets(bench.id).first())
    }

    // SF01-A08 / SF01-R08, S40-R40: the prefilled name counts as initial
    @Test
    fun hasUnsavedChanges_withPrefillName_followsTheDifferenceFromTheInitialValues() = runTest {
        val viewModel = newEditor(prefillName = "Remo")
        assertEquals("Remo", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onNameChange("Remo con barra")
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onNameChange("Remo")
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // SF01-R08, S40-R40
    @Test
    fun hasUnsavedChanges_inEditMode_isTrueAfterAChange() = runTest {
        val bench = createExercise("Press de banca")
        val viewModel = newEditor(exerciseId = bench.id)

        viewModel.onMuscleToggle(Muscle.TRICEPS)

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    // SF01-R08, S42-R06
    @Test
    fun onDiscardRequest_opensTheDiscardDialog_andDismissClosesIt() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("Press")

        viewModel.onDiscardRequest()
        assertEquals(EditorDialog.Discard, viewModel.uiState.value.confirmDialog)

        viewModel.onDialogDismiss()
        assertNull(viewModel.uiState.value.confirmDialog)
        assertEquals("Press", viewModel.uiState.value.name)
    }

    // SF01-A09 / SF01-R09, S40-A38
    @Test
    fun onSaveClick_whenTheWriteFails_setsGenericError_keepsTheForm_andARetrySaves() = runTest {
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "Press de banca")
        exercises.failNextWrite()

        viewModel.onSaveClick()

        val failed = viewModel.uiState.value
        assertTrue(failed.genericError)
        assertFalse(failed.isSaving)
        assertNull(failed.savedExerciseId)
        assertEquals("Press de banca", failed.name)
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())

        viewModel.onGenericErrorHandled()
        assertFalse(viewModel.uiState.value.genericError)
        viewModel.onSaveClick()

        val saved = exercises.observeActiveCatalog().first().single()
        assertEquals(saved.id, viewModel.uiState.value.savedExerciseId)
    }

    // SF01-A09 / SF01-R09, S40-R07
    @Test
    fun onSaveClick_twice_createsASingleExercise() = runTest {
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "Press de banca")

        viewModel.onSaveClick()
        viewModel.onSaveClick()

        assertEquals(1, exercises.observeActiveCatalog().first().size)
        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF01-R07, S30-R28: each effect is cleared once handled
    @Test
    fun onSavedHandled_clearsTheEffect() = runTest {
        val viewModel = newEditor()
        fillValidForm(viewModel, name = "Press de banca")
        viewModel.onSaveClick()

        viewModel.onSavedHandled()

        assertNull(viewModel.uiState.value.savedExerciseId)
    }

    // S30-R26: a new editor reads nothing until it saves
    @Test
    fun uiState_inNewMode_isReadyWithoutReadingTheCatalog() = runTest {
        val state = newEditor(prefillName = "Curl").uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.isNew)
        assertNull(state.hasSets)
        assertEquals("Curl", state.name)
    }

    /** Builds the editor and starts an empty collector, since `uiState` comes from `stateIn` (S31-R11). */
    private fun TestScope.newEditor(exerciseId: Long = 0L, prefillName: String = ""): ExerciseEditorViewModel {
        val viewModel = ExerciseEditorViewModel(exerciseId, prefillName, exercises)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun fillValidForm(
        viewModel: ExerciseEditorViewModel,
        name: String,
        equipment: Equipment = Equipment.BARBELL,
    ) {
        viewModel.onNameChange(name)
        viewModel.onEquipmentSelect(equipment)
        viewModel.onMuscleToggle(Muscle.CHEST)
    }

    private suspend fun assertRejected(viewModel: ExerciseEditorViewModel, error: ExerciseError) {
        val state = viewModel.uiState.value
        assertEquals(error, state.fieldError)
        assertTrue(state.focusOnError)
        assertNull(state.savedExerciseId)
        assertFalse(state.isSaving)
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())
    }

    private suspend fun createExercise(
        name: String,
        equipment: Equipment = Equipment.BARBELL,
        notes: String? = null,
        archived: Boolean = false,
        muscles: List<Pair<Muscle, Boolean>> = listOf(Muscle.CHEST to true),
    ): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = equipment, notes = notes, archived = archived)
        val result = exercises.create(exercise, muscles.map { (muscle, isPrimary) -> ExerciseMuscle(0, muscle, isPrimary) })
        return exercise.copy(id = (result as ExerciseWriteResult.Saved).id)
    }

    private suspend fun addSet(exerciseId: Long) {
        sessions.createSession(
            WorkoutSession(id = 0, date = LocalDate.of(2026, 10, 10), notes = null),
            listOf(ExerciseSet(id = 0, sessionId = 0, exerciseId = exerciseId, order = 1, weightGrams = 60_000, reps = 10)),
        )
    }
}

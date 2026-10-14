package com.juancholopes.weightrack.feature.catalog.ui.editor

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
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

/**
 * Paths of [ExerciseEditorViewModel] that `ExerciseEditorViewModelTest` leaves out: notes, the repository
 * rejecting a write the domain accepted, failures of archive and delete, loading, and the effects of
 * archive and delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditorViewModelEdgeCasesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    // SF01-R02, S10-R12
    @Test
    fun onSaveClick_withNotes_savesThemTrimmed() = runTest {
        val viewModel = newEditor()
        viewModel.onNameChange("Press de banca")
        viewModel.onEquipmentSelect(Equipment.BARBELL)
        viewModel.onMuscleToggle(Muscle.CHEST)
        viewModel.onNotesChange("  Agarre medio ")
        assertEquals("  Agarre medio ", viewModel.uiState.value.notes)

        viewModel.onSaveClick()

        assertEquals("Agarre medio", exercises.observeActiveCatalog().first().single().notes)
    }

    // SF01-R04, S20-R41: the unique index is the second barrier when the catalog read by the save was stale
    @Test
    fun onSaveClick_whenTheRepositoryRejectsTheWrite_showsThatError_untilTheNextEdit() = runTest {
        val rejecting = object : ExerciseRepository by exercises {
            override suspend fun create(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult =
                ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN)
        }
        val viewModel = newEditor(repository = rejecting)
        viewModel.onNameChange("Press de banca")
        viewModel.onEquipmentSelect(Equipment.BARBELL)
        viewModel.onMuscleToggle(Muscle.CHEST)

        viewModel.onSaveClick()

        val rejected = viewModel.uiState.value
        assertEquals(ExerciseError.NAME_TAKEN, rejected.fieldError)
        assertTrue(rejected.focusOnError)
        assertNull(rejected.savedExerciseId)
        assertFalse(rejected.isSaving)

        viewModel.onNameChange("Press plano")
        assertNull(viewModel.uiState.value.fieldError)
    }

    // SF01-R09, S30-R23: archiving or deleting fails like any other write
    @Test
    fun onDeleteConfirm_whenTheWriteFails_setsGenericError_andKeepsTheExercise() = runTest {
        val row = createExercise("Remo con mancuerna")
        val viewModel = newEditor(exerciseId = row.id)
        viewModel.onDeleteClick()
        exercises.failNextWrite()

        viewModel.onDeleteConfirm()

        val failed = viewModel.uiState.value
        assertTrue(failed.genericError)
        assertFalse(failed.isSaving)
        assertNull(failed.deletedResult)
        assertNull(failed.confirmDialog)
        assertEquals(row, exercises.observeExercise(row.id).first())

        viewModel.onGenericErrorHandled()
        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()
        assertEquals("Remo con mancuerna", viewModel.uiState.value.deletedResult)
    }

    // SF01-R07, S30-R28
    @Test
    fun onArchivedHandled_andOnDeletedHandled_clearTheirEffects() = runTest {
        val bench = createExercise("Press de banca")
        addSet(bench.id)
        val archiving = newEditor(exerciseId = bench.id)
        archiving.onArchiveClick()
        assertEquals(ArchivedResult(bench.id, "Press de banca", wasDeleteRequest = false), archiving.uiState.value.archivedResult)

        archiving.onArchivedHandled()

        assertNull(archiving.uiState.value.archivedResult)

        val row = createExercise("Remo con mancuerna")
        val deleting = newEditor(exerciseId = row.id)
        deleting.onDeleteClick()
        deleting.onDeleteConfirm()
        assertEquals("Remo con mancuerna", deleting.uiState.value.deletedResult)

        deleting.onDeletedHandled()

        assertNull(deleting.uiState.value.deletedResult)
    }

    // SF01-R03, S42-R25: an editor of an existing exercise has nothing to show until its data arrives
    @Test
    fun uiState_inEditMode_beforeTheDataArrives_isLoading() = runTest {
        val bench = createExercise("Press de banca")

        val state = ExerciseEditorViewModel(bench.id, "", exercises).uiState.value

        assertTrue(state.isLoading)
        assertFalse(state.isNew)
        assertNull(state.hasSets)
    }

    private fun TestScope.newEditor(
        exerciseId: Long = 0L,
        repository: ExerciseRepository = exercises,
    ): ExerciseEditorViewModel {
        val viewModel = ExerciseEditorViewModel(exerciseId, "", repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun createExercise(name: String): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = Equipment.BARBELL, notes = null)
        val result = exercises.create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true)))
        return exercise.copy(id = (result as ExerciseWriteResult.Saved).id)
    }

    private suspend fun addSet(exerciseId: Long) {
        sessions.createSession(
            WorkoutSession(id = 0, date = LocalDate.of(2026, 10, 10), notes = null),
            listOf(ExerciseSet(id = 0, sessionId = 0, exerciseId = exerciseId, order = 1, weightGrams = 60_000, reps = 10)),
        )
    }
}

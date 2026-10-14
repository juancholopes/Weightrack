package com.juancholopes.weightrack.feature.catalog.ui.archived

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
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
class ArchivedExercisesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    // SF01-A13 / SF01-R10, S42-R25: the empty state needs the first data, so the screen can tell loading apart
    @Test
    fun uiState_beforeTheFirstData_isLoading_andHasNoItems() {
        val viewModel = ArchivedExercisesViewModel(exercises)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertEquals(emptyList<Exercise>(), state.items)
        assertNull(state.notice)
    }

    // SF01-A10 / SF01-R10, S10-R31
    @Test
    fun uiState_listsOnlyTheArchivedExercises_sortedByName() = runTest {
        createExercise("Sentadilla", archived = true)
        val curl = createExercise("curl", archived = true)
        createExercise("Press de banca")
        val viewModel = ArchivedExercisesViewModel(exercises)

        collect(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("curl", "Sentadilla"), state.items.map { it.name })
        assertEquals(curl, state.items.first())
    }

    // SF01-A13 / SF01-R10, S42-R25: with nothing archived the data still arrives, so the screen may show its empty state
    @Test
    fun uiState_withNoArchivedExercises_isReadyAndEmpty() = runTest {
        createExercise("Press de banca")
        val viewModel = ArchivedExercisesViewModel(exercises)

        collect(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<Exercise>(), viewModel.uiState.value.items)
    }

    // SF01-A10 / SF01-R10, S40-A17
    @Test
    fun onUnarchiveClick_removesTheRow_returnsTheExerciseToTheCatalogWithItsSets_andSetsTheNotice() = runTest {
        val bench = createExercise("Press de banca", archived = true)
        val squat = createExercise("Sentadilla", archived = true)
        addSet(bench.id)
        val viewModel = ArchivedExercisesViewModel(exercises)
        collect(viewModel)

        viewModel.onUnarchiveClick(bench.id)

        val state = viewModel.uiState.value
        assertEquals(listOf(squat), state.items)
        assertEquals("Press de banca", state.notice)
        assertEquals(listOf(bench.copy(archived = false)), exercises.observeActiveCatalog().first())
        assertTrue(exercises.observeHasSets(bench.id).first())
    }

    // SF01-A10 / SF01-R10, S30-R28: each effect is cleared once handled
    @Test
    fun onNoticeHandled_clearsTheNotice() = runTest {
        val bench = createExercise("Press de banca", archived = true)
        val viewModel = ArchivedExercisesViewModel(exercises)
        collect(viewModel)
        viewModel.onUnarchiveClick(bench.id)

        viewModel.onNoticeHandled()

        assertNull(viewModel.uiState.value.notice)
    }

    // SF01-R10, S40-R44: a second tap on the same row, while it still shows, finds nothing left to do
    @Test
    fun onUnarchiveClick_forAnExerciseThatIsNotArchived_setsNoNotice() = runTest {
        val bench = createExercise("Press de banca", archived = true)
        val viewModel = ArchivedExercisesViewModel(exercises)
        collect(viewModel)
        viewModel.onUnarchiveClick(bench.id)
        viewModel.onNoticeHandled()

        viewModel.onUnarchiveClick(bench.id)

        assertNull(viewModel.uiState.value.notice)
        assertFalse(viewModel.uiState.value.genericError)
    }

    // SF01-R10, S30-R23, S40-R06
    @Test
    fun onUnarchiveClick_whenTheWriteFails_setsGenericError_keepsTheRow_andARetryWorks() = runTest {
        val bench = createExercise("Press de banca", archived = true)
        val viewModel = ArchivedExercisesViewModel(exercises)
        collect(viewModel)
        exercises.failNextWrite()

        viewModel.onUnarchiveClick(bench.id)

        val failed = viewModel.uiState.value
        assertTrue(failed.genericError)
        assertNull(failed.notice)
        assertEquals(listOf(bench), failed.items)

        viewModel.onGenericErrorHandled()
        assertFalse(viewModel.uiState.value.genericError)
        viewModel.onUnarchiveClick(bench.id)

        assertEquals(emptyList<Exercise>(), viewModel.uiState.value.items)
        assertEquals("Press de banca", viewModel.uiState.value.notice)
    }

    /** `stateIn(WhileSubscribed)` only reads the repository while someone collects (S31-R11, D8). */
    private fun TestScope.collect(viewModel: ArchivedExercisesViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private suspend fun createExercise(name: String, archived: Boolean = false): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = Equipment.BARBELL, notes = null, archived = archived)
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

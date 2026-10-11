package com.juancholopes.weightrack.ui

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The temporary start screen reads the catalog through the container chain (Spec F00, D2 changed). */
@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exercises = FakeExerciseRepository()

    // SF00-A15 / SF00-R16
    @Test
    fun uiState_beforeAnyCollector_isLoading() {
        val viewModel = StartViewModel(exercises)

        assertEquals(StartUiState.Loading, viewModel.uiState.value)
    }

    // SF00-A15 / SF00-R16
    @Test
    fun uiState_withActiveAndArchivedExercises_countsOnlyActiveOnes() = runTest {
        createExercise("Press de banca")
        createExercise("Curl")
        createExercise("Sentadilla", archived = true)
        val viewModel = StartViewModel(exercises)

        collect(viewModel)

        assertEquals(StartUiState.Ready(activeExerciseCount = 2), viewModel.uiState.value)
    }

    // SF00-A15 / SF00-R16
    @Test
    fun uiState_afterAnExerciseIsCreated_updatesTheCount() = runTest {
        val viewModel = StartViewModel(exercises)
        collect(viewModel)
        assertEquals(StartUiState.Ready(activeExerciseCount = 0), viewModel.uiState.value)

        createExercise("Press de banca")

        assertEquals(StartUiState.Ready(activeExerciseCount = 1), viewModel.uiState.value)
    }

    /** `stateIn(WhileSubscribed)` only reads the repository while someone collects (S31-R11, D8). */
    private fun TestScope.collect(viewModel: StartViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private suspend fun createExercise(name: String, archived: Boolean = false) {
        exercises.create(
            Exercise(id = 0, name = name, equipment = Equipment.MACHINE, notes = null, archived = archived),
            emptyList(),
        )
    }
}

package com.juancholopes.weightrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** State of the temporary start screen (Spec F00, D2 changed by the coordinator). */
sealed interface StartUiState {
    data object Loading : StartUiState
    data class Ready(val activeExerciseCount: Int) : StartUiState
}

/**
 * Shows how many active exercises the catalog has. It is the smallest screen that follows the screen
 * convention of the constitution and proves the chain container, factory, ViewModel and `StateFlow`
 * (SF00-R16). F02 deletes it with `StartScreen.kt`.
 */
class StartViewModel(exercises: ExerciseRepository) : ViewModel() {

    val uiState: StateFlow<StartUiState> = exercises.observeActiveCatalog()
        .map { catalog -> StartUiState.Ready(activeExerciseCount = catalog.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StartUiState.Loading)

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { StartViewModel(appContainer.exerciseRepository) }
        }
    }
}

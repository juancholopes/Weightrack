package com.juancholopes.weightrack.feature.catalog.ui.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The one-shot effects of the screen, kept apart from the database data (S30-R28). */
private data class PendingEffects(val notice: String? = null, val genericError: Boolean = false)

/** Lists the archived exercises and unarchives them at once (SF01-R10, S40-F07). */
class ArchivedExercisesViewModel(private val exercises: ExerciseRepository) : ViewModel() {

    private val effects = MutableStateFlow(PendingEffects())

    val uiState: StateFlow<ArchivedExercisesUiState> = combine(exercises.observeArchived(), effects) { items, pending ->
        ArchivedExercisesUiState(
            isLoading = false,
            items = items,
            notice = pending.notice,
            genericError = pending.genericError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ArchivedExercisesUiState())

    /**
     * Unarchives the exercise and sets the notice with its name; the row leaves the list because the list
     * is observed. An exercise that is no longer archived, such as after a second tap, does nothing.
     */
    fun onUnarchiveClick(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val exercise = exercises.observeArchived().first().firstOrNull { it.id == exerciseId }
                    ?: return@launch
                exercises.unarchive(exerciseId)
                effects.update { it.copy(notice = exercise.name) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                effects.update { it.copy(genericError = true) }
            }
        }
    }

    fun onNoticeHandled() {
        effects.update { it.copy(notice = null) }
    }

    fun onGenericErrorHandled() {
        effects.update { it.copy(genericError = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ArchivedExercisesViewModel(appContainer.exerciseRepository) }
        }
    }
}

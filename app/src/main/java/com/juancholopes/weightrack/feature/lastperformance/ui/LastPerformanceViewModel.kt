package com.juancholopes.weightrack.feature.lastperformance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.session.domain.LastPerformance
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The "Última vez" of one exercise as seen from one session, S42-P02 (SF03-R03). With a [sessionId] it shows the most
 * recent session before that one, so the sets just logged never appear (S10-R26); without one (no session of today
 * yet), the most recent session. It only reads (no events) and reads nothing until its state is collected (S30-R26).
 * [clock] gives the day that lets the screen say "Hoy" or "Ayer" (S30-R15).
 */
class LastPerformanceViewModel(
    private val exerciseId: Long,
    private val sessionId: Long?,
    private val sessions: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<LastPerformanceUiState> = observeLastPerformance()
        .map { lastPerformance -> toUiState(lastPerformance) }
        // S30-R23: a failed read is a state; `catch` never swallows the cancellation of the collector.
        .catch { emit(LastPerformanceUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LastPerformanceUiState.Loading)

    /**
     * SF03-R03: the session is read first and the query is asked "before" it, again whenever the session changes.
     * A session that does not exist (deleted while the logger is open) is read as no session.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLastPerformance(): Flow<LastPerformance?> =
        if (sessionId == null) {
            sessions.observeLastPerformance(exerciseId, before = null)
        } else {
            sessions.observeSession(sessionId).flatMapLatest { session ->
                sessions.observeLastPerformance(exerciseId, before = session)
            }
        }

    private fun toUiState(lastPerformance: LastPerformance?): LastPerformanceUiState =
        if (lastPerformance == null) {
            LastPerformanceUiState.FirstTime
        } else {
            LastPerformanceUiState.Shown(
                date = lastPerformance.session.date,
                today = LocalDate.now(clock),
                sets = lastPerformance.sets.map { SetValues(it.weightGrams, it.reps) },
            )
        }

    companion object {
        /** One ViewModel per exercise and session: the section keys it with both (SF03-R04, S30-R16). */
        fun factory(exerciseId: Long, sessionId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LastPerformanceViewModel(
                    exerciseId = exerciseId,
                    sessionId = sessionId,
                    sessions = appContainer.sessionRepository,
                    clock = appContainer.clock,
                )
            }
        }
    }
}

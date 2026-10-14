package com.juancholopes.weightrack.feature.lastperformance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The "Última vez" section the shell puts in the hole of the set logger, S42-P02 (SF03-R01, SF03-R02). It is the
 * `Screen` of this feature: it gets the ViewModel, collects its state and calls [LastPerformanceContent].
 *
 * [sessionId] is the session the logger resolved, or `null` while there is none yet (S10-R26, SF03-R03). The ViewModel
 * is keyed by the exercise and the session, so that when the first set creates the session and [sessionId] goes from
 * `null` to an id, the section switches to a new ViewModel and does not show the sets just logged (SF03-R04). Until
 * F05, [onOpenProgress] is `null` and "Ver estadísticas" is not shown (S42-R24).
 */
@Composable
fun LastPerformanceSection(
    exerciseId: Long,
    sessionId: Long?,
    onOpenProgress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: LastPerformanceViewModel = viewModel(
        key = "lastPerformance:$exerciseId:$sessionId",
        factory = LastPerformanceViewModel.factory(exerciseId, sessionId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LastPerformanceContent(uiState = uiState, onOpenProgress = onOpenProgress, modifier = modifier)
}

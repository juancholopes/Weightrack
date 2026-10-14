package com.juancholopes.weightrack.feature.catalog.ui.archived

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen of the archived exercises list, S42-P04 (SF01-R01). It gets the ViewModel, collects its state and
 * connects the events. Going back is a navigation the user starts with no business logic, so it goes
 * from the composable to [onBack] without passing through the ViewModel (S30-R29).
 */
@Composable
fun ArchivedExercisesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedExercisesViewModel = viewModel(factory = ArchivedExercisesViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArchivedExercisesContent(
        uiState = uiState,
        onUnarchiveClick = viewModel::onUnarchiveClick,
        onNoticeHandled = viewModel::onNoticeHandled,
        onGenericErrorHandled = viewModel::onGenericErrorHandled,
        onBack = onBack,
        modifier = modifier,
    )
}

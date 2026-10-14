package com.juancholopes.weightrack.feature.session.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juancholopes.weightrack.ui.PendingNotice

/**
 * Screen of the home, S42-P01 (SF02-R06 to SF02-R11). It gets the ViewModel, collects its state and hands the
 * notice the shell kept to it: the ViewModel puts it in its `UiState`, and the screen then tells the shell it
 * was consumed (S42-R09, D6; no `Channel` or `SharedFlow`, S30-R28). Navigation arrives as lambdas, never as
 * the stack (S30-R39).
 *
 * @param notice what another screen left to show here, or `null`.
 * @param onNoticeConsumed the notice reached the ViewModel; the shell can forget it.
 * @param onOpenLogger a card was tapped: the set logger of that exercise, on the session of today.
 * @param onCreateExercise the exercise editor, with a name to propose and whether to log after saving.
 * @param onOpenProgress `null` until F05.
 * @param onOpenHistory `null` until F04.
 */
@Composable
fun HomeScreen(
    notice: PendingNotice?,
    onNoticeConsumed: () -> Unit,
    onOpenLogger: (Long) -> Unit,
    onCreateExercise: (prefill: String, logAfterSave: Boolean) -> Unit,
    onEditExercise: (Long) -> Unit,
    onEditSession: (Long) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenProgress: ((Long) -> Unit)?,
    onOpenHistory: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(notice) {
        notice?.let {
            viewModel.onNotice(it)
            onNoticeConsumed()
        }
    }

    HomeContent(
        uiState = uiState,
        onTabSelect = viewModel::onTabSelect,
        onQueryChange = viewModel::onQueryChange,
        onClearSearch = viewModel::onClearSearch,
        onMuscleFilterSelect = viewModel::onMuscleFilterSelect,
        onEquipmentFilterSelect = viewModel::onEquipmentFilterSelect,
        onStartAnotherSessionClick = viewModel::onStartAnotherSessionClick,
        onUndoArchiveClick = viewModel::onUndoArchiveClick,
        onNoticeHandled = viewModel::onNoticeHandled,
        onGenericErrorHandled = viewModel::onGenericErrorHandled,
        onOpenLogger = onOpenLogger,
        onCreateExercise = onCreateExercise,
        onEditExercise = onEditExercise,
        onEditSession = onEditSession,
        onOpenArchived = onOpenArchived,
        onOpenProgress = onOpenProgress,
        onOpenHistory = onOpenHistory,
        modifier = modifier,
    )
}

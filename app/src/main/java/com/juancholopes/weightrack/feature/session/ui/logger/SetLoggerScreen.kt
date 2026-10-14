package com.juancholopes.weightrack.feature.session.ui.logger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen of the set logger, S42-P02 (SF02-R13). It collects the state of [viewModel] and connects its events to
 * [SetLoggerContent]. Navigation arrives as lambdas, never as the stack (S30-R39), and the ViewModel is created by
 * the entry that owns this screen, with the exercise and the session of its key (S30-R37).
 *
 * @param onBack leave the screen: the arrow, or "Descartar" in the discard dialog.
 * @param onEditExercise opens the exercise editor without logging after saving; the logger sees the new name and
 * note because it observes them in the database.
 * @param onOpenProgress `null` until F05 (S42-R24).
 * @param lastPerformance the slot of F03 (S42-R16, D2 of Spec 30), filled by the shell with the resolved session.
 */
@Composable
fun SetLoggerScreen(
    onBack: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenProgress: ((Long) -> Unit)?,
    lastPerformance: @Composable (exerciseId: Long, sessionId: Long?, modifier: Modifier) -> Unit,
    viewModel: SetLoggerViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SetLoggerContent(
        uiState = uiState,
        onWeightDecrease = viewModel::onWeightDecrease,
        onWeightIncrease = viewModel::onWeightIncrease,
        onWeightSlide = viewModel::onWeightSlide,
        onWeightSlideFinished = viewModel::onWeightSlideFinished,
        onWeightClick = viewModel::onWeightClick,
        onRepsDecrease = viewModel::onRepsDecrease,
        onRepsIncrease = viewModel::onRepsIncrease,
        onRepsSlide = viewModel::onRepsSlide,
        onRepsSlideFinished = viewModel::onRepsSlideFinished,
        onRepsClick = viewModel::onRepsClick,
        onTypedTextChange = viewModel::onTypedTextChange,
        onTypedValueConfirm = viewModel::onTypedValueConfirm,
        onTypingDismiss = viewModel::onTypingDismiss,
        onSetClick = viewModel::onSetClick,
        onCancelCorrectionClick = viewModel::onCancelCorrectionClick,
        onSaveClick = viewModel::onSaveClick,
        onDeleteSetClick = viewModel::onDeleteSetClick,
        onDeleteSetConfirm = viewModel::onDeleteSetConfirm,
        onDiscardRequest = viewModel::onDiscardRequest,
        onDialogDismiss = viewModel::onDialogDismiss,
        onSavedAnnouncementHandled = viewModel::onSavedAnnouncementHandled,
        onGenericErrorHandled = viewModel::onGenericErrorHandled,
        onBack = onBack,
        onEditExercise = onEditExercise,
        onOpenProgress = onOpenProgress,
        lastPerformance = lastPerformance,
        modifier = modifier,
    )
}

package com.juancholopes.weightrack.feature.catalog.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen of the exercise editor, S42-P03 (SF01-R01). It gets the ViewModel, collects its state and runs
 * the navigation effects: each result is reported to the shell once the write has ended and then marked
 * as handled (S30-R28, S30-R35). Navigation arrives as lambdas, never as the stack (S30-R39).
 *
 * @param exerciseId `0L` to create a new exercise (the value of `ExerciseEditorKey`).
 * @param prefillName the proposed name when creating; empty for none.
 * @param onSaved the exercise was created or edited, or unarchived from the archived-name dialog.
 * @param onArchived the exercise ended archived (archive action, or a delete that found sets).
 * @param onDeleted the exercise was deleted; carries its name.
 * @param onClose the user left without saving; nothing was written.
 */
@Composable
fun ExerciseEditorScreen(
    exerciseId: Long,
    prefillName: String,
    onSaved: (Long) -> Unit,
    onArchived: (ArchivedResult) -> Unit,
    onDeleted: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseEditorViewModel = viewModel(
        factory = ExerciseEditorViewModel.factory(exerciseId, prefillName),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedExerciseId) {
        uiState.savedExerciseId?.let { id ->
            onSaved(id)
            viewModel.onSavedHandled()
        }
    }
    LaunchedEffect(uiState.archivedResult) {
        uiState.archivedResult?.let { result ->
            onArchived(result)
            viewModel.onArchivedHandled()
        }
    }
    LaunchedEffect(uiState.deletedResult) {
        uiState.deletedResult?.let { name ->
            onDeleted(name)
            viewModel.onDeletedHandled()
        }
    }

    ExerciseEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEquipmentSelect = viewModel::onEquipmentSelect,
        onMuscleToggle = viewModel::onMuscleToggle,
        onPrimaryChange = viewModel::onPrimaryChange,
        onNotesChange = viewModel::onNotesChange,
        onSaveClick = viewModel::onSaveClick,
        onArchiveClick = viewModel::onArchiveClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onUnarchiveConflictClick = viewModel::onUnarchiveConflictClick,
        onUseOtherNameClick = viewModel::onUseOtherNameClick,
        onDiscardRequest = viewModel::onDiscardRequest,
        onDialogDismiss = viewModel::onDialogDismiss,
        onErrorFocusHandled = viewModel::onErrorFocusHandled,
        onGenericErrorHandled = viewModel::onGenericErrorHandled,
        onClose = onClose,
        modifier = modifier,
    )
}

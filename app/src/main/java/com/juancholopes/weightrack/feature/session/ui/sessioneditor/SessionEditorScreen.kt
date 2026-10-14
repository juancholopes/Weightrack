package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen of the data of a session, S42-P05 (SF02-R19). It gets the ViewModel, collects its state and runs the
 * navigation effect: the save is reported to the shell once the write has ended and then marked as handled
 * (S30-R28, S30-R35). Navigation arrives as lambdas, never as the stack (S30-R39).
 *
 * @param sessionId the session whose date and note are edited (the value of `SessionEditorKey`).
 * @param onSaved the session was saved: the shell removes the form.
 * @param onClose the user left without saving; nothing was written.
 */
@Composable
fun SessionEditorScreen(
    sessionId: Long,
    onSaved: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionEditorViewModel = viewModel(factory = SessionEditorViewModel.factory(sessionId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onSaved()
            viewModel.onSavedHandled()
        }
    }

    SessionEditorContent(
        uiState = uiState,
        onDateClick = viewModel::onDateClick,
        onDatePickerDismiss = viewModel::onDatePickerDismiss,
        onDateSelected = viewModel::onDateSelected,
        onNotesChange = viewModel::onNotesChange,
        onSaveClick = viewModel::onSaveClick,
        onDiscardRequest = viewModel::onDiscardRequest,
        onDialogDismiss = viewModel::onDialogDismiss,
        onGenericErrorHandled = viewModel::onGenericErrorHandled,
        onClose = onClose,
        modifier = modifier,
    )
}

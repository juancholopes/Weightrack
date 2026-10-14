package com.juancholopes.weightrack.feature.session.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.session.ui.home.HomeScreen
import com.juancholopes.weightrack.feature.session.ui.logger.SetLoggerScreen
import com.juancholopes.weightrack.feature.session.ui.logger.SetLoggerViewModel
import com.juancholopes.weightrack.feature.session.ui.sessioneditor.SessionEditorScreen
import com.juancholopes.weightrack.ui.PendingNotice

/**
 * Registers the screens of the session feature (SF02-R02, S30-R40). Navigation arrives as lambdas: the feature never
 * sees the stack, and the shell bridge turns each lambda into an operation on it (S30-R39, SF00-R08).
 *
 * Each screen creates its ViewModel inside its entry, so the entry owns it and clears it when its key leaves the
 * stack (S30-R37, skill `navigation-3`).
 *
 * @param notice what another screen left for the home to show (S42-R09, D6). It is read while the entry is
 * composed, so the home sees it when it comes back to the top.
 * @param onNoticeConsumed the home took the notice; the shell can forget it.
 * @param onOpenLogger a card was tapped: opens the logger of that exercise on the session of today.
 * @param onCreateExercise opens the exercise editor with a name to propose and whether to log after saving.
 * @param onEditExerciseFromHome opens the exercise editor from the menu of a card.
 * @param onEditExerciseFromLogger opens the exercise editor from the logger; it does not log after saving (S42-R08).
 * @param onEditSession opens the data of a session.
 * @param onOpenArchived opens the archived exercises.
 * @param onLoggerBack the user left the logger.
 * @param onSessionEditorDone the data of the session was saved, or the user left the form.
 * @param onOpenProgress `null` until F05 (S42-R24).
 * @param onOpenHistory `null` until F04 (S42-R24).
 * @param lastPerformance the slot of F03 (S42-R16); an empty composable until then.
 */
fun EntryProviderScope<NavKey>.sessionEntries(
    notice: () -> PendingNotice?,
    onNoticeConsumed: () -> Unit,
    onOpenLogger: (exerciseId: Long) -> Unit,
    onCreateExercise: (prefill: String, logAfterSave: Boolean) -> Unit,
    onEditExerciseFromHome: (exerciseId: Long) -> Unit,
    onEditExerciseFromLogger: (exerciseId: Long) -> Unit,
    onEditSession: (sessionId: Long) -> Unit,
    onOpenArchived: () -> Unit,
    onLoggerBack: () -> Unit,
    onSessionEditorDone: () -> Unit,
    onOpenProgress: ((exerciseId: Long) -> Unit)?,
    onOpenHistory: (() -> Unit)?,
    lastPerformance: @Composable (exerciseId: Long, sessionId: Long?, modifier: Modifier) -> Unit,
) {
    entry<HomeKey> {
        HomeScreen(
            notice = notice(),
            onNoticeConsumed = onNoticeConsumed,
            onOpenLogger = onOpenLogger,
            onCreateExercise = onCreateExercise,
            onEditExercise = onEditExerciseFromHome,
            onEditSession = onEditSession,
            onOpenArchived = onOpenArchived,
            onOpenProgress = onOpenProgress,
            onOpenHistory = onOpenHistory,
        )
    }
    entry<SetLoggerKey> { key ->
        SetLoggerScreen(
            onBack = onLoggerBack,
            onEditExercise = onEditExerciseFromLogger,
            onOpenProgress = onOpenProgress,
            lastPerformance = lastPerformance,
            viewModel = viewModel(factory = SetLoggerViewModel.factory(key.exerciseId, key.sessionId)),
        )
    }
    entry<SessionEditorKey> { key ->
        SessionEditorScreen(
            sessionId = key.sessionId,
            onSaved = onSessionEditorDone,
            onClose = onSessionEditorDone,
        )
    }
}

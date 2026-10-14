package com.juancholopes.weightrack.ui

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.catalog.ui.ArchivedExercisesKey
import com.juancholopes.weightrack.feature.catalog.ui.ExerciseEditorKey
import com.juancholopes.weightrack.feature.lastperformance.ui.LastPerformanceSection
import com.juancholopes.weightrack.feature.session.ui.HomeKey
import com.juancholopes.weightrack.feature.session.ui.SessionEditorKey
import com.juancholopes.weightrack.feature.session.ui.SetLoggerKey
import com.juancholopes.weightrack.feature.session.ui.sessionEntries

/**
 * Bridge of the session feature (SF00-R08, SF02-R02, S30-R40). It registers the session entries and turns each of
 * their lambdas into an operation on [backStack]. Every push is made from the screen that offers it and only while
 * that screen is on top, so a double tap adds one entry, not two. The shell is the only place that changes the stack.
 *
 * The slot of the last time is F03's [LastPerformanceSection] (SF03-R02). Until F05 and F04 the lambdas of statistics
 * and history are `null`, so their buttons are not shown (S42-R24).
 *
 * @param notice what the editor left for the home (S42-R09). The shell keeps it as saveable state.
 * @param onNoticeConsumed the home received the notice.
 */
fun EntryProviderScope<NavKey>.sessionDestinations(
    backStack: NavBackStack<NavKey>,
    notice: () -> PendingNotice?,
    onNoticeConsumed: () -> Unit,
) {
    sessionEntries(
        notice = notice,
        onNoticeConsumed = onNoticeConsumed,
        onOpenLogger = { exerciseId -> backStack.pushWhenTopIs<HomeKey>(SetLoggerKey(exerciseId)) },
        onCreateExercise = { prefill, logAfterSave ->
            backStack.pushWhenTopIs<HomeKey>(ExerciseEditorKey(prefillName = prefill, logAfterSave = logAfterSave))
        },
        onEditExerciseFromHome = { exerciseId -> backStack.pushWhenTopIs<HomeKey>(ExerciseEditorKey(exerciseId = exerciseId)) },
        onEditExerciseFromLogger = { exerciseId ->
            backStack.pushWhenTopIs<SetLoggerKey>(ExerciseEditorKey(exerciseId = exerciseId))
        },
        onEditSession = { sessionId -> backStack.pushWhenTopIs<HomeKey>(SessionEditorKey(sessionId)) },
        onOpenArchived = { backStack.pushWhenTopIs<HomeKey>(ArchivedExercisesKey) },
        onLoggerBack = { backStack.popTop<SetLoggerKey>() },
        onSessionEditorDone = { backStack.popTop<SessionEditorKey>() },
        onOpenProgress = null,
        onOpenHistory = null,
        lastPerformance = { exerciseId, sessionId, modifier ->
            LastPerformanceSection(exerciseId = exerciseId, sessionId = sessionId, modifier = modifier)
        },
    )
}

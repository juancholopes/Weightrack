package com.juancholopes.weightrack.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.catalog.ui.ArchivedExercisesKey
import com.juancholopes.weightrack.feature.catalog.ui.catalogEntries

/**
 * Bridge of the catalog (SF00-R08, S30-R40). It registers the catalog entries and turns each of their lambdas
 * into an operation on [backStack] with the rules of `EditorResults.kt`. The shell is the only place that
 * changes the stack.
 *
 * @param onNotice receives the notice the editor leaves for the home when it archives or deletes
 * (S42-R09). The shell keeps it as saveable state.
 */
fun EntryProviderScope<NavKey>.catalogDestinations(
    backStack: NavBackStack<NavKey>,
    onNotice: (PendingNotice) -> Unit,
) {
    catalogEntries(
        onEditorSaved = { exerciseId -> backStack.applyEditorSaved(exerciseId) },
        onEditorArchived = { result -> onNotice(backStack.applyEditorArchived(result)) },
        onEditorDeleted = { name -> onNotice(backStack.applyEditorDeleted(name)) },
        onEditorClosed = { backStack.applyEditorClosed() },
        onArchivedBack = { backStack.popTop<ArchivedExercisesKey>() },
    )
}

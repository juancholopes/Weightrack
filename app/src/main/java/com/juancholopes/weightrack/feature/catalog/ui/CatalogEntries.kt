package com.juancholopes.weightrack.feature.catalog.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.catalog.ui.archived.ArchivedExercisesScreen
import com.juancholopes.weightrack.feature.catalog.ui.editor.ArchivedResult
import com.juancholopes.weightrack.feature.catalog.ui.editor.ExerciseEditorScreen

/**
 * Registers the screens of the catalog (SF01-R01, S30-R40). Navigation arrives as lambdas: the feature never
 * sees the stack, and the shell bridge turns each lambda into an operation on it (S30-R39, SF00-R08).
 *
 * Each screen creates its ViewModel inside its entry, so the entry owns it and clears it when the key leaves
 * the stack (S30-R37, skill `navigation-3`).
 *
 * @param onEditorSaved the editor created, edited or unarchived an exercise; carries its id.
 * @param onEditorArchived the editor ended with the exercise archived.
 * @param onEditorDeleted the editor deleted the exercise; carries its name.
 * @param onEditorClosed the user left the editor without saving.
 * @param onArchivedBack the user left the archived list.
 */
fun EntryProviderScope<NavKey>.catalogEntries(
    onEditorSaved: (exerciseId: Long) -> Unit,
    onEditorArchived: (ArchivedResult) -> Unit,
    onEditorDeleted: (name: String) -> Unit,
    onEditorClosed: () -> Unit,
    onArchivedBack: () -> Unit,
) {
    entry<ExerciseEditorKey> { key ->
        ExerciseEditorScreen(
            exerciseId = key.exerciseId,
            prefillName = key.prefillName,
            onSaved = onEditorSaved,
            onArchived = onEditorArchived,
            onDeleted = onEditorDeleted,
            onClose = onEditorClosed,
        )
    }
    entry<ArchivedExercisesKey> {
        ArchivedExercisesScreen(onBack = onArchivedBack)
    }
}

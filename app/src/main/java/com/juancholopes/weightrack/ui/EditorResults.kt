package com.juancholopes.weightrack.ui

import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.feature.catalog.ui.ExerciseEditorKey
import com.juancholopes.weightrack.feature.catalog.ui.editor.ArchivedResult
import com.juancholopes.weightrack.feature.session.ui.SetLoggerKey

/*
 * What the shell does with the stack when the exercise editor ends (S42-R08, S42-R09), and the guards of the
 * catalog flows that open and close its screens. They are pure functions over a list, so they run on the
 * JVM without writing data (S31-R18). The screens report their result once, but each function ignores a
 * stack whose top is not the expected screen, so a repeated report or a second tap can never remove the
 * entry below (the root included) or add an equal key twice.
 */

/**
 * The editor saved or unarchived the exercise [exerciseId] (S42-R08, D5): removes the editor and, when its key had
 * `logAfterSave`, opens the set logger of that exercise in its place. Without it, for example when the editor was
 * opened from the logger, it only removes the editor: the logger below sees the new name and note because it
 * observes them in the database.
 */
fun MutableList<NavKey>.applyEditorSaved(exerciseId: Long) {
    val editor = lastOrNull() as? ExerciseEditorKey ?: return
    removeAt(lastIndex)
    if (editor.logAfterSave) add(SetLoggerKey(exerciseId))
}

/** The user left the editor without saving (S42-R06): removes the editor and nothing else. */
fun MutableList<NavKey>.applyEditorClosed() {
    popTop<ExerciseEditorKey>()
}

/**
 * The exercise ended archived (S42-R09, D6): removes the editor and whatever sits above the root, because an
 * archived exercise gets no new sets (S40-R29). Returns the notice the home shows.
 */
fun MutableList<NavKey>.applyEditorArchived(result: ArchivedResult): PendingNotice {
    popToRoot()
    return PendingNotice.Archived(result.exerciseId, result.name, result.wasDeleteRequest)
}

/**
 * The exercise was deleted (S42-R09, D6): removes the editor and whatever sits above the root, because the
 * exercise no longer exists. Returns the notice the home shows.
 */
fun MutableList<NavKey>.applyEditorDeleted(name: String): PendingNotice {
    popToRoot()
    return PendingNotice.Deleted(name)
}

/** Opens [key] only while the screen on top is a [K]: a double tap on the button that opens it adds one entry. */
internal inline fun <reified K : NavKey> MutableList<NavKey>.pushWhenTopIs(key: NavKey) {
    if (lastOrNull() is K) add(key)
}

/** Removes the top entry only if it is a [K]; any other top, or an empty list, is left as it is. */
internal inline fun <reified K : NavKey> MutableList<NavKey>.popTop() {
    if (lastOrNull() is K) removeAt(lastIndex)
}

/** Removes every entry above the root, which stays. */
private fun MutableList<NavKey>.popToRoot() {
    while (size > 1) removeAt(lastIndex)
}

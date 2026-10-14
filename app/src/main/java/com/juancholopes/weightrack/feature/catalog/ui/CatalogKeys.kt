package com.juancholopes.weightrack.feature.catalog.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Opens the exercise editor, S42-P03 (S42-R07, S30-R38). A key carries only ids and simple values.
 *
 * @property exerciseId `0L` creates a new exercise; any other id edits that exercise.
 * @property prefillName the proposed name when creating; empty for none.
 * @property logAfterSave when `true`, the shell opens the set logger for the exercise after it is saved
 * (S42-R08, D5). The shell applies it from F02, when the logger exists.
 */
@Serializable
data class ExerciseEditorKey(
    val exerciseId: Long = 0L,
    val prefillName: String = "",
    val logAfterSave: Boolean = false,
) : NavKey

/** Opens the list of archived exercises, S42-P04. */
@Serializable
data object ArchivedExercisesKey : NavKey

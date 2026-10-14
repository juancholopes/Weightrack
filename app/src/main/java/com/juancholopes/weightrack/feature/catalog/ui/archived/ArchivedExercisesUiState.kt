package com.juancholopes.weightrack.feature.catalog.ui.archived

import com.juancholopes.weightrack.feature.catalog.domain.Exercise

/**
 * State of the archived exercises list, S42-P04 (SF01-R10). The screen shows its empty state only when
 * `!isLoading && items.isEmpty()`, never before the first data (S42-R25). Fields marked "effect" are
 * one-shot (S30-R28): the screen runs them and calls the matching `on...Handled()`.
 */
data class ArchivedExercisesUiState(
    val isLoading: Boolean = true,
    /** Archived exercises sorted by name (S10-R31). */
    val items: List<Exercise> = emptyList(),
    /** Effect: name of the exercise that just returned to the catalog, for the snackbar (S40-R20). */
    val notice: String? = null,
    /** Effect: an unexpected failure happened while unarchiving (S30-R23, S40-R06). */
    val genericError: Boolean = false,
)

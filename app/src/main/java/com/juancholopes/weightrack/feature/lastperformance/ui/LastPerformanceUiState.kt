package com.juancholopes.weightrack.feature.lastperformance.ui

import java.time.LocalDate

/** The weight and the repetitions of one set of the last performance, as stored (weight in grams, S10-R02). */
data class SetValues(val weightGrams: Int, val reps: Int)

/**
 * State of the "Última vez" section of the set logger, S42-P02 (SF03-R05, SF03-R08). It is read-only, so it has no
 * one-shot effects (S30-R28). The section reserves its height while [Loading] so the panel does not jump (SF03-R05).
 */
sealed interface LastPerformanceUiState {
    /** Before the first read. */
    data object Loading : LastPerformanceUiState

    /** No earlier session has sets of this exercise: "Primera vez con este ejercicio." (S40-R35). */
    data object FirstTime : LastPerformanceUiState

    /**
     * The most recent earlier session with the exercise: its [date] and all the [sets] of the exercise in it, in
     * their `order` (S10-R25). [today] is the day of the clock, so the screen can say "Hoy" or "Ayer" (S42-R26).
     */
    data class Shown(val date: LocalDate, val today: LocalDate, val sets: List<SetValues>) : LastPerformanceUiState

    /** The read failed. The section says so and the logger keeps working (SF03-R08, S30-R23). */
    data object Error : LastPerformanceUiState
}

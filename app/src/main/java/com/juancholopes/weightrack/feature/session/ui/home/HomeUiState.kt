package com.juancholopes.weightrack.feature.session.ui.home

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import java.time.LocalDate

/** The two tabs of the home screen; Hoy is the default (S42-R03). */
enum class HomeTab { TODAY, EXERCISES }

/** The session of today as the header of Hoy shows it (S42-P01). */
data class TodaySessionUi(val sessionId: Long, val date: LocalDate, val notes: String?)

/**
 * One exercise card (SF02-R09, S42-R12). [peakWeightGrams] and [peakDate] are the heaviest weight and the date of
 * the latest session of the exercise, counting today (D11); both are `null` when it has no sets ("Sin series").
 * For a bodyweight exercise the weight is the additional weight, and 0 reads "Sin peso". [setsToday] is the
 * number of sets in the session of today and only has a value for the cards of that section ("{n} series hoy").
 */
data class ExerciseCardUi(
    val exerciseId: Long,
    val name: String,
    /** The first primary muscle in the order of the `Muscle` enum (D2). */
    val primaryMuscle: Muscle?,
    val equipment: Equipment,
    /** Only a card of Hoy can be archived: it carries the "Archivado" mark (S42-P01). */
    val archived: Boolean,
    val peakWeightGrams: Int?,
    val peakDate: LocalDate?,
    val setsToday: Int?,
)

/**
 * A notice the home shows in its `SnackbarHost`, mapped from the shell's `PendingNotice` (SF02-R11). [Archived]
 * offers "Deshacer"; [Deleted] has no action.
 */
sealed interface HomeNotice {
    /** "«Press de banca» archivado", or "«…» tiene series, así que se archivó" when [wasDeleteRequest] (S42-R09). */
    data class Archived(val exerciseId: Long, val name: String, val wasDeleteRequest: Boolean) : HomeNotice

    /** "«Remo con mancuerna» eliminado". */
    data class Deleted(val name: String) : HomeNotice
}

/**
 * State of the home screen, S42-P01 (SF02-R06 to SF02-R11). The screen shows an empty state only when [isLoading]
 * is `false` (S42-R25). Fields marked "effect" are one-shot (S30-R28): the screen runs them and calls the matching
 * `on...Handled()`.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    /** Today by the clock, for "Hoy · dom 11 oct" and the relative dates of the cards. */
    val today: LocalDate,
    val selectedTab: HomeTab = HomeTab.TODAY,
    /** The most recent session if it is dated today; `null` shows "Aún no tienes series hoy. Toca un ejercicio para empezar.". */
    val todaySession: TodaySessionUi? = null,
    /** One card per exercise of the session of today, in the order of its first set. Empty for a session without sets. */
    val todayCards: List<ExerciseCardUi> = emptyList(),
    /** The date of the most recent session before today; `null` when there is none. */
    val lastSessionDate: LocalDate? = null,
    /** "De tu última sesión": the active exercises of that session without sets today. The section is hidden when empty. */
    val lastSessionCards: List<ExerciseCardUi> = emptyList(),
    /** The Ejercicios tab: active exercises by name, already filtered by [query], [muscleFilter] and [equipmentFilter]. */
    val exerciseCards: List<ExerciseCardUi> = emptyList(),
    val query: String = "",
    val muscleFilter: Muscle? = null,
    val equipmentFilter: Equipment? = null,
    /** There are no active exercises at all, whatever the filters ("Aún no tienes ejercicios…", S40-R12). */
    val catalogIsEmpty: Boolean = false,
    /** "Empezar otra sesión" is offered only with a session of today (S40-F09). */
    val canStartAnotherSession: Boolean = false,
    /** Effect: a notice to show in the snackbar (SF02-R11). */
    val notice: HomeNotice? = null,
    /** Effect: an unexpected failure happened (SF02-R22, S30-R23). */
    val genericError: Boolean = false,
)

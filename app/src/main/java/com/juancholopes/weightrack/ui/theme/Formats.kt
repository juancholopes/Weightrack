package com.juancholopes.weightrack.ui.theme

import java.time.LocalDate
import kotlin.math.abs

/*
 * Formats of weights, repetitions, sets and dates (Spec 41 "Formatos", SF02-R20, S41-R14, S42-R26). Pure functions,
 * tested on the JVM. Everything is shown in Spanish and in kilograms with the precision of 0,25 kg. They are the
 * only place that writes these formats by hand: a screen never builds "kg" or a date itself. Each visible format
 * has its reading for TalkBack ("spoken...").
 */

private const val GRAMS_PER_KILOGRAM = 1000

/** The unit of every weight. Screens that draw the unit apart from the number (cards, the big number) read it here. */
const val WEIGHT_UNIT = "kg"

/**
 * The day and month names and the two relative days, read from `strings.xml` by the UI (S41-R14). Weekdays start
 * on Monday and months on January, as `java.time` numbers them. `DateTimeFormatter` is not used because Android
 * and the JVM give different abbreviations ("sept" or "sep.").
 */
data class DateNames(
    val weekdaysShort: List<String>,
    val weekdaysLong: List<String>,
    val monthsShort: List<String>,
    val monthsLong: List<String>,
    val today: String,
    val yesterday: String,
) {
    init {
        require(weekdaysShort.size == WEEKDAYS && weekdaysLong.size == WEEKDAYS) { "A week has $WEEKDAYS days" }
        require(monthsShort.size == MONTHS && monthsLong.size == MONTHS) { "A year has $MONTHS months" }
    }

    private companion object {
        const val WEEKDAYS = 7
        const val MONTHS = 12
    }
}

/**
 * The kilograms of [grams] as a number: decimal comma, no superfluous zeros and no thousands separator
 * ("62,5", "60", "62,25", "1000"). It is what the typing dialog starts with.
 */
fun formatKilograms(grams: Int): String {
    val sign = if (grams < 0) "-" else ""
    val absolute = abs(grams)
    val fraction = (absolute % GRAMS_PER_KILOGRAM).toString().padStart(3, '0').trimEnd('0')
    val whole = absolute / GRAMS_PER_KILOGRAM
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole,$fraction"
}

/** "62,5 kg", "60 kg", "62,25 kg", "1000 kg". */
fun formatWeight(grams: Int): String = "${formatKilograms(grams)} $WEIGHT_UNIT"

/** "62,5 kilogramos"; "1 kilogramo". */
fun spokenWeight(grams: Int): String =
    "${formatKilograms(grams)} ${if (grams == GRAMS_PER_KILOGRAM) "kilogramo" else "kilogramos"}"

/** "10": the number goes next to its label (Spec 41, "Formatos"). */
fun formatReps(reps: Int): String = reps.toString()

/** "10 repeticiones"; "1 repetición". */
fun spokenReps(reps: Int): String = "$reps ${if (reps == 1) "repetición" else "repeticiones"}"

/**
 * "60 kg × 10". With [withUnit] `false`, "60 × 10": inside a list of sets of the same exercise the unit can be
 * left out after the first one (Spec 42, "Formatos").
 */
fun formatSet(weightGrams: Int, reps: Int, withUnit: Boolean = true): String =
    "${if (withUnit) formatWeight(weightGrams) else formatKilograms(weightGrams)} × ${formatReps(reps)}"

/** "60 kilogramos, 10 repeticiones". */
fun spokenSet(weightGrams: Int, reps: Int): String = "${spokenWeight(weightGrams)}, ${spokenReps(reps)}"

/** The additional weight of a bodyweight exercise: "+10 kg", or "Sin peso" when it is 0 (S10-R02). */
fun formatAdditionalWeight(grams: Int): String = if (grams == 0) NO_WEIGHT else "+${formatWeight(grams)}"

/** "10 kilogramos adicionales"; "1 kilogramo adicional"; "Sin peso". */
fun spokenAdditionalWeight(grams: Int): String = when (grams) {
    0 -> NO_WEIGHT
    GRAMS_PER_KILOGRAM -> "${spokenWeight(grams)} adicional"
    else -> "${spokenWeight(grams)} adicionales"
}

private const val NO_WEIGHT = "Sin peso"

/**
 * The short date: "jue 8 oct", and "jue 8 oct 2025" when the year is not the one of [today]. With [relative],
 * [today] and the day before it read "Hoy" and "Ayer" (S42-R26); without it they keep their date, for the headers
 * that already say "Hoy · dom 11 oct" (Spec 42, S42-P02).
 */
fun formatShortDate(date: LocalDate, today: LocalDate, names: DateNames, relative: Boolean = true): String {
    if (relative && date == today) return names.today
    if (relative && date == today.minusDays(1)) return names.yesterday
    val weekday = names.weekdaysShort[date.dayOfWeek.ordinal]
    val month = names.monthsShort[date.monthValue - 1]
    val year = if (date.year == today.year) "" else " ${date.year}"
    return "$weekday ${date.dayOfMonth} $month$year"
}

/** The reading of a date for TalkBack: "jueves 8 de octubre", with " de 2025" when the year is not the current one. */
fun spokenDate(date: LocalDate, today: LocalDate, names: DateNames): String {
    val weekday = names.weekdaysLong[date.dayOfWeek.ordinal]
    val month = names.monthsLong[date.monthValue - 1]
    val year = if (date.year == today.year) "" else " de ${date.year}"
    return "$weekday ${date.dayOfMonth} de $month$year"
}

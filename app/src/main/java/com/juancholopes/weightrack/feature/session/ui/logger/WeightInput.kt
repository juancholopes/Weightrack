package com.juancholopes.weightrack.feature.session.ui.logger

import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import com.juancholopes.weightrack.feature.session.domain.REPS_MAX
import com.juancholopes.weightrack.feature.session.domain.WEIGHT_MAX_GRAMS
import com.juancholopes.weightrack.feature.session.domain.WEIGHT_STEP_GRAMS
import com.juancholopes.weightrack.ui.theme.formatKilograms
import java.math.BigDecimal
import java.math.BigInteger

/*
 * Reading what the user types in the "Escribe el peso" and "Escribe las repeticiones" dialogs (SF02-R15, S42-R19).
 * Nothing is rounded, clipped or corrected: a value that breaks a rule is returned as an error and the dialog
 * stays open (S40-R08). The rules are the ones of the domain (S10-R01 to S10-R03, S10-R11).
 */

private const val GRAMS_PER_KILOGRAM = 1000

/** Why a typed text was not accepted. The UI shows one message per case next to the field. */
sealed interface TypedValueError {
    /** The text is not a number ("Escribe un número, por ejemplo 62,5."). */
    data object NotANumber : TypedValueError

    /** The text is a number that breaks a rule of the domain; [error] says which one. */
    data class Rejected(val error: ExerciseSetError) : TypedValueError
}

/** The result of reading a typed text: the value in grams or repetitions, or why it was not accepted. */
sealed interface ParsedValue {
    data class Valid(val value: Int) : ParsedValue
    data class Invalid(val error: TypedValueError) : ParsedValue
}

private val decimalNumber = Regex("-?(\\d+[.,]?\\d*|[.,]\\d+)")
private val wholeNumber = Regex("-?\\d+")

/**
 * Reads a weight in kilograms with a comma or a point as the decimal mark and returns grams ("62,5" and "62.5"
 * give 62 500). The checks follow the domain order: negative, not a multiple of 0,25 kg, above 1000 kg.
 */
fun parseWeightInput(text: String): ParsedValue {
    val trimmed = text.trim()
    if (!decimalNumber.matches(trimmed)) return notANumber()
    val kilograms = BigDecimal(trimmed.replace(',', '.').let { if (it.endsWith('.')) it.dropLast(1) else it })
    val grams = kilograms.multiply(BigDecimal(GRAMS_PER_KILOGRAM))
    val error = when {
        grams.signum() < 0 -> ExerciseSetError.WEIGHT_NEGATIVE
        !grams.isWholeNumber() || grams.toBigInteger().mod(BigInteger.valueOf(WEIGHT_STEP_GRAMS.toLong())).signum() != 0 ->
            ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP
        grams > BigDecimal(WEIGHT_MAX_GRAMS) -> ExerciseSetError.WEIGHT_TOO_HIGH
        else -> null
    }
    return if (error == null) ParsedValue.Valid(grams.toInt()) else rejected(error)
}

/** Reads a whole number of repetitions from 1 to 999. */
fun parseRepsInput(text: String): ParsedValue {
    val trimmed = text.trim()
    if (!wholeNumber.matches(trimmed)) return notANumber()
    val reps = BigInteger(trimmed)
    val error = when {
        reps.signum() <= 0 -> ExerciseSetError.REPS_NOT_POSITIVE
        reps > BigInteger.valueOf(REPS_MAX.toLong()) -> ExerciseSetError.REPS_TOO_HIGH
        else -> null
    }
    return if (error == null) ParsedValue.Valid(reps.toInt()) else rejected(error)
}

/** What the weight dialog starts with: the weight as it is shown, with a comma and no unit; empty when not fixed. */
fun weightInputText(grams: Int?): String = grams?.let(::formatKilograms).orEmpty()

/** What the repetitions dialog starts with; empty when not fixed. */
fun repsInputText(reps: Int?): String = reps?.toString().orEmpty()

private fun BigDecimal.isWholeNumber(): Boolean = signum() == 0 || stripTrailingZeros().scale() <= 0

private fun notANumber() = ParsedValue.Invalid(TypedValueError.NotANumber)

private fun rejected(error: ExerciseSetError) = ParsedValue.Invalid(TypedValueError.Rejected(error))

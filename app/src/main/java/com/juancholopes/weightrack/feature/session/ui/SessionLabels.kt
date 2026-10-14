package com.juancholopes.weightrack.feature.session.ui

import androidx.annotation.StringRes
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.feature.session.ui.logger.LoggerFieldError
import com.juancholopes.weightrack.feature.session.ui.logger.TypedValueError
import com.juancholopes.weightrack.feature.session.ui.logger.ValueField
import com.juancholopes.weightrack.ui.theme.component.CardTone

/*
 * One `when` per enum, from the domain value to its string resource (SF02-R22, S30-R24, D3 of F01). The
 * texts live once in `strings.xml`: the equipment and muscle labels reuse the resources of F01, but this
 * feature has its own mapping so `session/ui` never imports `catalog/ui`.
 */

@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.MACHINE -> R.string.catalog_equipment_machine
    Equipment.CABLE -> R.string.catalog_equipment_cable
    Equipment.DUMBBELL -> R.string.catalog_equipment_dumbbell
    Equipment.BARBELL -> R.string.catalog_equipment_barbell
    Equipment.BODYWEIGHT -> R.string.catalog_equipment_bodyweight
}

@StringRes
fun Muscle.labelRes(): Int = when (this) {
    Muscle.CHEST -> R.string.catalog_muscle_chest
    Muscle.UPPER_BACK -> R.string.catalog_muscle_upper_back
    Muscle.LATS -> R.string.catalog_muscle_lats
    Muscle.LOWER_BACK -> R.string.catalog_muscle_lower_back
    Muscle.SHOULDERS -> R.string.catalog_muscle_shoulders
    Muscle.BICEPS -> R.string.catalog_muscle_biceps
    Muscle.TRICEPS -> R.string.catalog_muscle_triceps
    Muscle.FOREARMS -> R.string.catalog_muscle_forearms
    Muscle.ABS -> R.string.catalog_muscle_abs
    Muscle.QUADRICEPS -> R.string.catalog_muscle_quadriceps
    Muscle.HAMSTRINGS -> R.string.catalog_muscle_hamstrings
    Muscle.GLUTES -> R.string.catalog_muscle_glutes
    Muscle.CALVES -> R.string.catalog_muscle_calves
    Muscle.ADDUCTORS_ABDUCTORS -> R.string.catalog_muscle_adductors_abductors
}

/**
 * The tone of a card by equipment, in direction A (Spec 42, "Tarjeta"): machine `Primary`, cable `Secondary`,
 * dumbbell and barbell `Tertiary`, bodyweight `Neutral`. The equipment is also written in the subtitle, so
 * nothing depends on the color.
 */
fun Equipment.cardTone(): CardTone = when (this) {
    Equipment.MACHINE -> CardTone.Primary
    Equipment.CABLE -> CardTone.Secondary
    Equipment.DUMBBELL, Equipment.BARBELL -> CardTone.Tertiary
    Equipment.BODYWEIGHT -> CardTone.Neutral
}

/** The message next to the weight or the repetitions that [ExerciseSetError] belongs to (S40-R05). */
@StringRes
fun ExerciseSetError.messageRes(): Int = when (this) {
    ExerciseSetError.WEIGHT_NEGATIVE -> R.string.session_error_weight_negative
    ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP -> R.string.session_error_weight_not_multiple
    ExerciseSetError.WEIGHT_TOO_HIGH -> R.string.session_error_weight_too_high
    ExerciseSetError.REPS_NOT_POSITIVE -> R.string.session_error_reps_out_of_range
    ExerciseSetError.REPS_TOO_HIGH -> R.string.session_error_reps_out_of_range
    ExerciseSetError.EXERCISE_ARCHIVED -> R.string.session_error_exercise_archived
}

/** The message of a rejected session form; the date error goes by the date and the note error by the note. */
@StringRes
fun WorkoutSessionError.messageRes(): Int = when (this) {
    WorkoutSessionError.DATE_IN_FUTURE -> R.string.session_error_date_in_future
    WorkoutSessionError.NOTES_TOO_LONG -> R.string.session_error_notes_too_long
}

/** The message under the panel of the logger when "Guardar serie" did not save. */
@StringRes
fun LoggerFieldError.messageRes(): Int = when (this) {
    LoggerFieldError.WeightMissing -> R.string.logger_error_weight_missing
    LoggerFieldError.RepsMissing -> R.string.logger_error_reps_missing
    is LoggerFieldError.RejectedSet -> error.messageRes()
    is LoggerFieldError.RejectedSession -> error.messageRes()
}

/** The message in a typing dialog; "not a number" has a text for each field because its example differs. */
@StringRes
fun TypedValueError.messageRes(field: ValueField): Int = when (this) {
    TypedValueError.NotANumber ->
        if (field == ValueField.WEIGHT) R.string.session_error_not_a_weight else R.string.session_error_not_reps
    is TypedValueError.Rejected -> error.messageRes()
}

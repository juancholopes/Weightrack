package com.juancholopes.weightrack.feature.catalog.ui

import androidx.annotation.StringRes
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.Muscle

/*
 * One `when` per enum, from the domain value to its string resource (SF01-R13, D3). The texts live once
 * in `strings.xml`; `session/ui` writes its own mapping over the same resources.
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

/** The message shown next to the field (or under the group) that the error belongs to (SF01-R04). */
@StringRes
fun ExerciseError.messageRes(): Int = when (this) {
    ExerciseError.NAME_BLANK -> R.string.catalog_error_name_blank
    ExerciseError.NAME_TOO_LONG -> R.string.catalog_error_name_too_long
    ExerciseError.NAME_TAKEN -> R.string.catalog_error_name_taken
    ExerciseError.NAME_TAKEN_BY_ARCHIVED -> R.string.catalog_error_name_taken_by_archived
    ExerciseError.EQUIPMENT_MISSING -> R.string.catalog_error_equipment_missing
    ExerciseError.MUSCLES_MISSING -> R.string.catalog_error_muscles_missing
    ExerciseError.PRIMARY_MUSCLE_MISSING -> R.string.catalog_error_primary_muscle_missing
    ExerciseError.MUSCLE_REPEATED -> R.string.catalog_error_muscle_repeated
    ExerciseError.NOTES_TOO_LONG -> R.string.catalog_error_notes_too_long
}

package com.juancholopes.weightrack.feature.catalog.domain

import java.util.Locale

const val EXERCISE_NAME_MAX_LENGTH = 60
const val NOTES_MAX_LENGTH = 500

private val whitespaceRun = Regex("\\s+")

/** S10-R04: trims the ends and collapses each internal run of spaces into one. */
fun normalizeExerciseName(raw: String): String = raw.trim().replace(whitespaceRun, " ")

/** S10-R12: trims the ends; an empty or blank note becomes `null`. */
fun normalizeNotes(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

/** S10-R05: names are compared without case, using `Locale.ROOT`; accents still count. */
internal fun nameKey(name: String): String = name.lowercase(Locale.ROOT)

enum class ExerciseError {
    NAME_BLANK,
    NAME_TOO_LONG,
    NAME_TAKEN,
    NAME_TAKEN_BY_ARCHIVED,
    EQUIPMENT_MISSING,
    MUSCLES_MISSING,
    PRIMARY_MUSCLE_MISSING,
    MUSCLE_REPEATED,
    NOTES_TOO_LONG,
}

sealed interface ExerciseValidation {
    data class Valid(val exercise: Exercise, val muscles: List<ExerciseMuscle>) : ExerciseValidation
    data class Invalid(val error: ExerciseError) : ExerciseValidation
}

/**
 * Validates the creation or edition of an exercise (S10-R04 to S10-R08, S10-R12, S10-R15, S10-R21, S10-R22).
 *
 * [id] is the exercise being edited, or a not-yet-persisted id when creating. [existing] holds every
 * exercise, archived ones included. An edited exercise keeps its id and its archived state.
 */
fun validateExercise(
    id: Long,
    name: String,
    equipment: Equipment?,
    muscles: List<ExerciseMuscle>,
    notes: String?,
    existing: List<Exercise>,
): ExerciseValidation {
    if (equipment == null) return ExerciseValidation.Invalid(ExerciseError.EQUIPMENT_MISSING)
    val normalizedName = normalizeExerciseName(name)
    val normalizedNotes = normalizeNotes(notes)
    val sameName = existing.firstOrNull { it.id != id && nameKey(it.name) == nameKey(normalizedName) }
    val error = when {
        normalizedName.isEmpty() -> ExerciseError.NAME_BLANK
        normalizedName.length > EXERCISE_NAME_MAX_LENGTH -> ExerciseError.NAME_TOO_LONG
        sameName?.archived == true -> ExerciseError.NAME_TAKEN_BY_ARCHIVED
        sameName != null -> ExerciseError.NAME_TAKEN
        muscles.isEmpty() -> ExerciseError.MUSCLES_MISSING
        muscles.distinctBy { it.muscle }.size != muscles.size -> ExerciseError.MUSCLE_REPEATED
        muscles.none { it.isPrimary } -> ExerciseError.PRIMARY_MUSCLE_MISSING
        normalizedNotes != null && normalizedNotes.length > NOTES_MAX_LENGTH -> ExerciseError.NOTES_TOO_LONG
        else -> null
    }
    if (error != null) return ExerciseValidation.Invalid(error)
    val exercise = Exercise(
        id = id,
        name = normalizedName,
        equipment = equipment,
        notes = normalizedNotes,
        archived = existing.firstOrNull { it.id == id }?.archived ?: false,
    )
    return ExerciseValidation.Valid(exercise, muscles.map { it.copy(exerciseId = id) })
}

package com.juancholopes.weightrack.feature.catalog.domain

/** Outcome of deleting an exercise (S10-R17, S10-R20). */
sealed interface ExerciseDeletion {
    /** The exercise has sets: it is kept archived, with its name, muscles and sets. */
    data class Archive(val exercise: Exercise) : ExerciseDeletion

    /** The exercise has no sets: it is deleted for good together with its muscles. */
    data class Delete(val exercise: Exercise, val muscles: List<ExerciseMuscle>) : ExerciseDeletion
}

/** [hasSets] tells whether at least one set references [exercise]. */
fun deleteExercise(exercise: Exercise, muscles: List<ExerciseMuscle>, hasSets: Boolean): ExerciseDeletion =
    if (hasSets) {
        ExerciseDeletion.Archive(exercise.copy(archived = true))
    } else {
        ExerciseDeletion.Delete(exercise, muscles.filter { it.exerciseId == exercise.id })
    }

/** S10-R19: an archived exercise returns to the catalog with its history. */
fun unarchiveExercise(exercise: Exercise): Exercise = exercise.copy(archived = false)

private val byName = compareBy<Exercise> { nameKey(it.name) }

/**
 * S10-R30: non-archived exercises sorted by name ignoring case, optionally filtered by a
 * [muscle] (primary or secondary) and by an [equipment]. Both filters apply together.
 */
fun activeCatalog(
    exercises: List<Exercise>,
    muscles: List<ExerciseMuscle>,
    muscle: Muscle? = null,
    equipment: Equipment? = null,
): List<Exercise> {
    val withMuscle = muscle?.let { wanted ->
        muscles.filter { it.muscle == wanted }.map { it.exerciseId }.toSet()
    }
    return exercises
        .filter { !it.archived }
        .filter { withMuscle == null || it.id in withMuscle }
        .filter { equipment == null || it.equipment == equipment }
        .sortedWith(byName)
}

/** S10-R31: archived exercises sorted by name, so they can be unarchived. */
fun archivedExercises(exercises: List<Exercise>): List<Exercise> =
    exercises.filter { it.archived }.sortedWith(byName)

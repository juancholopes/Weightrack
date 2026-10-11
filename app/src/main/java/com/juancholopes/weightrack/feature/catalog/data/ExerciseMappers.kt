package com.juancholopes.weightrack.feature.catalog.data

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.nameKey
import com.juancholopes.weightrack.feature.catalog.domain.normalizeExerciseName

/** S20-R12: the uniqueness key comes from the domain functions, never from a copy of them. */
internal fun normalizedNameOf(name: String): String = nameKey(normalizeExerciseName(name))

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    normalizedName = normalizedNameOf(name),
    equipment = equipment,
    notes = notes,
    archived = archived,
)

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    equipment = equipment,
    notes = notes,
    archived = archived,
)

fun ExerciseMuscle.toEntity(): ExerciseMuscleEntity = ExerciseMuscleEntity(
    exerciseId = exerciseId,
    muscle = muscle,
    isPrimary = isPrimary,
)

fun ExerciseMuscleEntity.toDomain(): ExerciseMuscle = ExerciseMuscle(
    exerciseId = exerciseId,
    muscle = muscle,
    isPrimary = isPrimary,
)

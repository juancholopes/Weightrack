package com.juancholopes.weightrack.feature.session.data

import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(id = id, date = date, notes = notes)

fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(id = id, date = date, notes = notes)

/** S20-R16: grams are stored as the same integer, without conversion. */
fun ExerciseSet.toEntity(): ExerciseSetEntity = ExerciseSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    weightGrams = weightGrams,
    reps = reps,
)

fun ExerciseSetEntity.toDomain(): ExerciseSet = ExerciseSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    order = order,
    weightGrams = weightGrams,
    reps = reps,
)

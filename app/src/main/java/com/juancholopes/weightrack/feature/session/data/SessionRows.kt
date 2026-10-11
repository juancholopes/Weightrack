package com.juancholopes.weightrack.feature.session.data

import androidx.room3.Embedded
import com.juancholopes.weightrack.feature.catalog.data.ExerciseEntity

/** A set with its session, for "última vez" and "evolución" (S20-R29, S20-R30). */
data class SetWithSessionRow(
    @Embedded val set: ExerciseSetEntity,
    @Embedded(prefix = "ws_") val session: WorkoutSessionEntity,
)

/** A set with its exercise, archived or not, for the sets of a session (S20-R32). */
data class SetWithExerciseRow(
    @Embedded val set: ExerciseSetEntity,
    @Embedded(prefix = "ex_") val exercise: ExerciseEntity,
)

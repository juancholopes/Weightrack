package com.juancholopes.weightrack.feature.session.data

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.juancholopes.weightrack.feature.catalog.data.ExerciseEntity
import java.time.LocalDate

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

/** The latest session of an exercise and its heaviest weight there, for the home cards (S20-R49). */
data class LatestSessionPeakRow(
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "heaviest_weight_grams") val heaviestWeightGrams: Int,
)

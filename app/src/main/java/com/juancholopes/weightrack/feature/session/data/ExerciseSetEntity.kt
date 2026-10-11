package com.juancholopes.weightrack.feature.session.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.juancholopes.weightrack.feature.catalog.data.ExerciseEntity

/**
 * Table `exercise_set` (Spec 20). Deleting a session cascades to its sets (S20-R20); an exercise with
 * sets cannot be hard-deleted (S20-R22). Ids never change (S20-R23).
 */
@Entity(
    tableName = "exercise_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["session_id", "set_order"], unique = true, name = "index_exercise_set_session_id_set_order"),
        Index(value = ["exercise_id", "session_id"], name = "index_exercise_set_exercise_id_session_id"),
    ],
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "set_order") val order: Int,
    @ColumnInfo(name = "weight_grams") val weightGrams: Int,
    @ColumnInfo(name = "reps") val reps: Int,
)

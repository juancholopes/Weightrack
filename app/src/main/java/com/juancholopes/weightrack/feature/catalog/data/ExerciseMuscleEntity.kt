package com.juancholopes.weightrack.feature.catalog.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.juancholopes.weightrack.feature.catalog.domain.Muscle

/**
 * Table `exercise_muscle` (Spec 20). The composite primary key keeps a muscle from repeating in an
 * exercise (S20-R25); deleting an exercise cascades to its muscles (S20-R21).
 */
@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exercise_id", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["muscle", "exercise_id"], name = "index_exercise_muscle_muscle_exercise_id"),
    ],
)
data class ExerciseMuscleEntity(
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "muscle") val muscle: Muscle,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean,
)

package com.juancholopes.weightrack.feature.catalog.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.juancholopes.weightrack.feature.catalog.domain.Equipment

/** Table `exercise` (Spec 20). `normalized_name` is the uniqueness key of S10-R05 (S20-R12, S20-R24). */
@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["normalized_name"], unique = true, name = "index_exercise_normalized_name"),
        Index(value = ["archived", "equipment"], name = "index_exercise_archived_equipment"),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "equipment") val equipment: Equipment,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "archived") val archived: Boolean,
)

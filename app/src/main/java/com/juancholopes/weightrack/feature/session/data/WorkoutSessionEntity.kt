package com.juancholopes.weightrack.feature.session.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.time.LocalDate

/** Table `workout_session` (Spec 20). `date` is stored as ISO-8601 text (S20-R15). */
@Entity(
    tableName = "workout_session",
    indices = [Index(value = ["date", "id"], name = "index_workout_session_date_id")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "notes") val notes: String?,
)

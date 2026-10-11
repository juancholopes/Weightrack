package com.juancholopes.weightrack.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.juancholopes.weightrack.feature.catalog.data.ExerciseDao
import com.juancholopes.weightrack.feature.catalog.data.ExerciseEntity
import com.juancholopes.weightrack.feature.catalog.data.ExerciseMuscleEntity
import com.juancholopes.weightrack.feature.session.data.ExerciseSetEntity
import com.juancholopes.weightrack.feature.session.data.SessionDao
import com.juancholopes.weightrack.feature.session.data.WorkoutSessionEntity

/** Weightrack local database (S20-R04). Every schema change needs a migration (S20-R44). */
@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(Converters::class)
abstract class WeightrackDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "weightrack.db"
    }
}

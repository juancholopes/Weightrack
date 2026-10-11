package com.juancholopes.weightrack.feature.catalog.data

import androidx.room3.withWriteTransaction
import androidx.sqlite.SQLiteException
import com.juancholopes.weightrack.data.WeightrackDatabase
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.domain.deleteExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room implementation of [ExerciseRepository] (S20-R43). No entity leaves this class. */
class RoomExerciseRepository(
    private val database: WeightrackDatabase,
    private val dao: ExerciseDao,
) : ExerciseRepository {

    override fun observeActiveCatalog(muscle: Muscle?, equipment: Equipment?): Flow<List<Exercise>> =
        dao.observeActive(muscle, equipment).map { list -> list.map { it.toDomain() } }

    override fun observeArchived(): Flow<List<Exercise>> =
        dao.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observeExercise(id: Long): Flow<Exercise?> =
        dao.observeExercise(id).map { it?.toDomain() }

    override fun observeMuscles(exerciseId: Long): Flow<List<ExerciseMuscle>> =
        dao.observeMuscles(exerciseId).map { list -> list.map { it.toDomain() } }

    override suspend fun create(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult =
        rejectingNameCollision(exercise, excludingId = null) {
            dao.insertWithMuscles(exercise.toEntity(), muscles.map { it.toEntity() })
        }

    override suspend fun update(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult =
        rejectingNameCollision(exercise, excludingId = exercise.id) {
            dao.updateWithMuscles(exercise.toEntity(), muscles.map { it.toEntity() })
            exercise.id
        }

    /** S20-R40: read, decide with the domain and apply in a single write transaction. */
    override suspend fun delete(exerciseId: Long): ExerciseDeletion =
        database.withWriteTransaction {
            val exercise = checkNotNull(dao.findById(exerciseId)) { "Exercise $exerciseId does not exist" }
            val muscles = dao.musclesOf(exerciseId).map { it.toDomain() }
            val hasSets = dao.countSetsOfExercise(exerciseId) > 0
            val deletion = deleteExercise(exercise.toDomain(), muscles, hasSets)
            when (deletion) {
                is ExerciseDeletion.Archive -> dao.setArchived(exerciseId, true)
                is ExerciseDeletion.Delete -> dao.deleteIfHasNoSets(exerciseId)
            }
            deletion
        }

    override suspend fun unarchive(exerciseId: Long) {
        dao.setArchived(exerciseId, false)
    }

    /**
     * S20-R41: when the unique index on `normalized_name` rejects the write, reports which exercise
     * holds the name instead of letting the SQLite exception escape.
     */
    private suspend fun rejectingNameCollision(
        exercise: Exercise,
        excludingId: Long?,
        write: suspend () -> Long,
    ): ExerciseWriteResult = try {
        ExerciseWriteResult.Saved(write())
    } catch (error: SQLiteException) {
        val holder = dao.findByNormalizedName(normalizedNameOf(exercise.name))
            ?.takeIf { it.id != excludingId }
            ?: throw error
        ExerciseWriteResult.Rejected(
            if (holder.archived) ExerciseError.NAME_TAKEN_BY_ARCHIVED else ExerciseError.NAME_TAKEN,
        )
    }
}

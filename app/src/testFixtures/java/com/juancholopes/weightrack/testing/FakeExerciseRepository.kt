package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.domain.activeCatalog
import com.juancholopes.weightrack.feature.catalog.domain.archivedExercises
import com.juancholopes.weightrack.feature.catalog.domain.deleteExercise
import com.juancholopes.weightrack.feature.catalog.domain.nameKey
import com.juancholopes.weightrack.feature.catalog.domain.normalizeExerciseName
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory [ExerciseRepository] for JVM tests (Spec 31, S31-R05). It honors the same contract as
 * `RoomExerciseRepository`, proven by `ExerciseRepositoryContract`, and answers reads with the domain
 * functions of Spec 10.
 */
class FakeExerciseRepository(private val store: InMemoryStore = InMemoryStore()) : ExerciseRepository {

    private val nextWriteFailure = AtomicReference<Exception?>(null)

    /**
     * Makes the next write (`create`, `update`, `delete` or `unarchive`) throw [error] without writing
     * anything, as an unexpected database failure would (S31-R08, Spec F01 SF01-R16). Later writes work.
     */
    fun failNextWrite(error: Exception = IllegalStateException("Simulated write failure")) {
        nextWriteFailure.set(error)
    }

    private fun throwIfWriteFails() {
        nextWriteFailure.getAndSet(null)?.let { throw it }
    }

    override fun observeActiveCatalog(muscle: Muscle?, equipment: Equipment?): Flow<List<Exercise>> =
        store.observe { activeCatalog(it.exercises, it.muscles, muscle, equipment) }

    override fun observeArchived(): Flow<List<Exercise>> =
        store.observe { archivedExercises(it.exercises) }

    override fun observeExercise(id: Long): Flow<Exercise?> =
        store.observe { tables -> tables.exercises.firstOrNull { it.id == id } }

    override fun observeMuscles(exerciseId: Long): Flow<List<ExerciseMuscle>> =
        store.observe { tables -> tables.muscles.filter { it.exerciseId == exerciseId } }

    /** S20-R48. */
    override fun observeHasSets(exerciseId: Long): Flow<Boolean> =
        store.observe { tables -> tables.sets.any { it.exerciseId == exerciseId } }

    override suspend fun create(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult {
        throwIfWriteFails()
        return store.write { tables ->
            val taken = tables.nameTakenError(exercise.name, excludingId = null)
            if (taken != null) return@write tables to ExerciseWriteResult.Rejected(taken)
            val id = tables.lastExerciseId + 1
            val next = tables.copy(
                exercises = tables.exercises + exercise.copy(id = id),
                muscles = tables.muscles + muscles.map { it.copy(exerciseId = id) },
                lastExerciseId = id,
            )
            next to ExerciseWriteResult.Saved(id)
        }
    }

    override suspend fun update(exercise: Exercise, muscles: List<ExerciseMuscle>): ExerciseWriteResult {
        throwIfWriteFails()
        return store.write { tables ->
            val taken = tables.nameTakenError(exercise.name, excludingId = exercise.id)
            if (taken != null) return@write tables to ExerciseWriteResult.Rejected(taken)
            check(tables.exercises.any { it.id == exercise.id }) { "Exercise ${exercise.id} does not exist" }
            val next = tables.copy(
                exercises = tables.exercises.map { if (it.id == exercise.id) exercise else it },
                muscles = tables.muscles.filter { it.exerciseId != exercise.id } +
                    muscles.map { it.copy(exerciseId = exercise.id) },
            )
            next to ExerciseWriteResult.Saved(exercise.id)
        }
    }

    /** S20-R40: decides with the domain and applies the result in one change. */
    override suspend fun delete(exerciseId: Long): ExerciseDeletion {
        throwIfWriteFails()
        return store.write { tables ->
            val exercise = checkNotNull(tables.exercises.firstOrNull { it.id == exerciseId }) {
                "Exercise $exerciseId does not exist"
            }
            val deletion = deleteExercise(
                exercise = exercise,
                muscles = tables.muscles.filter { it.exerciseId == exerciseId },
                hasSets = tables.sets.any { it.exerciseId == exerciseId },
            )
            val next = when (deletion) {
                is ExerciseDeletion.Archive -> tables.copy(
                    exercises = tables.exercises.map { if (it.id == exerciseId) deletion.exercise else it },
                )
                is ExerciseDeletion.Delete -> tables.copy(
                    exercises = tables.exercises.filter { it.id != exerciseId },
                    muscles = tables.muscles.filter { it.exerciseId != exerciseId },
                )
            }
            next to deletion
        }
    }

    override suspend fun unarchive(exerciseId: Long) {
        throwIfWriteFails()
        store.write { tables ->
            tables.copy(
                exercises = tables.exercises.map { if (it.id == exerciseId) it.copy(archived = false) else it },
            ) to Unit
        }
    }

    /** S20-R41: the error Room reports when the unique index on the normalized name rejects a write. */
    private fun InMemoryStore.Tables.nameTakenError(name: String, excludingId: Long?): ExerciseError? {
        val key = nameKey(normalizeExerciseName(name))
        val holder = exercises.firstOrNull { it.id != excludingId && nameKey(normalizeExerciseName(it.name)) == key }
        return when {
            holder == null -> null
            holder.archived -> ExerciseError.NAME_TAKEN_BY_ARCHIVED
            else -> ExerciseError.NAME_TAKEN
        }
    }
}

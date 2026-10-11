package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.nameKey
import com.juancholopes.weightrack.feature.catalog.domain.normalizeExerciseName
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * In-memory tables behind [FakeExerciseRepository] and [FakeSessionRepository] (Spec 31, S31-R05). It plays
 * the role of the Room database: share one store between both fakes to get the shared tables.
 *
 * Like the database, it never reuses an id (S20-R17), checks foreign keys and unique keys after every
 * change (S20-R20 to S20-R26), and applies each change all or nothing (S20-R38, S20-R39).
 */
class InMemoryStore {

    internal data class Tables(
        val exercises: List<Exercise> = emptyList(),
        val muscles: List<ExerciseMuscle> = emptyList(),
        val sessions: List<WorkoutSession> = emptyList(),
        val sets: List<ExerciseSet> = emptyList(),
        val lastExerciseId: Long = 0,
        val lastSessionId: Long = 0,
        val lastSetId: Long = 0,
    )

    private val tables = MutableStateFlow(Tables())

    /** Emits [query] over the current tables and again after every change that alters its result. */
    internal fun <T> observe(query: (Tables) -> T): Flow<T> = tables.map(query).distinctUntilChanged()

    internal fun <T> read(query: (Tables) -> T): T = query(tables.value)

    /**
     * Applies [change], which returns the new tables and a result. If [change] throws or the new tables
     * break a constraint, nothing is written.
     */
    internal fun <T> write(change: (Tables) -> Pair<Tables, T>): T = synchronized(this) {
        val (next, result) = change(tables.value)
        next.checkConstraints()
        tables.value = next
        result
    }

    private fun Tables.checkConstraints() {
        val nameKeys = exercises.map { nameKey(normalizeExerciseName(it.name)) }
        check(nameKeys.size == nameKeys.toSet().size) { "UNIQUE constraint failed: exercise.normalized_name" }
        val exerciseIds = exercises.map { it.id }.toSet()
        val sessionIds = sessions.map { it.id }.toSet()
        check(muscles.all { it.exerciseId in exerciseIds }) { "FOREIGN KEY constraint failed: exercise_muscle" }
        check(muscles.distinctBy { it.exerciseId to it.muscle }.size == muscles.size) {
            "UNIQUE constraint failed: exercise_muscle.exercise_id, exercise_muscle.muscle"
        }
        check(sets.all { it.sessionId in sessionIds && it.exerciseId in exerciseIds }) {
            "FOREIGN KEY constraint failed: exercise_set"
        }
        check(sets.distinctBy { it.sessionId to it.order }.size == sets.size) {
            "UNIQUE constraint failed: exercise_set.session_id, exercise_set.set_order"
        }
    }
}

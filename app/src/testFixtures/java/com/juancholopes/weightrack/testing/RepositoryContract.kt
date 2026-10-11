package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.first
import org.junit.Before
import java.time.LocalDate

/** Both repositories over one new, empty storage, wired the way the app wires them. */
class Repositories(val exercises: ExerciseRepository, val sessions: SessionRepository)

/**
 * Shared set-up and helpers of the repository contract (Spec 31, S31-R07). Each concrete subclass runs
 * the same tests against one implementation: the fakes on the JVM and Room on a device.
 */
abstract class RepositoryContract {

    /** Builds both repositories over one new, empty storage. Called before each test. */
    protected abstract fun createRepositories(): Repositories

    protected lateinit var exercises: ExerciseRepository
        private set
    protected lateinit var sessions: SessionRepository
        private set

    protected val oct10: LocalDate = LocalDate.of(2026, 10, 10)

    @Before
    fun setUpRepositories() {
        val repositories = createRepositories()
        exercises = repositories.exercises
        sessions = repositories.sessions
    }

    /** An exercise that is not saved yet (`id = 0`) unless [id] says otherwise. */
    protected fun exercise(name: String, equipment: Equipment = Equipment.BARBELL, id: Long = 0) =
        Exercise(id = id, name = name, equipment = equipment, notes = null)

    protected fun muscle(muscle: Muscle, isPrimary: Boolean = true) =
        ExerciseMuscle(exerciseId = 0, muscle = muscle, isPrimary = isPrimary)

    /** Saves an exercise and returns it with the id the repository assigned. */
    protected suspend fun createExercise(
        name: String,
        equipment: Equipment = Equipment.BARBELL,
        vararg muscles: ExerciseMuscle = arrayOf(muscle(Muscle.CHEST)),
    ): Exercise {
        val result = exercises.create(exercise(name, equipment), muscles.toList())
        return exercise(name, equipment, id = (result as ExerciseWriteResult.Saved).id)
    }

    /** Archives [exercise] with an edit that keeps its muscles, and returns it archived. */
    protected suspend fun archive(exercise: Exercise): Exercise {
        val archived = exercise.copy(archived = true)
        exercises.update(archived, exercises.observeMuscles(exercise.id).first())
        return archived
    }

    /** A set that is not saved yet; [sessionId] only matters for `addSet`. */
    protected fun set(
        exerciseId: Long,
        order: Int,
        weightGrams: Int = 60_000,
        reps: Int = 10,
        sessionId: Long = 0,
    ) = ExerciseSet(
        id = 0,
        sessionId = sessionId,
        exerciseId = exerciseId,
        order = order,
        weightGrams = weightGrams,
        reps = reps,
    )

    /** Saves a session with [sets] and returns it with the id the repository assigned. */
    protected suspend fun createSession(date: LocalDate = oct10, vararg sets: ExerciseSet): WorkoutSession {
        val id = sessions.createSession(WorkoutSession(id = 0, date = date, notes = null), sets.toList())
        return WorkoutSession(id = id, date = date, notes = null)
    }

    /** Asserts that [block] fails with an exception, whatever its type, and returns it. */
    protected suspend fun assertFails(block: suspend () -> Unit): Exception {
        try {
            block()
        } catch (expected: Exception) {
            return expected
        }
        throw AssertionError("Expected the write to fail")
    }
}

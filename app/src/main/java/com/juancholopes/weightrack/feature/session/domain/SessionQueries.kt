package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import java.time.LocalDate

/**
 * Session order shared by every question: a later date is more recent and, on the same date, the
 * higher `id` (created later, S10-R14) is more recent. Sorts from oldest to most recent.
 */
val sessionRecency: Comparator<WorkoutSession> = compareBy<WorkoutSession> { it.date }.thenBy { it.id }

/** "Última vez": the most recent session containing the exercise and its sets there. */
data class LastPerformance(val session: WorkoutSession, val sets: List<ExerciseSet>)

/** "Evolución": every set of an exercise with the date of its session, ordered in time. */
data class ExerciseEvolution(val entries: List<Entry>) {
    data class Entry(val date: LocalDate, val set: ExerciseSet)
}

/** What deleting a session removes: the session and all its sets (S10-R23). */
data class SessionDeletion(val session: WorkoutSession, val sets: List<ExerciseSet>)

/**
 * S10-R25, S10-R26: the most recent session with at least one set of [exerciseId], with those sets
 * sorted by `order`, or `null` when there is none. With [before], only sessions earlier than it count.
 */
fun lastPerformance(
    exerciseId: Long,
    sessions: List<WorkoutSession>,
    sets: List<ExerciseSet>,
    before: WorkoutSession? = null,
): LastPerformance? {
    val exerciseSets = sets.filter { it.exerciseId == exerciseId }
    val sessionIds = exerciseSets.map { it.sessionId }.toSet()
    val session = sessions
        .filter { it.id in sessionIds }
        .filter { before == null || sessionRecency.compare(it, before) < 0 }
        .maxWithOrNull(sessionRecency)
        ?: return null
    return LastPerformance(session, exerciseSets.filter { it.sessionId == session.id }.sortedBy { it.order })
}

/** S10-R27: every set of [exerciseId] with its session date, by date, session `id` and `order`. */
fun exerciseEvolution(
    exerciseId: Long,
    sessions: List<WorkoutSession>,
    sets: List<ExerciseSet>,
): ExerciseEvolution {
    val sessionsById = sessions.associateBy { it.id }
    val entries = sets
        .filter { it.exerciseId == exerciseId }
        .map { it to sessionsById.getValue(it.sessionId) }
        .sortedWith(compareBy(sessionRecency) { pair: Pair<ExerciseSet, WorkoutSession> -> pair.second }
            .thenBy { it.first.order })
        .map { (set, session) -> ExerciseEvolution.Entry(session.date, set) }
    return ExerciseEvolution(entries)
}

/** S10-R28: sessions from the most recent to the oldest. */
fun sessionHistory(sessions: List<WorkoutSession>): List<WorkoutSession> =
    sessions.sortedWith(sessionRecency.reversed())

/** S10-R29: the sets of [sessionId] sorted by `order`, each with its exercise, archived or not. */
fun sessionSets(
    sessionId: Long,
    sets: List<ExerciseSet>,
    exercises: List<Exercise>,
): List<Pair<ExerciseSet, Exercise>> {
    val exercisesById = exercises.associateBy { it.id }
    return sets
        .filter { it.sessionId == sessionId }
        .sortedBy { it.order }
        .map { it to exercisesById.getValue(it.exerciseId) }
}

/** S10-R23: deleting [session] deletes all its sets; exercises do not change. */
fun deleteSession(session: WorkoutSession, sets: List<ExerciseSet>): SessionDeletion =
    SessionDeletion(session, sets.filter { it.sessionId == session.id })

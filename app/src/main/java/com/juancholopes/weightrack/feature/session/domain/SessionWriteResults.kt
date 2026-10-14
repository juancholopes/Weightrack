package com.juancholopes.weightrack.feature.session.domain

/*
 * What the session use cases return (S30-R22): a business rejection is a value that carries the error enum of
 * the domain; it is never thrown. Unexpected failures stay exceptions for the ViewModel to catch (S30-R23).
 */

/** Outcome of creating or updating a session (SF02-R04). */
sealed interface SessionWriteResult {
    data class Saved(val sessionId: Long) : SessionWriteResult
    data class Rejected(val error: WorkoutSessionError) : SessionWriteResult
}

/**
 * Outcome of adding a set (SF02-R04, SF02-R05). The set can be rejected for its own values or, when it creates the
 * session, because of the session's date.
 */
sealed interface AddSetResult {
    /** The set is saved; [sessionId] is the session that holds it, new or not. */
    data class Saved(val sessionId: Long) : AddSetResult
    data class RejectedSet(val error: ExerciseSetError) : AddSetResult
    data class RejectedSession(val error: WorkoutSessionError) : AddSetResult
}

/** Outcome of correcting a set (SF02-R04). */
sealed interface CorrectSetResult {
    /** [set] is the set as stored after the correction. */
    data class Saved(val set: ExerciseSet) : CorrectSetResult
    data class Rejected(val error: ExerciseSetError) : CorrectSetResult
}

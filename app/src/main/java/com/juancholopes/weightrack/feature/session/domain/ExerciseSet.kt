package com.juancholopes.weightrack.feature.session.domain

data class ExerciseSet(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val order: Int,
    val weightGrams: Int,
    val reps: Int,
)

package com.juancholopes.weightrack.feature.catalog.domain

data class ExerciseMuscle(
    val exerciseId: Long,
    val muscle: Muscle,
    val isPrimary: Boolean,
)

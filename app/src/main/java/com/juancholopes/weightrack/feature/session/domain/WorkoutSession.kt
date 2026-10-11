package com.juancholopes.weightrack.feature.session.domain

import java.time.LocalDate

data class WorkoutSession(
    val id: Long,
    val date: LocalDate,
    val notes: String?,
)

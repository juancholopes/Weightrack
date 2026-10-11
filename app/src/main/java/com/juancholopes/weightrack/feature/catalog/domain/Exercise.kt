package com.juancholopes.weightrack.feature.catalog.domain

data class Exercise(
    val id: Long,
    val name: String,
    val equipment: Equipment,
    val notes: String?,
    val archived: Boolean = false,
)

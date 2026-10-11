package com.juancholopes.weightrack.feature.catalog.data

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ExerciseMappersTest {

    // S20-A34
    @Test
    fun exercise_roundTrip() {
        val withNotes = Exercise(id = 7, name = "Press de banca", equipment = Equipment.BARBELL, notes = "asiento en 4", archived = true)
        val withoutNotes = Exercise(id = 8, name = "Prensa", equipment = Equipment.MACHINE, notes = null, archived = false)

        listOf(withNotes, withoutNotes).forEach { exercise ->
            val entity = exercise.toEntity()
            assertEquals(exercise.id, entity.id)
            assertEquals(exercise.name, entity.name)
            assertEquals(exercise.equipment, entity.equipment)
            assertEquals(exercise.notes, entity.notes)
            assertEquals(exercise.archived, entity.archived)
            assertEquals(exercise, entity.toDomain())
        }
    }

    // S20-A34
    @Test
    fun exerciseMuscle_roundTrip() {
        listOf(
            ExerciseMuscle(exerciseId = 3, muscle = Muscle.CHEST, isPrimary = true),
            ExerciseMuscle(exerciseId = 3, muscle = Muscle.TRICEPS, isPrimary = false),
        ).forEach { muscle ->
            val entity = muscle.toEntity()
            assertEquals(muscle.exerciseId, entity.exerciseId)
            assertEquals(muscle.muscle, entity.muscle)
            assertEquals(muscle.isPrimary, entity.isPrimary)
            assertEquals(muscle, entity.toDomain())
        }
    }

    // S20-A35 / S20-R12
    @Test
    fun normalizedName_usesDomainNormalization() {
        val exercise = Exercise(id = 0, name = "  Press   DE banca ", equipment = Equipment.BARBELL, notes = null)
        assertEquals("press de banca", exercise.toEntity().normalizedName)
        val accented = Exercise(id = 0, name = "Prés de banca", equipment = Equipment.BARBELL, notes = null)
        assertEquals("prés de banca", accented.toEntity().normalizedName)
    }

    // S20-A35 / S20-R12: Locale.ROOT, so a Turkish default locale does not turn "I" into "ı"
    @Test
    fun normalizedName_ignoresDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            val exercise = Exercise(id = 0, name = "PRESS INCLINADO", equipment = Equipment.BARBELL, notes = null)
            assertEquals("press inclinado", exercise.toEntity().normalizedName)
        } finally {
            Locale.setDefault(original)
        }
    }
}

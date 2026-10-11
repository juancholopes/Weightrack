package com.juancholopes.weightrack.feature.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseRulesTest {

    private val chestPrimary = ExerciseMuscle(exerciseId = 0, muscle = Muscle.CHEST, isPrimary = true)

    private fun validate(
        name: String = "Press de banca",
        equipment: Equipment? = Equipment.BARBELL,
        muscles: List<ExerciseMuscle> = listOf(chestPrimary),
        notes: String? = null,
        existing: List<Exercise> = emptyList(),
        id: Long = 0,
    ) = validateExercise(id, name, equipment, muscles, notes, existing)

    private fun errorOf(result: ExerciseValidation) = (result as ExerciseValidation.Invalid).error

    private fun validOf(result: ExerciseValidation) = result as ExerciseValidation.Valid

    @Test
    fun enumsListTheValuesOfSpec10() {
        assertEquals(
            listOf("MACHINE", "CABLE", "DUMBBELL", "BARBELL", "BODYWEIGHT"),
            Equipment.entries.map { it.name },
        )
        assertEquals(
            listOf(
                "CHEST", "UPPER_BACK", "LATS", "LOWER_BACK", "SHOULDERS", "BICEPS", "TRICEPS",
                "FOREARMS", "ABS", "QUADRICEPS", "HAMSTRINGS", "GLUTES", "CALVES", "ADDUCTORS_ABDUCTORS",
            ),
            Muscle.entries.map { it.name },
        )
    }

    // S10-A04 / S10-R04
    @Test
    fun nameIsNormalizedBeforeSaving() {
        assertEquals("Press de banca", normalizeExerciseName("  Press   de  banca "))
        assertEquals("Press de banca", validOf(validate(name = "  Press   de  banca ")).exercise.name)
    }

    @Test
    fun emptyOrBlankNameIsRejected() {
        assertEquals(ExerciseError.NAME_BLANK, errorOf(validate(name = "")))
        assertEquals(ExerciseError.NAME_BLANK, errorOf(validate(name = "    ")))
    }

    // S10-A05 / S10-R05, S10-R21
    @Test
    fun duplicateNameIgnoringCaseIsRejected() {
        val existing = listOf(Exercise(1, "Press de banca", Equipment.BARBELL, null, archived = false))
        assertEquals(ExerciseError.NAME_TAKEN, errorOf(validate(name = "press  DE banca", existing = existing)))
    }

    @Test
    fun duplicateNameOfArchivedExerciseIsRejectedAndReportsTheArchivedOne() {
        val existing = listOf(Exercise(1, "Press de banca", Equipment.BARBELL, null, archived = true))
        assertEquals(
            ExerciseError.NAME_TAKEN_BY_ARCHIVED,
            errorOf(validate(name = "press  DE banca", existing = existing)),
        )
    }

    @Test
    fun accentsAreDistinguishedInUniqueness() {
        val existing = listOf(Exercise(1, "Elevación", Equipment.DUMBBELL, null))
        assertEquals("Elevacion", validOf(validate(name = "Elevacion", existing = existing)).exercise.name)
    }

    @Test
    fun editingKeepsOwnNameWithoutConflict() {
        val existing = listOf(Exercise(7, "Press de banca", Equipment.BARBELL, null))
        val result = validOf(validate(id = 7, name = "PRESS DE BANCA", existing = existing))
        assertEquals(7L, result.exercise.id)
    }

    // S10-A06 / S10-R06, S10-R07
    @Test
    fun missingEquipmentIsRejected() {
        assertEquals(ExerciseError.EQUIPMENT_MISSING, errorOf(validate(equipment = null)))
    }

    @Test
    fun missingMusclesIsRejected() {
        assertEquals(ExerciseError.MUSCLES_MISSING, errorOf(validate(muscles = emptyList())))
    }

    @Test
    fun missingPrimaryMuscleIsRejected() {
        val onlySecondary = listOf(ExerciseMuscle(0, Muscle.TRICEPS, isPrimary = false))
        assertEquals(ExerciseError.PRIMARY_MUSCLE_MISSING, errorOf(validate(muscles = onlySecondary)))
    }

    @Test
    fun twoPrimariesAndOneSecondaryAreAccepted() {
        val muscles = listOf(
            ExerciseMuscle(0, Muscle.CHEST, isPrimary = true),
            ExerciseMuscle(0, Muscle.SHOULDERS, isPrimary = true),
            ExerciseMuscle(0, Muscle.TRICEPS, isPrimary = false),
        )
        val result = validOf(validate(id = 3, muscles = muscles))
        assertEquals(3, result.muscles.size)
        assertEquals(listOf(3L, 3L, 3L), result.muscles.map { it.exerciseId })
        assertEquals(false, result.exercise.archived)
    }

    // S10-A07 / S10-R08
    @Test
    fun repeatedMuscleIsRejectedEvenWithDifferentRole() {
        val muscles = listOf(
            ExerciseMuscle(0, Muscle.CHEST, isPrimary = true),
            ExerciseMuscle(0, Muscle.CHEST, isPrimary = false),
        )
        assertEquals(ExerciseError.MUSCLE_REPEATED, errorOf(validate(muscles = muscles)))
    }

    // S10-A10 / S10-R12
    @Test
    fun notesAreTrimmedAndBlankNotesBecomeNull() {
        assertEquals("asiento en 4", normalizeNotes("  asiento en 4 "))
        assertNull(normalizeNotes("   "))
        assertNull(normalizeNotes(null))
        assertEquals("asiento en 4", validOf(validate(notes = "  asiento en 4 ")).exercise.notes)
        assertNull(validOf(validate(notes = "   ")).exercise.notes)
    }

    // S10-R15
    @Test
    fun nameUpTo60CharactersIsAcceptedAndLongerIsRejected() {
        validOf(validate(name = "a".repeat(60)))
        assertEquals(ExerciseError.NAME_TOO_LONG, errorOf(validate(name = "a".repeat(61))))
    }

    @Test
    fun notesUpTo500CharactersAreAcceptedAndLongerAreRejected() {
        validOf(validate(notes = "a".repeat(500)))
        assertEquals(ExerciseError.NOTES_TOO_LONG, errorOf(validate(notes = "a".repeat(501))))
    }
}

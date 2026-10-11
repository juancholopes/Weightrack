package com.juancholopes.weightrack.feature.catalog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCatalogTest {

    private val bench = Exercise(1, "Press de banca", Equipment.BARBELL, null)
    private val curl = Exercise(2, "curl de bíceps", Equipment.DUMBBELL, null)
    private val pulldown = Exercise(3, "Jalón al pecho", Equipment.CABLE, null)
    private val oldRow = Exercise(4, "Remo antiguo", Equipment.MACHINE, null, archived = true)
    private val abRoller = Exercise(5, "abdominal en máquina", Equipment.MACHINE, null, archived = true)

    private val exercises = listOf(bench, curl, pulldown, oldRow, abRoller)
    private val muscles = listOf(
        ExerciseMuscle(1, Muscle.CHEST, isPrimary = true),
        ExerciseMuscle(1, Muscle.TRICEPS, isPrimary = false),
        ExerciseMuscle(2, Muscle.BICEPS, isPrimary = true),
        ExerciseMuscle(3, Muscle.LATS, isPrimary = true),
        ExerciseMuscle(3, Muscle.BICEPS, isPrimary = false),
        ExerciseMuscle(4, Muscle.UPPER_BACK, isPrimary = true),
    )

    // S10-A11 / S10-R17, S10-R18, S10-R19
    @Test
    fun deletingAnExerciseWithSetsArchivesIt() {
        val deletion = deleteExercise(bench, muscles, hasSets = true)

        val archived = (deletion as ExerciseDeletion.Archive).exercise
        assertEquals(bench.copy(archived = true), archived)

        val afterDeletion = exercises.map { if (it.id == archived.id) archived else it }
        assertEquals(false, activeCatalog(afterDeletion, muscles).contains(archived))
        assertEquals(true, archivedExercises(afterDeletion).contains(archived))

        val restored = unarchiveExercise(archived)
        assertEquals(bench, restored)
        val afterRestore = afterDeletion.map { if (it.id == restored.id) restored else it }
        assertEquals(true, activeCatalog(afterRestore, muscles).contains(restored))
    }

    // S10-A12 / S10-R20
    @Test
    fun deletingAnExerciseWithoutSetsDeletesItWithItsMuscles() {
        val deletion = deleteExercise(bench, muscles, hasSets = false)

        assertEquals(
            ExerciseDeletion.Delete(
                exercise = bench,
                muscles = listOf(
                    ExerciseMuscle(1, Muscle.CHEST, isPrimary = true),
                    ExerciseMuscle(1, Muscle.TRICEPS, isPrimary = false),
                ),
            ),
            deletion,
        )
    }

    // S10-A19 / S10-R30
    @Test
    fun activeCatalogExcludesArchivedAndSortsByNameIgnoringCase() {
        assertEquals(listOf(curl, pulldown, bench), activeCatalog(exercises, muscles))
    }

    @Test
    fun activeCatalogFiltersByPrimaryOrSecondaryMuscle() {
        assertEquals(listOf(curl, pulldown), activeCatalog(exercises, muscles, muscle = Muscle.BICEPS))
        assertEquals(listOf(bench), activeCatalog(exercises, muscles, muscle = Muscle.TRICEPS))
    }

    @Test
    fun activeCatalogFiltersByEquipment() {
        assertEquals(listOf(pulldown), activeCatalog(exercises, muscles, equipment = Equipment.CABLE))
    }

    @Test
    fun activeCatalogAppliesBothFilters() {
        assertEquals(
            listOf(curl),
            activeCatalog(exercises, muscles, muscle = Muscle.BICEPS, equipment = Equipment.DUMBBELL),
        )
    }

    // S10-A19 / S10-R31
    @Test
    fun archivedExercisesAreListedApartSortedByName() {
        assertEquals(listOf(abRoller, oldRow), archivedExercises(exercises))
    }
}

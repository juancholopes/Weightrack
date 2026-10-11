package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSetRulesTest {

    private val bench = Exercise(1, "Press de banca", Equipment.BARBELL, null)
    private val pullUp = Exercise(2, "Dominadas", Equipment.BODYWEIGHT, null)
    private val archivedRow = Exercise(3, "Remo", Equipment.MACHINE, null, archived = true)

    private fun add(
        exercise: Exercise = bench,
        weightGrams: Int = 60_000,
        reps: Int = 10,
        sets: List<ExerciseSet> = emptyList(),
    ) = validateNewSet(id = 0, sessionId = 10, exercise = exercise, sets = sets, weightGrams = weightGrams, reps = reps)

    private fun errorOf(result: ExerciseSetValidation) = (result as ExerciseSetValidation.Invalid).error

    private fun validOf(result: ExerciseSetValidation) = (result as ExerciseSetValidation.Valid).set

    // S10-A01 / S10-R01
    @Test
    fun negativeWeightIsRejected() {
        assertEquals(ExerciseSetError.WEIGHT_NEGATIVE, errorOf(add(weightGrams = -250)))
    }

    @Test
    fun weightNotMultipleOf250GramsIsRejected() {
        assertEquals(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP, errorOf(add(weightGrams = 100)))
        assertEquals(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP, errorOf(add(weightGrams = 62_600)))
    }

    @Test
    fun weightsOf0And250And62500GramsAreAccepted() {
        assertEquals(0, validOf(add(weightGrams = 0)).weightGrams)
        assertEquals(250, validOf(add(weightGrams = 250)).weightGrams)
        assertEquals(62_500, validOf(add(weightGrams = 62_500)).weightGrams)
    }

    // S10-A02 / S10-R02
    @Test
    fun bodyweightSetWithZeroGramsMeansNoAddedWeight() {
        val set = validOf(add(exercise = pullUp, weightGrams = 0))
        assertEquals(2L, set.exerciseId)
        assertEquals(0, set.weightGrams)
    }

    // S10-A03 / S10-R03
    @Test
    fun zeroOrNegativeRepsAreRejected() {
        assertEquals(ExerciseSetError.REPS_NOT_POSITIVE, errorOf(add(reps = 0)))
        assertEquals(ExerciseSetError.REPS_NOT_POSITIVE, errorOf(add(reps = -1)))
    }

    // S10-R15
    @Test
    fun upperLimitsForRepsAndWeight() {
        validOf(add(reps = 999))
        assertEquals(ExerciseSetError.REPS_TOO_HIGH, errorOf(add(reps = 1000)))
        validOf(add(weightGrams = 1_000_000))
        assertEquals(ExerciseSetError.WEIGHT_TOO_HIGH, errorOf(add(weightGrams = 1_000_250)))
    }

    // S10-A08 / S10-R09, S10-R24
    @Test
    fun deletingASetKeepsOtherOrdersAndNextSetGetsMaxPlusOne() {
        val sets = (1..3).map { ExerciseSet(it.toLong(), 10, 1, order = it, weightGrams = 60_000, reps = 10) }
        val afterDeletion = sets.filterNot { it.order == 2 }

        assertEquals(listOf(1, 3), afterDeletion.map { it.order })
        assertEquals(4, nextSetOrder(sessionId = 10, sets = afterDeletion))
        assertEquals(4, validOf(add(sets = afterDeletion)).order)
    }

    @Test
    fun firstSetOfASessionGetsOrder1AndOtherSessionsDoNotCount() {
        val otherSession = listOf(ExerciseSet(1, 99, 1, order = 7, weightGrams = 0, reps = 5))
        assertEquals(1, nextSetOrder(sessionId = 10, sets = otherSession))
    }

    // S10-A09 / S10-R11
    @Test
    fun addingASetOfAnArchivedExerciseIsRejected() {
        assertEquals(ExerciseSetError.EXERCISE_ARCHIVED, errorOf(add(exercise = archivedRow)))
    }

    @Test
    fun correctingTheWeightOfAnExistingSetOfAnArchivedExerciseIsAccepted() {
        val existing = ExerciseSet(5, 10, exerciseId = 3, order = 1, weightGrams = 40_000, reps = 8)
        val corrected = validateSetCorrection(existing, archivedRow, weightGrams = 42_500, reps = 8)
        assertEquals(existing.copy(weightGrams = 42_500), (corrected as ExerciseSetValidation.Valid).set)
    }

    @Test
    fun changingASetToAnArchivedExerciseIsRejected() {
        val existing = ExerciseSet(5, 10, exerciseId = 1, order = 1, weightGrams = 40_000, reps = 8)
        val result = validateSetCorrection(existing, archivedRow, weightGrams = 40_000, reps = 8)
        assertEquals(ExerciseSetError.EXERCISE_ARCHIVED, errorOf(result))
    }

    @Test
    fun correctionAlsoChecksWeightAndReps() {
        val existing = ExerciseSet(5, 10, exerciseId = 1, order = 1, weightGrams = 40_000, reps = 8)
        assertEquals(ExerciseSetError.REPS_NOT_POSITIVE, errorOf(validateSetCorrection(existing, bench, 40_000, 0)))
    }
}

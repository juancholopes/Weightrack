package com.juancholopes.weightrack.feature.session.domain

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseValidation
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.domain.deleteExercise
import com.juancholopes.weightrack.feature.catalog.domain.validateExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SessionQueriesTest {

    private val bench = Exercise(1, "Press de banca", Equipment.BARBELL, null)
    private val squat = Exercise(2, "Sentadilla", Equipment.BARBELL, null)
    private val benchMuscles = listOf(ExerciseMuscle(1, Muscle.CHEST, isPrimary = true))

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val oct5 = LocalDate.of(2026, 10, 5)

    // Ids grow in creation order (S10-R14): session 4 was created after session 3 on the same day.
    private val s1 = WorkoutSession(1, oct1, null)
    private val s3 = WorkoutSession(3, oct5, null)
    private val s4 = WorkoutSession(4, oct5, null)
    private val s2 = WorkoutSession(2, oct1.minusDays(3), null)
    private val sessions = listOf(s3, s1, s4, s2)

    private fun set(id: Long, session: WorkoutSession, exercise: Exercise, order: Int, kg: Int = 60) =
        ExerciseSet(id, session.id, exercise.id, order, weightGrams = kg * 1000, reps = 10)

    private val sets = listOf(
        set(10, s2, bench, order = 1, kg = 50),
        set(11, s1, bench, order = 2, kg = 55),
        set(12, s1, bench, order = 1, kg = 52),
        set(13, s3, bench, order = 1, kg = 57),
        set(14, s4, squat, order = 1, kg = 80),
        set(15, s4, bench, order = 3, kg = 60),
        set(16, s4, bench, order = 2, kg = 58),
    )

    // S10-A15 / S10-R25, S10-R14
    @Test
    fun lastPerformanceReturnsAllSetsOfTheMostRecentSessionOrderedByOrder() {
        val last = lastPerformance(bench.id, sessions, sets)!!
        assertEquals(s4, last.session)
        assertEquals(listOf(16L, 15L), last.sets.map { it.id })
    }

    @Test
    fun lastPerformanceIsEmptyWithoutSets() {
        assertNull(lastPerformance(exerciseId = 99, sessions = sessions, sets = sets))
    }

    // S10-A15 / S10-R26
    @Test
    fun lastPerformanceFromAnOngoingSessionOnlyConsidersEarlierSessions() {
        val fromS4 = lastPerformance(bench.id, sessions, sets, before = s4)!!
        assertEquals(s3, fromS4.session)
        assertEquals(listOf(13L), fromS4.sets.map { it.id })

        val fromS3 = lastPerformance(bench.id, sessions, sets, before = s3)!!
        assertEquals(s1, fromS3.session)
        assertEquals(listOf(12L, 11L), fromS3.sets.map { it.id })
    }

    // S10-A16 / S10-R27
    @Test
    fun evolutionReturnsEverySetWithItsDateOrderedByDateSessionIdAndOrder() {
        val evolution = exerciseEvolution(bench.id, sessions, sets)
        assertEquals(listOf(10L, 12L, 11L, 13L, 16L, 15L), evolution.entries.map { it.set.id })
        assertEquals(listOf(s2.date, oct1, oct1, oct5, oct5, oct5), evolution.entries.map { it.date })
    }

    // S10-A17 / S10-R28
    @Test
    fun historyIsSortedByDateDescendingThenIdDescending() {
        assertEquals(listOf(s4, s3, s1, s2), sessionHistory(sessions))
    }

    // S10-A18 / S10-R29
    @Test
    fun setsOfASessionAreSortedByOrderAndIncludeArchivedExercises() {
        val archivedSquat = squat.copy(archived = true)
        val result = sessionSets(s4.id, sets, listOf(bench, archivedSquat))
        assertEquals(listOf(14L, 16L, 15L), result.map { it.first.id })
        assertEquals(listOf(archivedSquat, bench, bench), result.map { it.second })
    }

    // S10-A14 / S10-R23
    @Test
    fun deletingASessionDeletesAllItsSets() {
        val deletion = deleteSession(s4, sets)
        assertEquals(s4, deletion.session)
        assertEquals(listOf(14L, 15L, 16L), deletion.sets.map { it.id }.sorted())
    }

    // S10-A11 / S10-R17, S10-R18
    @Test
    fun archivedExerciseKeepsItsSetsInLastPerformanceAndEvolution() {
        val archived = (deleteExercise(bench, benchMuscles, hasSets = true) as ExerciseDeletion.Archive).exercise
        assertEquals(true, archived.archived)
        assertEquals(listOf(16L, 15L), lastPerformance(archived.id, sessions, sets)!!.sets.map { it.id })
        assertEquals(6, exerciseEvolution(archived.id, sessions, sets).entries.size)
    }

    // S10-A13 / S10-R22, S01-R07
    @Test
    fun renamingAnExerciseKeepsItsIdLastPerformanceAndEvolution() {
        val before = lastPerformance(bench.id, sessions, sets)
        val evolutionBefore = exerciseEvolution(bench.id, sessions, sets)

        val renamed = validateExercise(
            id = bench.id,
            name = "Press plano",
            equipment = Equipment.BARBELL,
            muscles = benchMuscles,
            notes = null,
            existing = listOf(bench, squat),
        ) as ExerciseValidation.Valid

        assertEquals(bench.id, renamed.exercise.id)
        assertEquals("Press plano", renamed.exercise.name)
        assertEquals(before, lastPerformance(renamed.exercise.id, sessions, sets))
        assertEquals(evolutionBefore, exerciseEvolution(renamed.exercise.id, sessions, sets))
    }
}

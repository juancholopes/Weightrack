package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseDeletion
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of [ExerciseRepository] (S20-R42) as Room implements it (Spec 31, S31-R06). Runs against the
 * fake in `src/test` and against Room in `src/androidTest`.
 */
abstract class ExerciseRepositoryContract : RepositoryContract() {

    // S20-R17, S20-R38
    @Test
    fun create_savesExerciseAndMusclesUnderANewId() = runTest {
        val result = exercises.create(
            exercise("Press de banca"),
            listOf(muscle(Muscle.CHEST), muscle(Muscle.TRICEPS, isPrimary = false)),
        )

        val id = (result as ExerciseWriteResult.Saved).id
        assertTrue(id > 0)
        assertEquals(exercise("Press de banca", id = id), exercises.observeExercise(id).first())
        assertEquals(
            setOf(ExerciseMuscle(id, Muscle.CHEST, isPrimary = true), ExerciseMuscle(id, Muscle.TRICEPS, isPrimary = false)),
            exercises.observeMuscles(id).first().toSet(),
        )
    }

    // S20-R17
    @Test
    fun create_neverReusesTheIdOfADeletedExercise() = runTest {
        createExercise("Press de banca")
        val curl = createExercise("Curl")
        exercises.delete(curl.id)

        val squat = createExercise("Sentadilla")

        assertTrue(squat.id > curl.id)
    }

    // S20-R41, S10-R05
    @Test
    fun create_withNameOfActiveExercise_returnsNameTaken_andWritesNothing() = runTest {
        val bench = createExercise("Press de banca")

        val result = exercises.create(exercise("press  DE BANCA"), listOf(muscle(Muscle.TRICEPS)))

        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN), result)
        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
        assertEquals(listOf(Muscle.CHEST), exercises.observeMuscles(bench.id).first().map { it.muscle })
    }

    // S20-R41, S10-R21
    @Test
    fun create_withNameOfArchivedExercise_returnsNameTakenByArchived() = runTest {
        val bench = archive(createExercise("Press de banca"))

        val result = exercises.create(exercise("Press de banca"), listOf(muscle(Muscle.CHEST)))

        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN_BY_ARCHIVED), result)
        assertEquals(listOf(bench), exercises.observeArchived().first())
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())
    }

    // S20-R12, S10-R05: accents count
    @Test
    fun create_withAccentedVariantOfAName_isSaved() = runTest {
        createExercise("Press de banca")

        val result = exercises.create(exercise("Prés de banca"), listOf(muscle(Muscle.CHEST)))

        assertTrue(result is ExerciseWriteResult.Saved)
    }

    // S20-R25, S20-R38
    @Test
    fun create_withRepeatedMuscle_fails_andWritesNothing() = runTest {
        assertFails {
            exercises.create(
                exercise("Press de banca"),
                listOf(muscle(Muscle.CHEST), muscle(Muscle.CHEST, isPrimary = false)),
            )
        }

        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())
    }

    // S20-R38, S10-R22
    @Test
    fun update_keepsTheId_andReplacesTheMuscles() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            muscle(Muscle.CHEST),
            muscle(Muscle.TRICEPS, isPrimary = false),
        )
        val edited = bench.copy(name = "Press inclinado", equipment = Equipment.DUMBBELL, notes = "Banco a 30 grados")

        val result = exercises.update(edited, listOf(muscle(Muscle.SHOULDERS)))

        assertEquals(ExerciseWriteResult.Saved(bench.id), result)
        assertEquals(edited, exercises.observeExercise(bench.id).first())
        assertEquals(
            listOf(ExerciseMuscle(bench.id, Muscle.SHOULDERS, isPrimary = true)),
            exercises.observeMuscles(bench.id).first(),
        )
    }

    // S20-R41
    @Test
    fun update_toNameOfAnotherExercise_returnsNameTaken_andKeepsTheStoredExercise() = runTest {
        val bench = createExercise("Press de banca")
        createExercise("Sentadilla")

        val result = exercises.update(bench.copy(name = "SENTADILLA"), listOf(muscle(Muscle.QUADRICEPS)))

        assertEquals(ExerciseWriteResult.Rejected(ExerciseError.NAME_TAKEN), result)
        assertEquals(bench, exercises.observeExercise(bench.id).first())
        assertEquals(listOf(Muscle.CHEST), exercises.observeMuscles(bench.id).first().map { it.muscle })
    }

    // S20-R24: the unique index accepts a change of case on the same exercise
    @Test
    fun update_changingOnlyTheCaseOfItsOwnName_isSaved() = runTest {
        val bench = createExercise("Press de banca")

        val result = exercises.update(bench.copy(name = "Press De Banca"), listOf(muscle(Muscle.CHEST)))

        assertEquals(ExerciseWriteResult.Saved(bench.id), result)
        assertEquals("Press De Banca", exercises.observeExercise(bench.id).first()?.name)
    }

    @Test
    fun update_ofMissingExercise_fails() = runTest {
        assertFails { exercises.update(exercise("Press de banca", id = 42), listOf(muscle(Muscle.CHEST))) }

        assertNull(exercises.observeExercise(42).first())
    }

    // S20-R33, S10-R30
    @Test
    fun observeActiveCatalog_excludesArchived_sortsByNameIgnoringCase_andCombinesFilters() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            muscle(Muscle.CHEST),
            muscle(Muscle.TRICEPS, isPrimary = false),
        )
        val curl = createExercise("curl con mancuerna", Equipment.DUMBBELL, muscle(Muscle.BICEPS))
        val lunge = createExercise("Zancada", Equipment.BODYWEIGHT, muscle(Muscle.QUADRICEPS))
        archive(createExercise("Aperturas", Equipment.DUMBBELL, muscle(Muscle.CHEST)))

        assertEquals(listOf(curl, bench, lunge), exercises.observeActiveCatalog().first())
        assertEquals(listOf(bench), exercises.observeActiveCatalog(muscle = Muscle.TRICEPS).first())
        assertEquals(listOf(bench), exercises.observeActiveCatalog(muscle = Muscle.CHEST).first())
        assertEquals(listOf(curl), exercises.observeActiveCatalog(equipment = Equipment.DUMBBELL).first())
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog(Muscle.CHEST, Equipment.DUMBBELL).first())
    }

    // S20-R34, S10-R31
    @Test
    fun observeArchived_listsOnlyArchivedExercisesByName() = runTest {
        val squat = archive(createExercise("sentadilla"))
        createExercise("Curl")
        val bench = archive(createExercise("Press de banca"))

        assertEquals(listOf(bench, squat), exercises.observeArchived().first())
    }

    // S20-R40, S10-R17
    @Test
    fun delete_withSets_archivesTheExercise_andKeepsItsMusclesAndSets() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            muscle(Muscle.CHEST),
            muscle(Muscle.TRICEPS, isPrimary = false),
        )
        val session = createSession(oct10, set(bench.id, order = 1))

        val result = exercises.delete(bench.id)

        val archived = bench.copy(archived = true)
        assertEquals(ExerciseDeletion.Archive(archived), result)
        assertEquals(archived, exercises.observeExercise(bench.id).first())
        assertEquals(2, exercises.observeMuscles(bench.id).first().size)
        assertEquals(1, sessions.setsOfSession(session.id).size)
    }

    // S20-R40, S10-R20
    @Test
    fun delete_withoutSets_deletesTheExerciseAndItsMuscles() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            muscle(Muscle.CHEST),
            muscle(Muscle.TRICEPS, isPrimary = false),
        )

        val result = exercises.delete(bench.id) as ExerciseDeletion.Delete

        assertEquals(bench, result.exercise)
        assertEquals(setOf(Muscle.CHEST, Muscle.TRICEPS), result.muscles.map { it.muscle }.toSet())
        assertNull(exercises.observeExercise(bench.id).first())
        assertEquals(emptyList<ExerciseMuscle>(), exercises.observeMuscles(bench.id).first())
    }

    // Spec 20, "Hechos de la implementación"
    @Test
    fun delete_ofMissingExercise_throwsIllegalStateException() = runTest {
        val error = assertFails { exercises.delete(42) }

        assertTrue("Expected IllegalStateException, got $error", error is IllegalStateException)
    }

    // S20-R35, S10-R19
    @Test
    fun unarchive_returnsTheExerciseToTheActiveCatalog() = runTest {
        val bench = archive(createExercise("Press de banca"))

        exercises.unarchive(bench.id)

        assertEquals(listOf(bench.copy(archived = false)), exercises.observeActiveCatalog().first())
        assertEquals(emptyList<Exercise>(), exercises.observeArchived().first())
    }

    @Test
    fun unarchive_ofMissingExercise_doesNothing() = runTest {
        val bench = createExercise("Press de banca")

        exercises.unarchive(42)

        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
    }

    // SF01-A02 / SF01-R11, S20-A41 / S20-R48
    @Test
    fun observeHasSets_followsTheSetsOfTheExercise() = runTest {
        val bench = createExercise("Press de banca")
        val squat = createExercise("Sentadilla", Equipment.BARBELL, muscle(Muscle.QUADRICEPS))
        assertFalse(exercises.observeHasSets(bench.id).first())

        val session = createSession(oct10, set(bench.id, order = 1))

        assertTrue(exercises.observeHasSets(bench.id).first())
        assertFalse(exercises.observeHasSets(squat.id).first())

        sessions.deleteSet(sessions.setsOfSession(session.id).single().id)

        assertFalse(exercises.observeHasSets(bench.id).first())
    }

    // SF01-A02 / SF01-R11, S20-R48: the subscription starts before the write, so the new value is a second emission
    @Test
    fun observeHasSets_emitsAgainAfterASetIsAdded() = runTest {
        val bench = createExercise("Press de banca")
        val hasSets = async(start = CoroutineStart.UNDISPATCHED) {
            exercises.observeHasSets(bench.id).first { it }
        }

        createSession(oct10, set(bench.id, order = 1))

        assertTrue(hasSets.await())
    }

    // SF02-A03 / SF02-R18, D2: the muscles of every exercise in one read, archived exercises included
    @Test
    fun observeAllMuscles_returnsTheMusclesOfAllExercises() = runTest {
        val bench = createExercise(
            "Press de banca",
            Equipment.BARBELL,
            muscle(Muscle.CHEST),
            muscle(Muscle.TRICEPS, isPrimary = false),
        )
        val squat = archive(createExercise("Sentadilla", Equipment.BARBELL, muscle(Muscle.QUADRICEPS)))

        val all = exercises.observeAllMuscles().first()

        assertEquals(
            setOf(
                ExerciseMuscle(bench.id, Muscle.CHEST, isPrimary = true),
                ExerciseMuscle(bench.id, Muscle.TRICEPS, isPrimary = false),
                ExerciseMuscle(squat.id, Muscle.QUADRICEPS, isPrimary = true),
            ),
            all.toSet(),
        )
        assertEquals(3, all.size)
    }

    // SF02-A03 / SF02-R18: with no exercises there are no muscles
    @Test
    fun observeAllMuscles_withoutExercises_isEmpty() = runTest {
        assertEquals(emptyList<ExerciseMuscle>(), exercises.observeAllMuscles().first())
    }

    // SF02-A03 / SF02-R18: the subscription starts before the write, so the new value is a second emission
    @Test
    fun observeAllMuscles_emitsAgainAfterCreate() = runTest {
        val muscles = async(start = CoroutineStart.UNDISPATCHED) {
            exercises.observeAllMuscles().first { it.isNotEmpty() }
        }

        val result = exercises.create(exercise("Press de banca"), listOf(muscle(Muscle.CHEST)))

        val id = (result as ExerciseWriteResult.Saved).id
        assertEquals(listOf(ExerciseMuscle(id, Muscle.CHEST, isPrimary = true)), muscles.await())
    }

    // S20-A33: the subscription starts before the write, so the new value is a second emission
    @Test
    fun observeActiveCatalog_emitsAgainAfterCreate() = runTest {
        val names = async(start = CoroutineStart.UNDISPATCHED) {
            exercises.observeActiveCatalog().map { list -> list.map { it.name } }.first { it.isNotEmpty() }
        }

        exercises.create(exercise("Press de banca"), listOf(muscle(Muscle.CHEST)))

        assertEquals(listOf("Press de banca"), names.await())
    }
}

package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** S31-A02 / S31-R06, S31-R07: the fake honors the same contract as Room. */
class FakeExerciseRepositoryTest : ExerciseRepositoryContract() {

    private lateinit var fake: FakeExerciseRepository

    override fun createRepositories(): Repositories {
        val store = InMemoryStore()
        fake = FakeExerciseRepository(store)
        return Repositories(fake, FakeSessionRepository(store))
    }

    // SF01-A09 / SF01-R16, S31-R08
    @Test
    fun failNextWrite_makesOnlyTheNextWriteThrow_andWritesNothing() = runTest {
        val failure = IllegalStateException("Disk full")
        fake.failNextWrite(failure)

        val error = assertFails { exercises.create(exercise("Press de banca"), listOf(muscle(Muscle.CHEST))) }

        assertSame(failure, error)
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())
        val retry = exercises.create(exercise("Press de banca"), listOf(muscle(Muscle.CHEST)))
        assertTrue(retry is ExerciseWriteResult.Saved)
    }

    // SF01-R16, S31-R08: every write honors it, not only create
    @Test
    fun failNextWrite_appliesToUpdateDeleteAndUnarchive() = runTest {
        val bench = createExercise("Press de banca")

        fake.failNextWrite()
        assertFails { exercises.update(bench.copy(name = "Press inclinado"), listOf(muscle(Muscle.CHEST))) }
        fake.failNextWrite()
        assertFails { exercises.delete(bench.id) }
        fake.failNextWrite()
        assertFails { exercises.unarchive(bench.id) }

        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
    }
}

package com.juancholopes.weightrack.testing

import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/** S31-A02 / S31-R06, S31-R07: the fake honors the same contract as Room. */
class FakeSessionRepositoryTest : SessionRepositoryContract() {

    private lateinit var fake: FakeSessionRepository

    override fun createRepositories(): Repositories {
        val store = InMemoryStore()
        fake = FakeSessionRepository(store)
        return Repositories(FakeExerciseRepository(store), fake)
    }

    private lateinit var bench: Exercise

    /** The contract already created "Press de banca" before each test. */
    @Before
    fun findBench() = runTest {
        bench = exercises.observeActiveCatalog().first().first { it.name == "Press de banca" }
    }

    // SF02-A14 / SF02-R22, S31-R08
    @Test
    fun failNextWrite_makesOnlyTheNextWriteThrow_andWritesNothing() = runTest {
        val failure = IllegalStateException("Disk full")
        fake.failNextWrite(failure)

        val error = assertFails { createSession(oct10, set(bench.id, order = 1)) }

        assertSame(failure, error)
        assertEquals(emptyList<WorkoutSession>(), sessions.observeHistory().first())
        val retry = createSession(oct10, set(bench.id, order = 1))
        assertEquals(listOf(retry), sessions.observeHistory().first())
        assertEquals(1, sessions.setsOfSession(retry.id).size)
    }

    // SF02-A14 / SF02-R22, S31-R08: every write honors it, not only createSession
    @Test
    fun failNextWrite_appliesToEveryWrite() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        val stored = sessions.setsOfSession(session.id).single()

        fake.failNextWrite()
        assertFails { sessions.updateSession(session.copy(notes = "Otra")) }
        fake.failNextWrite()
        assertFails { sessions.addSet(set(bench.id, order = 2, sessionId = session.id)) }
        fake.failNextWrite()
        assertFails { sessions.updateSet(stored.copy(weightGrams = 100_000)) }
        fake.failNextWrite()
        assertFails { sessions.deleteSet(stored.id) }
        fake.failNextWrite()
        assertFails { sessions.deleteSession(session.id) }

        assertEquals(listOf(session), sessions.observeHistory().first())
        assertEquals(listOf(stored), sessions.setsOfSession(session.id))
    }

    // SF02-A14 / SF02-R22: the failure is consumed by one write; reads never trigger it
    @Test
    fun failNextWrite_isNotTriggeredByReads_andIsConsumedOnce() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        fake.failNextWrite()

        sessions.observeHistory().first()
        sessions.setsOfSession(session.id)
        assertFails { sessions.deleteSet(sessions.setsOfSession(session.id).single().id) }
        sessions.deleteSet(sessions.setsOfSession(session.id).single().id)

        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(session.id))
    }

    // SF03-A04 / SF03-R08, S31-R08: a provoked read failure makes the next read fail and later reads work
    @Test
    fun failNextRead_makesOnlyTheNextReadThrow_andLaterReadsWork() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))
        val failure = IllegalStateException("Disk unreadable")
        fake.failNextRead(failure)

        val error = assertFails { sessions.observeLastPerformance(bench.id).first() }

        assertSame(failure, error)
        assertEquals(session, sessions.observeLastPerformance(bench.id).first()?.session)
    }

    // SF03-A04 / SF03-R08, S31-R08: every read honors it, not only observeLastPerformance
    @Test
    fun failNextRead_appliesToEveryRead() = runTest {
        val session = createSession(oct10, set(bench.id, order = 1))

        fake.failNextRead()
        assertFails { sessions.observeHistory().first() }
        fake.failNextRead()
        assertFails { sessions.observeSession(session.id).first() }
        fake.failNextRead()
        assertFails { sessions.observeSessionSets(session.id).first() }
        fake.failNextRead()
        assertFails { sessions.observeLastPerformance(bench.id, before = null).first() }
        fake.failNextRead()
        assertFails { sessions.observeEvolution(bench.id).first() }
        fake.failNextRead()
        assertFails { sessions.observeLatestSessionPeaks().first() }
        fake.failNextRead()
        assertFails { sessions.setsOfSession(session.id) }

        assertEquals(listOf(session), sessions.observeHistory().first())
    }

    // SF03-A04 / SF03-R08: the failure is not consumed by writes, and a read failure does not touch writes
    @Test
    fun failNextRead_isIndependentOfWrites() = runTest {
        val failure = IllegalStateException("Disk unreadable")
        fake.failNextRead(failure)

        val session = createSession(oct10, set(bench.id, order = 1))

        assertSame(failure, assertFails { sessions.observeHistory().first() })
        assertEquals(listOf(session), sessions.observeHistory().first())
    }

    // SF03-A04 / SF03-R08: the failure happens when the flow is collected, so `catch` can turn it into a state
    @Test
    fun failNextRead_isRaisedWhenTheFlowIsCollected_notWhenItIsCreated() = runTest {
        val failure = IllegalStateException("Disk unreadable")
        fake.failNextRead(failure)

        val flow = sessions.observeLastPerformance(bench.id)

        assertSame(failure, assertFails { flow.first() })
    }
}

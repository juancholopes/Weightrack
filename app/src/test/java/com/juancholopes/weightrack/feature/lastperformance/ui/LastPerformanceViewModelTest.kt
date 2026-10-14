package com.juancholopes.weightrack.feature.lastperformance.ui

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.lastperformance.ui.LastPerformanceUiState.Error
import com.juancholopes.weightrack.feature.lastperformance.ui.LastPerformanceUiState.FirstTime
import com.juancholopes.weightrack.feature.lastperformance.ui.LastPerformanceUiState.Loading
import com.juancholopes.weightrack.feature.lastperformance.ui.LastPerformanceUiState.Shown
import com.juancholopes.weightrack.feature.session.domain.AddSetUseCase
import com.juancholopes.weightrack.feature.session.domain.CorrectSetUseCase
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.ui.logger.SetLoggerViewModel
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import com.juancholopes.weightrack.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The "Última vez" section reads a past session, never the one being logged (S10-R25, S10-R26, S31-R11). */
@OptIn(ExperimentalCoroutinesApi::class)
class LastPerformanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    /** Sunday. */
    private val oct11 = LocalDate.of(2026, 10, 11)
    private val oct10 = LocalDate.of(2026, 10, 10)
    private val oct8 = LocalDate.of(2026, 10, 8)
    private val oct6 = LocalDate.of(2026, 10, 6)
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC)

    private fun shown(date: LocalDate, vararg sets: Pair<Int, Int>) =
        Shown(date, oct11, sets.map { (weightGrams, reps) -> SetValues(weightGrams, reps) })

    // --- Loading ---

    // SF03-A04 / SF03-R05: nothing is read before the first emission, so the section only reserves its height
    @Test
    fun uiState_beforeTheFirstData_isLoading() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))

        val viewModel = LastPerformanceViewModel(bench.id, null, sessions, clock)

        assertEquals(Loading, viewModel.uiState.value)
    }

    // --- Without a session (SF03-R03) ---

    // SF03-A01 / SF03-R03, S10-R25: the most recent earlier session with the exercise and all its sets, by `order`
    @Test
    fun withoutSessionId_showsTheMostRecentSessionWithTheExercise_withAllItsSetsInOrder() = runTest {
        val bench = newExercise()
        val squat = newExercise("Sentadilla")
        newSession(oct6, set(bench.id, 1, 50_000, 12))
        newSession(
            oct8,
            set(bench.id, 3, 60_000, 8),
            set(squat.id, 4, 100_000, 5),
            set(bench.id, 1, 60_000, 10),
            set(bench.id, 2, 62_500, 10),
        )
        newSession(oct10, set(squat.id, 1, 100_000, 5))

        val viewModel = newViewModel(bench.id, sessionId = null)

        assertEquals(
            shown(oct8, 60_000 to 10, 62_500 to 10, 60_000 to 8),
            viewModel.uiState.value,
        )
    }

    // SF03-A01 / SF03-R05, S42-R26: the state carries the date of the clock, for "Hoy" and "Ayer"
    @Test
    fun shown_carriesTheDateOfTheSessionAndTheDayOfTheClock() = runTest {
        val bench = newExercise()
        newSession(oct10, set(bench.id, 1, 60_000, 10))

        val viewModel = newViewModel(bench.id, sessionId = null)

        assertEquals(Shown(date = oct10, today = oct11, sets = listOf(SetValues(60_000, 10))), viewModel.uiState.value)
    }

    // SF03-A01 / SF03-R03: the section follows the database, so a newer session replaces what it shows
    @Test
    fun uiState_followsTheDatabase() = runTest {
        val bench = newExercise()
        newSession(oct6, set(bench.id, 1, 50_000, 12))
        val viewModel = newViewModel(bench.id, sessionId = null)
        assertEquals(shown(oct6, 50_000 to 12), viewModel.uiState.value)

        newSession(oct8, set(bench.id, 1, 60_000, 10))

        assertEquals(shown(oct8, 60_000 to 10), viewModel.uiState.value)
    }

    // --- With a session (SF03-R03) ---

    // SF03-A02 / SF03-R03, S10-R26, S40-A32: from an old session it shows the one before it, not it nor the later ones
    @Test
    fun withASession_showsTheSessionBeforeIt() = runTest {
        val bench = newExercise()
        newSession(oct6, set(bench.id, 1, 50_000, 12))
        val middle = newSession(oct8, set(bench.id, 1, 60_000, 10))
        newSession(oct10, set(bench.id, 1, 65_000, 6))

        val viewModel = newViewModel(bench.id, sessionId = middle.id)

        assertEquals(shown(oct6, 50_000 to 12), viewModel.uiState.value)
    }

    // SF03-A02 / SF03-R03, S10-R14, S40-A32: two sessions of the same day, the one with the higher id is the most recent
    @Test
    fun withTwoSessionsOfTheSameDay_theOneWithTheHigherIdIsTheMostRecent() = runTest {
        val bench = newExercise()
        val first = newSession(oct8, set(bench.id, 1, 60_000, 10))
        val second = newSession(oct8, set(bench.id, 1, 62_500, 8))

        val withoutSession = newViewModel(bench.id, sessionId = null)
        val fromTheSecond = newViewModel(bench.id, sessionId = second.id)
        val fromTheFirst = newViewModel(bench.id, sessionId = first.id)

        assertEquals(shown(oct8, 62_500 to 8), withoutSession.uiState.value)
        assertEquals(shown(oct8, 60_000 to 10), fromTheSecond.uiState.value)
        assertEquals(FirstTime, fromTheFirst.uiState.value)
    }

    // SF03-A02 / SF03-R03, S10-R18, S40-R34: the sets of an archived exercise are still its last performance
    @Test
    fun archivedExercise_stillShowsItsSets() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 60_000, 8))
        archive(bench)

        val viewModel = newViewModel(bench.id, sessionId = null)

        assertEquals(shown(oct8, 60_000 to 10, 60_000 to 8), viewModel.uiState.value)
    }

    // SF03-R03: a session that does not exist (deleted while the logger is open) is the same as no session
    @Test
    fun withAnUnknownSessionId_behavesAsWithoutASession() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))

        val viewModel = newViewModel(bench.id, sessionId = 999L)

        assertEquals(shown(oct8, 60_000 to 10), viewModel.uiState.value)
    }

    // --- The set just saved (SF03-R04) ---

    // SF03-A03 / SF03-R04, S10-R26: with the set of today already saved, the ViewModel of that session shows the previous one
    @Test
    fun withASetOfTodayAlreadySaved_theViewModelOfThatSessionShowsThePreviousSession_notToday() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 60_000, 8))
        val today = newSession(oct11, set(bench.id, 1, 70_000, 5))

        val viewModel = newViewModel(bench.id, sessionId = today.id)

        assertEquals(shown(oct8, 60_000 to 10, 60_000 to 8), viewModel.uiState.value)
    }

    // SF03-A03 / SF03-R04: the first set creates the session, so the section changes of ViewModel and does not show the new set
    @Test
    fun whenTheFirstSetCreatesTheSession_theViewModelOfTheNewSessionDoesNotShowTheNewSet() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val beforeTheFirstSet = newViewModel(bench.id, sessionId = null)
        assertEquals(shown(oct8, 60_000 to 10), beforeTheFirstSet.uiState.value)

        val today = newSession(oct11, set(bench.id, 1, 70_000, 5))
        val afterTheFirstSet = newViewModel(bench.id, sessionId = today.id)

        assertEquals(shown(oct8, 60_000 to 10), afterTheFirstSet.uiState.value)
        // The old ViewModel would show the set just written: that is why the section is keyed by the session (SF03-R04).
        assertEquals(shown(oct11, 70_000 to 5), beforeTheFirstSet.uiState.value)
    }

    // SF03-A03 / SF03-R03, S01-A06: renaming the exercise does not change its last performance
    @Test
    fun afterRenamingTheExercise_theLastPerformanceIsTheSame() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newViewModel(bench.id, sessionId = null)
        val before = viewModel.uiState.value

        exercises.update(bench.copy(name = "Press plano"), listOf(ExerciseMuscle(bench.id, Muscle.CHEST, isPrimary = true)))

        assertEquals(before, viewModel.uiState.value)
        assertEquals(shown(oct8, 60_000 to 10), viewModel.uiState.value)
    }

    // --- First time and error (SF03-R05, SF03-R08) ---

    // SF03-A04 / SF03-R05, S40-R35: an exercise without sets is the first time
    @Test
    fun withoutSetsOfTheExercise_isFirstTime() = runTest {
        val bench = newExercise()
        val squat = newExercise("Sentadilla")
        newSession(oct8, set(squat.id, 1, 100_000, 5))

        val viewModel = newViewModel(bench.id, sessionId = null)

        assertEquals(FirstTime, viewModel.uiState.value)
    }

    // SF03-A04 / SF03-R05, S10-R26: the only session with the exercise is the open one, so there is no earlier one
    @Test
    fun whenTheOnlySessionWithTheExerciseIsTheOpenOne_isFirstTime() = runTest {
        val bench = newExercise()
        val today = newSession(oct11, set(bench.id, 1, 60_000, 10))

        val viewModel = newViewModel(bench.id, sessionId = today.id)

        assertEquals(FirstTime, viewModel.uiState.value)
    }

    // SF03-A04 / SF03-R08, S30-R23: if the read fails the state is Error, and the failure is not rethrown
    @Test
    fun whenTheReadFails_isError() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        sessions.failNextRead()

        val viewModel = newViewModel(bench.id, sessionId = null)

        assertEquals(Error, viewModel.uiState.value)
    }

    // SF03-A04 / SF03-R08: it also fails when the first read is the one of the session
    @Test
    fun whenTheReadOfTheSessionFails_isError() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val today = newSession(oct11, set(bench.id, 1, 70_000, 5))
        sessions.failNextRead()

        val viewModel = newViewModel(bench.id, sessionId = today.id)

        assertEquals(Error, viewModel.uiState.value)
    }

    // --- Coherence with the proposal of the logger (SF03-R09) ---

    // SF03-A05 / SF03-R09: without a session of today, the first set the section shows is the one the logger proposes
    @Test
    fun firstSet_matchesLoggerProposal() = runTest {
        val bench = newExercise()
        newSession(oct6, set(bench.id, 1, 50_000, 12))
        newSession(oct8, set(bench.id, 2, 62_500, 8), set(bench.id, 1, 60_000, 10))
        val logger = newLogger(bench.id, sessionId = 0L)
        assertNull(logger.uiState.value.resolvedSessionId)

        val section = newViewModel(bench.id, sessionId = logger.uiState.value.resolvedSessionId)

        assertEquals(proposalOf(logger), firstSetOf(section))
        assertEquals(60_000 to 10, firstSetOf(section))
    }

    // SF03-A05 / SF03-R09, S10-R26: with a session of today that has no sets of this exercise, both exclude it
    @Test
    fun firstSet_matchesLoggerProposal_withASessionOfTodayWithoutTheExercise() = runTest {
        val bench = newExercise()
        val squat = newExercise("Sentadilla")
        newSession(oct8, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 60_000, 8))
        newSession(oct11, set(squat.id, 1, 100_000, 5))
        val logger = newLogger(bench.id, sessionId = 0L)
        val resolved = logger.uiState.value.resolvedSessionId
        assertNotNull(resolved)

        val section = newViewModel(bench.id, sessionId = resolved)

        assertEquals(proposalOf(logger), firstSetOf(section))
        assertEquals(60_000 to 10, firstSetOf(section))
    }

    // SF03-A05 / SF03-R09, S10-R26: opened from an old session without this exercise, both look at the session before it
    @Test
    fun firstSet_matchesLoggerProposal_fromAnOldSessionWithoutTheExercise() = runTest {
        val bench = newExercise()
        val squat = newExercise("Sentadilla")
        newSession(oct6, set(bench.id, 1, 50_000, 12), set(bench.id, 2, 52_500, 10))
        val old = newSession(oct8, set(squat.id, 1, 100_000, 5))
        newSession(oct10, set(bench.id, 1, 65_000, 6))
        val logger = newLogger(bench.id, sessionId = old.id)
        assertEquals(old.id, logger.uiState.value.resolvedSessionId)

        val section = newViewModel(bench.id, sessionId = logger.uiState.value.resolvedSessionId)

        assertEquals(proposalOf(logger), firstSetOf(section))
        assertEquals(50_000 to 12, firstSetOf(section))
    }

    // SF03-A05 / SF03-R09: with no previous set, the section is the first time and the logger proposes nothing
    @Test
    fun firstTime_matchesALoggerWithoutProposal() = runTest {
        val bench = newExercise()
        val logger = newLogger(bench.id, sessionId = 0L)

        val section = newViewModel(bench.id, sessionId = logger.uiState.value.resolvedSessionId)

        assertEquals(FirstTime, section.uiState.value)
        assertNull(logger.uiState.value.weight.value)
        assertNull(logger.uiState.value.reps.value)
    }

    // --- Helpers ---

    private fun proposalOf(logger: SetLoggerViewModel): Pair<Int?, Int?> =
        logger.uiState.value.weight.value to logger.uiState.value.reps.value

    private fun firstSetOf(section: LastPerformanceViewModel): Pair<Int, Int> {
        val state = section.uiState.value as Shown
        return state.sets.first().let { it.weightGrams to it.reps }
    }

    /** Builds the ViewModel and starts an empty collector, since `uiState` comes from `stateIn` (S31-R11). */
    private fun TestScope.newViewModel(exerciseId: Long, sessionId: Long?): LastPerformanceViewModel {
        val viewModel = LastPerformanceViewModel(exerciseId, sessionId, sessions, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.newLogger(exerciseId: Long, sessionId: Long): SetLoggerViewModel {
        val viewModel = SetLoggerViewModel(
            exerciseId = exerciseId,
            sessionId = sessionId,
            exercises = exercises,
            sessions = sessions,
            addSet = AddSetUseCase(exercises, sessions, clock),
            correctSet = CorrectSetUseCase(exercises, sessions),
            clock = clock,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun newExercise(name: String = "Press de banca", equipment: Equipment = Equipment.BARBELL): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = equipment, notes = null)
        val result = exercises.create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true)))
        return exercise.copy(id = (result as ExerciseWriteResult.Saved).id)
    }

    private suspend fun archive(exercise: Exercise) {
        exercises.update(exercise.copy(archived = true), listOf(ExerciseMuscle(exercise.id, Muscle.CHEST, isPrimary = true)))
    }

    private fun set(exerciseId: Long, order: Int, weightGrams: Int, reps: Int) =
        ExerciseSet(id = 0, sessionId = 0, exerciseId = exerciseId, order = order, weightGrams = weightGrams, reps = reps)

    private suspend fun newSession(date: LocalDate, vararg sets: ExerciseSet): WorkoutSession {
        val id = sessions.createSession(WorkoutSession(0, date, null), sets.toList())
        return WorkoutSession(id, date, null)
    }
}

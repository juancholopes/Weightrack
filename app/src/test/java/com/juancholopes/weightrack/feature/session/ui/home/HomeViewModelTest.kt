package com.juancholopes.weightrack.feature.session.ui.home

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.CreateSessionUseCase
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import com.juancholopes.weightrack.testing.MainDispatcherRule
import com.juancholopes.weightrack.ui.PendingNotice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    /** Sunday. */
    private val oct11 = LocalDate.of(2026, 10, 11)
    private val oct8 = LocalDate.of(2026, 10, 8)
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneOffset.UTC)

    // --- Today (SF02-R06, SF02-R07) ---

    // SF02-A04 / SF02-R06, S42-R25: before the first data the screen is loading and shows nothing empty
    @Test
    fun uiState_beforeTheFirstData_isLoading_onTheTodayTab() {
        val viewModel = HomeViewModel(exercises, sessions, CreateSessionUseCase(sessions, clock), clock)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertEquals(HomeTab.TODAY, state.selectedTab)
        assertEquals(oct11, state.today)
        assertNull(state.todaySession)
    }

    // SF02-A04 / SF02-R06, S40-A08: the home opens on Hoy; without a session of today it does not offer "Empezar otra sesión"
    @Test
    fun uiState_withoutASessionOfToday_opensOnToday_andDoesNotOfferAnotherSession() = runTest {
        newExercise("Press de banca")
        val viewModel = newHome()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(HomeTab.TODAY, state.selectedTab)
        assertEquals(oct11, state.today)
        assertNull(state.todaySession)
        assertEquals(emptyList<ExerciseCardUi>(), state.todayCards)
        assertFalse(state.canStartAnotherSession)

        viewModel.onStartAnotherSessionClick()

        assertEquals(emptyList<WorkoutSession>(), history())
    }

    // SF02-A04 / SF02-R07, S40-A19, S40-R22: with a session of today it offers it, and tapping it creates a single session that Hoy shows
    @Test
    fun onStartAnotherSessionClick_createsASingleSessionOfToday_thatTodayShows() = runTest {
        val bench = newExercise("Press de banca")
        val first = newSession(oct11, set(bench.id, 1, 60_000))
        val viewModel = newHome()
        assertTrue(viewModel.uiState.value.canStartAnotherSession)
        assertEquals(first.id, viewModel.uiState.value.todaySession?.sessionId)

        viewModel.onStartAnotherSessionClick()

        val created = history().first()
        assertEquals(2, history().size)
        assertEquals(WorkoutSession(created.id, oct11, null), created)
        val state = viewModel.uiState.value
        assertEquals(TodaySessionUi(created.id, oct11, null), state.todaySession)
        assertEquals(emptyList<ExerciseCardUi>(), state.todayCards)
        assertTrue(state.canStartAnotherSession)
        assertFalse(state.genericError)
    }

    // SF02-A04 / S40-R07: two taps while the first write runs create one session only
    @Test
    fun onStartAnotherSessionClick_twiceInARow_createsASingleSession() = runTest {
        newSession(oct11)
        val gate = CompletableDeferred<Unit>()
        val gated = GatedSessions(sessions, gate)
        val viewModel = newHome(sessionRepository = gated, createSession = CreateSessionUseCase(gated, clock))

        viewModel.onStartAnotherSessionClick()
        viewModel.onStartAnotherSessionClick()
        gate.complete(Unit)

        assertEquals(2, history().size)
    }

    // SF02-A04 / SF02-R06, S40-R23: one card per exercise of the session of today, in the order of its first set, with its sets
    @Test
    fun todayCards_listEachExerciseOnce_inTheOrderOfItsFirstSet_withTheNumberOfSetsToday() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        val curl = newExercise("Curl")
        newSession(
            oct11,
            set(squat.id, 1, 80_000),
            set(bench.id, 2, 60_000),
            set(squat.id, 3, 82_500),
            set(curl.id, 5, 20_000),
            set(squat.id, 6, 85_000),
        )

        val state = newHome().uiState.value

        assertEquals(listOf("Sentadilla", "Press de banca", "Curl"), state.todayCards.map { it.name })
        assertEquals(listOf(3, 1, 1), state.todayCards.map { it.setsToday })
        assertEquals(oct11, state.todaySession?.date)
    }

    // SF02-R06, S42-A10: the session of today carries its date and its note
    @Test
    fun todaySession_carriesTheDateAndTheNote() = runTest {
        val id = sessions.createSession(WorkoutSession(0, oct11, "Me sentí con energía"))

        val state = newHome().uiState.value

        assertEquals(TodaySessionUi(id, oct11, "Me sentí con energía"), state.todaySession)
    }

    // SF02-R06, S10-R28: with two sessions of today, the one with the higher id is the session of today
    @Test
    fun todaySession_withTwoSessionsOfToday_isTheOneWithTheHigherId() = runTest {
        val bench = newExercise("Press de banca")
        newSession(oct11, set(bench.id, 1, 60_000))
        val second = newSession(oct11)

        val state = newHome().uiState.value

        assertEquals(second.id, state.todaySession?.sessionId)
        assertEquals(emptyList<ExerciseCardUi>(), state.todayCards)
    }

    // SF02-R06: a session of an earlier day is not the session of today
    @Test
    fun todaySession_isNullWhenTheMostRecentSessionIsOfAnEarlierDay() = runTest {
        val bench = newExercise("Press de banca")
        newSession(oct8, set(bench.id, 1, 60_000))

        val state = newHome().uiState.value

        assertNull(state.todaySession)
        assertFalse(state.canStartAnotherSession)
    }

    // SF02-A04 / SF02-R06, S40-A20, D4: "De tu última sesión" lists the active exercises of the most recent earlier session without sets today
    @Test
    fun lastSessionCards_listTheActiveExercisesOfTheEarlierSessionThatHaveNoSetsToday() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        val curl = newExercise("Curl")
        val row = newExercise("Remo")
        newSession(oct8.minusDays(3), set(row.id, 1, 40_000))
        newSession(oct8, set(squat.id, 1, 80_000), set(bench.id, 2, 60_000), set(curl.id, 3, 20_000), set(squat.id, 4, 82_500))
        exercises.delete(curl.id) // it has sets, so it is archived
        newSession(oct11, set(bench.id, 1, 62_500))

        val state = newHome().uiState.value

        assertEquals(oct8, state.lastSessionDate)
        assertEquals(listOf("Sentadilla"), state.lastSessionCards.map { it.name })
        assertEquals(listOf<Int?>(null), state.lastSessionCards.map { it.setsToday })
        assertEquals(listOf("Press de banca"), state.todayCards.map { it.name })
    }

    // SF02-A04 / SF02-R06: without a session of today, "De tu última sesión" still lists the exercises of the last one
    @Test
    fun lastSessionCards_withoutASessionOfToday_listTheExercisesOfTheLastSession() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        newSession(oct8, set(bench.id, 1, 60_000), set(squat.id, 2, 80_000))

        val state = newHome().uiState.value

        assertNull(state.todaySession)
        assertEquals(oct8, state.lastSessionDate)
        assertEquals(listOf("Press de banca", "Sentadilla"), state.lastSessionCards.map { it.name })
    }

    // SF02-R06: a second session of today is not "the last session": only earlier days count
    @Test
    fun lastSessionCards_ignoreOtherSessionsOfToday() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        newSession(oct8, set(squat.id, 1, 80_000))
        newSession(oct11, set(bench.id, 1, 60_000))
        newSession(oct11)

        val state = newHome().uiState.value

        assertEquals(listOf("Sentadilla"), state.lastSessionCards.map { it.name })
        assertEquals(emptyList<ExerciseCardUi>(), state.todayCards)
    }

    // SF02-R06: with no earlier session there is no "última sesión"
    @Test
    fun lastSessionDate_isNullWithoutEarlierSessions() = runTest {
        newExercise("Press de banca")

        val state = newHome().uiState.value

        assertNull(state.lastSessionDate)
        assertEquals(emptyList<ExerciseCardUi>(), state.lastSessionCards)
    }

    // SF02-R09, S42-R11: an archived exercise with sets today keeps its card, marked as archived
    @Test
    fun todayCards_markAnArchivedExercise() = runTest {
        val bench = newExercise("Press de banca")
        newSession(oct11, set(bench.id, 1, 60_000))
        exercises.delete(bench.id) // it has sets, so it is archived

        val card = newHome().uiState.value.todayCards.single()

        assertTrue(card.archived)
        assertEquals(1, card.setsToday)
    }

    // --- Exercises (SF02-R08, SF02-R09) ---

    // SF02-A05 / SF02-R08, S40-A10: the tab lists the active exercises by name, without the archived ones
    @Test
    fun exerciseCards_excludeArchivedExercises_andAreSortedByName() = runTest {
        newExercise("Sentadilla")
        newExercise("curl con mancuerna")
        newExercise("Press de banca")
        val archived = newExercise("Aperturas")
        exercises.update(archived.copy(archived = true), listOf(ExerciseMuscle(archived.id, Muscle.CHEST, true)))

        val state = newHome().uiState.value

        assertEquals(listOf("curl con mancuerna", "Press de banca", "Sentadilla"), state.exerciseCards.map { it.name })
        assertFalse(state.catalogIsEmpty)
    }

    // SF02-A05 / SF02-R08, S10-R33, S10-A23: "elevacion" finds "Elevación lateral", and a blank search shows all
    @Test
    fun onQueryChange_searchesWithoutCaseOrAccents() = runTest {
        newExercise("Elevación lateral")
        newExercise("Press de banca")
        val viewModel = newHome()

        viewModel.onQueryChange("elevacion")
        assertEquals("elevacion", viewModel.uiState.value.query)
        assertEquals(listOf("Elevación lateral"), viewModel.uiState.value.exerciseCards.map { it.name })

        viewModel.onQueryChange("PRESS")
        assertEquals(listOf("Press de banca"), viewModel.uiState.value.exerciseCards.map { it.name })

        viewModel.onQueryChange("   ")
        assertEquals(2, viewModel.uiState.value.exerciseCards.size)

        viewModel.onQueryChange("zzz")
        assertEquals(emptyList<ExerciseCardUi>(), viewModel.uiState.value.exerciseCards)
        assertFalse(viewModel.uiState.value.catalogIsEmpty)

        viewModel.onClearSearch()
        assertEquals("", viewModel.uiState.value.query)
        assertEquals(2, viewModel.uiState.value.exerciseCards.size)
    }

    // SF02-A05 / SF02-R08, S40-R13: the equipment and muscle filters combine with each other and with the search
    @Test
    fun filters_combineWithEachOtherAndWithTheSearch() = runTest {
        newExercise("Press de banca", Equipment.BARBELL, Muscle.CHEST)
        newExercise("Press con mancuernas", Equipment.DUMBBELL, Muscle.CHEST)
        newExercise("Curl con mancuernas", Equipment.DUMBBELL, Muscle.BICEPS)
        newExercise("Sentadilla", Equipment.BARBELL, Muscle.QUADRICEPS)
        val viewModel = newHome()

        viewModel.onEquipmentFilterSelect(Equipment.DUMBBELL)
        assertEquals(Equipment.DUMBBELL, viewModel.uiState.value.equipmentFilter)
        assertEquals(listOf("Curl con mancuernas", "Press con mancuernas"), viewModel.uiState.value.exerciseCards.map { it.name })

        viewModel.onMuscleFilterSelect(Muscle.CHEST)
        assertEquals(Muscle.CHEST, viewModel.uiState.value.muscleFilter)
        assertEquals(listOf("Press con mancuernas"), viewModel.uiState.value.exerciseCards.map { it.name })

        viewModel.onQueryChange("curl")
        assertEquals(emptyList<ExerciseCardUi>(), viewModel.uiState.value.exerciseCards)

        viewModel.onQueryChange("press")
        viewModel.onEquipmentFilterSelect(null)
        assertEquals(listOf("Press con mancuernas", "Press de banca").sorted(), viewModel.uiState.value.exerciseCards.map { it.name }.sorted())

        viewModel.onMuscleFilterSelect(null)
        viewModel.onQueryChange("")
        assertEquals(4, viewModel.uiState.value.exerciseCards.size)
    }

    // SF02-A05 / SF02-R09, D11: each card carries the heaviest weight and the date of the latest session, or nothing without sets
    @Test
    fun exerciseCards_carryTheHeaviestWeightAndTheDateOfTheLatestSession_orNothingWithoutSets() = runTest {
        val bench = newExercise("Press de banca")
        val squat = newExercise("Sentadilla")
        newSession(oct8.minusDays(3), set(bench.id, 1, 100_000))
        newSession(oct8, set(bench.id, 1, 60_000), set(bench.id, 2, 62_500), set(bench.id, 3, 60_000))
        newSession(oct11, set(bench.id, 1, 57_500))

        val cards = newHome().uiState.value.exerciseCards.associateBy { it.name }

        assertEquals(57_500, cards.getValue("Press de banca").peakWeightGrams)
        assertEquals(oct11, cards.getValue("Press de banca").peakDate)
        assertNull(cards.getValue("Sentadilla").peakWeightGrams)
        assertNull(cards.getValue("Sentadilla").peakDate)
        assertNull(cards.getValue("Press de banca").setsToday)
    }

    // SF02-A05 / SF02-R09, D11: the peak of the latest session of that exercise, not of today, shows on the card of another day
    @Test
    fun exerciseCards_useThePeakOfTheLatestSessionOfThatExercise() = runTest {
        val bench = newExercise("Press de banca")
        newSession(oct8, set(bench.id, 1, 60_000), set(bench.id, 2, 62_500))

        val card = newHome().uiState.value.exerciseCards.single()

        assertEquals(62_500, card.peakWeightGrams)
        assertEquals(oct8, card.peakDate)
    }

    // SF02-A05 / SF02-R09, D2: the subtitle uses the first primary muscle in the order of the `Muscle` enum
    @Test
    fun exerciseCards_showTheFirstPrimaryMuscleInTheOrderOfTheEnum() = runTest {
        newExercise(
            "Prensa",
            Equipment.MACHINE,
            ExerciseMuscle(0, Muscle.GLUTES, isPrimary = true),
            ExerciseMuscle(0, Muscle.QUADRICEPS, isPrimary = true),
            ExerciseMuscle(0, Muscle.CHEST, isPrimary = false),
        )
        newExercise("Press de banca", Equipment.BARBELL, Muscle.CHEST)

        val cards = newHome().uiState.value.exerciseCards.associateBy { it.name }

        assertEquals(Muscle.QUADRICEPS, cards.getValue("Prensa").primaryMuscle)
        assertEquals(Equipment.MACHINE, cards.getValue("Prensa").equipment)
        assertEquals(Muscle.CHEST, cards.getValue("Press de banca").primaryMuscle)
    }

    // SF02-R08, S40-R12, S42-R25: an empty catalog is told apart from a search or filter without results
    @Test
    fun catalogIsEmpty_isTrueOnlyWithoutActiveExercises_whateverTheFilters() = runTest {
        val viewModel = newHome()
        assertTrue(viewModel.uiState.value.catalogIsEmpty)
        assertFalse(viewModel.uiState.value.isLoading)

        newExercise("Press de banca", Equipment.BARBELL, Muscle.CHEST)
        assertFalse(viewModel.uiState.value.catalogIsEmpty)

        viewModel.onMuscleFilterSelect(Muscle.BICEPS)
        assertEquals(emptyList<ExerciseCardUi>(), viewModel.uiState.value.exerciseCards)
        assertFalse(viewModel.uiState.value.catalogIsEmpty)
    }

    // SF02-R10, S42-R03: the tab is a state of the screen and Hoy is the default
    @Test
    fun onTabSelect_changesTheSelectedTab() = runTest {
        val viewModel = newHome()

        viewModel.onTabSelect(HomeTab.EXERCISES)
        assertEquals(HomeTab.EXERCISES, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelect(HomeTab.TODAY)
        assertEquals(HomeTab.TODAY, viewModel.uiState.value.selectedTab)
    }

    // --- Notices (SF02-R11) ---

    // SF02-A06 / SF02-R11, S42-A07: a notice that arrives from the shell is shown, and "Deshacer" returns the exercise to the catalog
    @Test
    fun onNotice_archived_showsTheNotice_andUndoReturnsTheExerciseToTheActiveCatalog() = runTest {
        val bench = newExercise("Press de banca")
        newSession(oct8, set(bench.id, 1, 60_000))
        exercises.delete(bench.id) // it has sets, so it is archived
        val viewModel = newHome()
        assertEquals(emptyList<ExerciseCardUi>(), viewModel.uiState.value.exerciseCards)

        viewModel.onNotice(PendingNotice.Archived(bench.id, "Press de banca", wasDeleteRequest = false))

        assertEquals(HomeNotice.Archived(bench.id, "Press de banca", wasDeleteRequest = false), viewModel.uiState.value.notice)

        viewModel.onUndoArchiveClick(bench.id)

        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
        assertEquals(listOf("Press de banca"), viewModel.uiState.value.exerciseCards.map { it.name })
        assertFalse(viewModel.uiState.value.genericError)
    }

    // SF02-A06 / SF02-R11: archiving after a delete request says it has sets, and a deleted exercise has no undo
    @Test
    fun onNotice_keepsTheKindOfNotice() = runTest {
        val viewModel = newHome()

        viewModel.onNotice(PendingNotice.Archived(7, "Curl", wasDeleteRequest = true))
        assertEquals(HomeNotice.Archived(7, "Curl", wasDeleteRequest = true), viewModel.uiState.value.notice)

        viewModel.onNotice(PendingNotice.Deleted("Remo con mancuerna"))
        assertEquals(HomeNotice.Deleted("Remo con mancuerna"), viewModel.uiState.value.notice)
    }

    // SF02-A06 / SF02-R11, S30-R28: the notice is an effect that is cleared once handled
    @Test
    fun onNoticeHandled_clearsTheNotice() = runTest {
        val viewModel = newHome()
        viewModel.onNotice(PendingNotice.Deleted("Curl"))

        viewModel.onNoticeHandled()

        assertNull(viewModel.uiState.value.notice)
    }

    // --- Failures (SF02-R22, S30-R23) ---

    // SF02-R11, S30-R23: undo fails, the generic message shows, and a retry works
    @Test
    fun onUndoArchiveClick_whenTheWriteFails_showsGenericError_andARetryWorks() = runTest {
        val bench = newExercise("Press de banca")
        exercises.update(bench.copy(archived = true), listOf(ExerciseMuscle(bench.id, Muscle.CHEST, true)))
        val viewModel = newHome()
        exercises.failNextWrite()

        viewModel.onUndoArchiveClick(bench.id)

        assertTrue(viewModel.uiState.value.genericError)
        assertEquals(emptyList<Exercise>(), exercises.observeActiveCatalog().first())

        viewModel.onGenericErrorHandled()
        assertFalse(viewModel.uiState.value.genericError)
        viewModel.onUndoArchiveClick(bench.id)

        assertEquals(listOf(bench), exercises.observeActiveCatalog().first())
    }

    // SF02-R07, S30-R23: "Empezar otra sesión" fails, the generic message shows, and a retry creates the session
    @Test
    fun onStartAnotherSessionClick_whenTheWriteFails_showsGenericError_andARetryCreatesTheSession() = runTest {
        newSession(oct11)
        val viewModel = newHome()
        sessions.failNextWrite()

        viewModel.onStartAnotherSessionClick()

        assertTrue(viewModel.uiState.value.genericError)
        assertEquals(1, history().size)

        viewModel.onGenericErrorHandled()
        viewModel.onStartAnotherSessionClick()

        assertEquals(2, history().size)
        assertFalse(viewModel.uiState.value.genericError)
    }

    // --- Helpers ---

    /** Builds the ViewModel and starts an empty collector, since `uiState` comes from `stateIn` (S31-R11). */
    private fun TestScope.newHome(
        sessionRepository: SessionRepository = sessions,
        createSession: CreateSessionUseCase = CreateSessionUseCase(sessions, clock),
    ): HomeViewModel {
        val viewModel = HomeViewModel(exercises, sessionRepository, createSession, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun newExercise(
        name: String,
        equipment: Equipment = Equipment.BARBELL,
        muscle: Muscle = Muscle.CHEST,
    ): Exercise = newExercise(name, equipment, ExerciseMuscle(0, muscle, isPrimary = true))

    private suspend fun newExercise(name: String, equipment: Equipment, vararg muscles: ExerciseMuscle): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = equipment, notes = null)
        val result = exercises.create(exercise, muscles.toList())
        return exercise.copy(id = (result as ExerciseWriteResult.Saved).id)
    }

    private fun set(exerciseId: Long, order: Int, weightGrams: Int, reps: Int = 10) =
        ExerciseSet(id = 0, sessionId = 0, exerciseId = exerciseId, order = order, weightGrams = weightGrams, reps = reps)

    private suspend fun newSession(date: LocalDate, vararg sets: ExerciseSet): WorkoutSession {
        val id = sessions.createSession(WorkoutSession(0, date, null), sets.toList())
        return WorkoutSession(id, date, null)
    }

    private suspend fun history() = sessions.observeHistory().first()

    /** Holds `createSession` until [gate] opens, to tap twice while the first write is still running. */
    private class GatedSessions(
        private val inner: SessionRepository,
        private val gate: CompletableDeferred<Unit>,
    ) : SessionRepository by inner {
        override suspend fun createSession(session: WorkoutSession, sets: List<ExerciseSet>): Long {
            gate.await()
            return inner.createSession(session, sets)
        }
    }
}

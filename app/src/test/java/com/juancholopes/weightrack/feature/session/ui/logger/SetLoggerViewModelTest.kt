package com.juancholopes.weightrack.feature.session.ui.logger

import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseWriteResult
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.domain.AddSetUseCase
import com.juancholopes.weightrack.feature.session.domain.CorrectSetUseCase
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.testing.FakeSessionRepository
import com.juancholopes.weightrack.testing.InMemoryStore
import com.juancholopes.weightrack.testing.MainDispatcherRule
import com.juancholopes.weightrack.ui.theme.formatKilograms
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SetLoggerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = InMemoryStore()
    private val exercises = FakeExerciseRepository(store)
    private val sessions = FakeSessionRepository(store)

    private val oct11 = LocalDate.of(2026, 10, 11)
    private val oct8 = LocalDate.of(2026, 10, 8)
    private val clock = clockAt("2026-10-11T15:00:00Z")
    private val addSet = AddSetUseCase(exercises, sessions, clock)
    private val correctSet = CorrectSetUseCase(exercises, sessions)

    private fun clockAt(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

    // --- Session (SF02-R12) ---

    // SF02-R12, S42-R25: nothing is read as data before the first emission, so the screen shows no content yet
    @Test
    fun uiState_beforeTheFirstData_isLoading() {
        val viewModel = SetLoggerViewModel(1L, 0L, exercises, sessions, addSet, correctSet, clock)

        val state = viewModel.uiState.value

        assertTrue(state.isLoading)
        assertNull(state.exercise)
        assertEquals(oct11, state.sessionDate)
    }

    // SF02-A07 / SF02-R12, S42-A08: opening the logger without a session of today writes nothing
    @Test
    fun uiState_withoutASessionOfToday_hasNoResolvedSession_andNothingIsCreated() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))

        val viewModel = newLogger(bench.id)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.resolvedSessionId)
        assertEquals(emptyList<SetRowUi>(), state.todaySets)
        assertEquals(oct11, state.sessionDate)
        assertEquals(oct11, state.today)
        assertEquals(1, history().size)
    }

    // SF02-A07 / SF02-R05, S40-R07: two taps in a row create a single session with a single set
    @Test
    fun onSaveClick_twiceInARow_createsASingleSessionWithASingleSet() = runTest {
        val bench = newExercise()
        val gate = CompletableDeferred<Unit>()
        val gated = GatedSessions(sessions, gate)
        val viewModel = newLogger(bench.id, addSetUseCase = AddSetUseCase(exercises, gated, clock), sessionRepository = gated)
        viewModel.onWeightClick()
        viewModel.onTypedValueConfirm("60")
        viewModel.onRepsClick()
        viewModel.onTypedValueConfirm("10")

        viewModel.onSaveClick()
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.onSaveClick()
        gate.complete(Unit)

        val session = history().single()
        assertEquals(1, sessions.setsOfSession(session.id).size)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(session.id, viewModel.uiState.value.resolvedSessionId)
    }

    // SF02-A07 / SF02-R05, S40-A18: the session takes the date of the clock and the set is in it
    @Test
    fun onSaveClick_withoutASessionOfToday_createsOneSessionWithTheDateOfTheClockAndTheSet() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)
        fix(viewModel, weightGrams = 62_500, reps = 8)

        viewModel.onSaveClick()

        val session = history().single()
        assertEquals(WorkoutSession(session.id, oct11, null), session)
        val stored = sessions.setsOfSession(session.id).single()
        assertEquals(ExerciseSet(stored.id, session.id, bench.id, order = 1, weightGrams = 62_500, reps = 8), stored)
        assertEquals(session.id, viewModel.uiState.value.resolvedSessionId)
        assertEquals(listOf(SetRowUi(stored.id, number = 1, weightGrams = 62_500, reps = 8)), viewModel.uiState.value.todaySets)
    }

    // SF02-A07 / SF02-R12, S40-A18: with two sessions of today, the set goes to the one with the higher id
    @Test
    fun onSaveClick_withTwoSessionsOfToday_addsTheSetToTheOneWithTheHigherId() = runTest {
        val bench = newExercise()
        val first = newSession(oct11)
        val second = newSession(oct11)
        val viewModel = newLogger(bench.id)
        assertEquals(second.id, viewModel.uiState.value.resolvedSessionId)
        fix(viewModel, weightGrams = 60_000, reps = 10)

        viewModel.onSaveClick()

        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(first.id))
        assertEquals(1, sessions.setsOfSession(second.id).size)
        assertEquals(2, history().size)
    }

    // SF02-A07 / SF02-R12, S42-A08: the use case clock is already on the next day, and the set goes to the opening date
    @Test
    fun onSaveClick_afterMidnight_createsTheSessionWithTheOpeningDate_andLaterSetsGoToIt() = runTest {
        val bench = newExercise()
        val afterMidnight = AddSetUseCase(exercises, sessions, clockAt("2026-10-12T00:10:00Z"))
        val viewModel = newLogger(bench.id, addSetUseCase = afterMidnight)
        fix(viewModel, weightGrams = 60_000, reps = 10)

        viewModel.onSaveClick()
        viewModel.onSaveClick()

        val session = history().single()
        assertEquals(oct11, session.date)
        assertEquals(2, sessions.setsOfSession(session.id).size)
    }

    // SF02-A07 / S40-A30: a new ViewModel over the same fakes shows the saved sets
    @Test
    fun newViewModel_overTheSameFakes_showsTheSavedSets() = runTest {
        val bench = newExercise()
        val first = newLogger(bench.id)
        fix(first, weightGrams = 60_000, reps = 10)
        first.onSaveClick()
        fix(first, weightGrams = 62_500, reps = 8)
        first.onSaveClick()

        val reopened = newLogger(bench.id)

        val rows = reopened.uiState.value.todaySets
        assertEquals(listOf(1, 2), rows.map { it.number })
        assertEquals(listOf(60_000 to 10, 62_500 to 8), rows.map { it.weightGrams to it.reps })
        assertEquals(first.uiState.value.resolvedSessionId, reopened.uiState.value.resolvedSessionId)
    }

    // SF02-R12, S42-R11: with another `sessionId` the logger uses that session, shows its date and never creates one
    @Test
    fun uiState_withASessionId_usesThatSession_andNeverCreatesAnother() = runTest {
        val bench = newExercise()
        val past = newSession(oct8, set(bench.id, 1, 55_000, 10))
        val viewModel = newLogger(bench.id, sessionId = past.id)

        val state = viewModel.uiState.value
        assertEquals(past.id, state.resolvedSessionId)
        assertEquals(oct8, state.sessionDate)
        assertEquals(oct11, state.today)
        assertEquals(listOf(55_000), state.todaySets.map { it.weightGrams })

        viewModel.onSaveClick()

        assertEquals(listOf(past), history())
        assertEquals(2, sessions.setsOfSession(past.id).size)
    }

    // --- Proposed values (SF02-R14) ---

    // SF02-A08 / SF02-R14, S40-A23: the first set proposes the first set of the last performance
    @Test
    fun uiState_firstSet_proposesTheFirstSetOfTheLastPerformance_withoutUnsavedChanges() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 2, 62_500, 8), set(bench.id, 1, 60_000, 10))
        newSession(oct8.minusDays(3), set(bench.id, 1, 50_000, 12))

        val viewModel = newLogger(bench.id)

        val state = viewModel.uiState.value
        assertEquals(60_000, state.weight.value)
        assertEquals(10, state.reps.value)
        assertEquals(weightWindow(60_000), state.weight.window)
        assertEquals(repsWindow(10), state.reps.window)
        assertFalse(state.hasUnsavedChanges)
    }

    // SF02-A08 / SF02-R14, S40-R27: after saving, the panel keeps the values and the next set proposes the saved one
    @Test
    fun onSaveClick_keepsTheValues_andTheNextSetProposesTheOneJustSaved() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        viewModel.onWeightIncrease()
        viewModel.onWeightIncrease()
        viewModel.onRepsDecrease()
        assertEquals(60_500, viewModel.uiState.value.weight.value)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals(60_500, state.weight.value)
        assertEquals(9, state.reps.value)
        assertFalse(state.hasUnsavedChanges)
        val reopened = newLogger(bench.id)
        assertEquals(60_500, reopened.uiState.value.weight.value)
        assertEquals(9, reopened.uiState.value.reps.value)
    }

    // SF02-A08 / SF02-R14, S10-R02, S40-A23: bodyweight without a proposal starts with 0 kg of additional weight
    @Test
    fun uiState_bodyweightWithoutAProposal_startsWithZeroAdditionalWeight_andNoRepetitions() = runTest {
        val pullUp = newExercise("Dominadas", Equipment.BODYWEIGHT)

        val viewModel = newLogger(pullUp.id)

        val state = viewModel.uiState.value
        assertEquals(0, state.weight.value)
        assertTrue(state.weight.isFixed)
        assertNull(state.reps.value)
        assertFalse(state.reps.isFixed)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.weight.canDecrease)

        viewModel.onSaveClick()
        assertEquals(LoggerFieldError.RepsMissing, viewModel.uiState.value.fieldError)
        fix(viewModel, reps = 12)
        viewModel.onSaveClick()

        assertEquals(listOf(0 to 12), sessions.setsOfSession(history().single().id).map { it.weightGrams to it.reps })
    }

    // SF02-A08 / SF02-R14, S40-A23: without a proposal in another equipment, saving asks for the weight and writes nothing
    @Test
    fun onSaveClick_withoutAProposal_asksToFixTheWeight_andWritesNothing() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)
        assertNull(viewModel.uiState.value.weight.value)
        assertNull(viewModel.uiState.value.reps.value)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onSaveClick()

        assertEquals(LoggerFieldError.WeightMissing, viewModel.uiState.value.fieldError)
        assertEquals(emptyList<WorkoutSession>(), history())

        viewModel.onWeightIncrease()
        assertNull(viewModel.uiState.value.fieldError)
        viewModel.onSaveClick()
        assertEquals(LoggerFieldError.RepsMissing, viewModel.uiState.value.fieldError)
        assertEquals(emptyList<WorkoutSession>(), history())

        viewModel.onRepsIncrease()
        viewModel.onSaveClick()

        assertNull(viewModel.uiState.value.fieldError)
        assertEquals(1, sessions.setsOfSession(history().single().id).size)
    }

    // SF02-R14, S40-R40: proposed values count as the initial ones; changing and restoring them leaves no changes
    @Test
    fun hasUnsavedChanges_isTrueOnlyWhileTheValuesDifferFromTheProposedOnes() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onWeightIncrease()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        viewModel.onWeightDecrease()
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    // --- Control (SF02-R15) ---

    // SF02-A09 / SF02-R15, S42-A15: "62,3" and "1000,25" show the error and do not change the value
    @Test
    fun onTypedValueConfirm_withAnInvalidWeight_keepsTheDialogOpenWithTheError_andTheValue() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        viewModel.onWeightClick()
        assertEquals(TypingDialog(ValueField.WEIGHT, initialText = "60"), viewModel.uiState.value.typingDialog)

        viewModel.onTypedValueConfirm("62,3")
        assertEquals(
            TypedValueError.Rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP),
            viewModel.uiState.value.typingDialog?.error,
        )
        assertEquals(60_000, viewModel.uiState.value.weight.value)

        viewModel.onTypedValueConfirm("1000,25")
        assertEquals(
            TypedValueError.Rejected(ExerciseSetError.WEIGHT_TOO_HIGH),
            viewModel.uiState.value.typingDialog?.error,
        )

        viewModel.onTypedValueConfirm("hola")
        assertEquals(TypedValueError.NotANumber, viewModel.uiState.value.typingDialog?.error)
        assertEquals(60_000, viewModel.uiState.value.weight.value)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onTypedTextChange()
        assertNull(viewModel.uiState.value.typingDialog?.error)
        assertEquals(ValueField.WEIGHT, viewModel.uiState.value.typingDialog?.field)
    }

    // SF02-A09 / SF02-R15, S42-A15: "62,5" and "62.5" set 62 500 g, close the dialog and recenter the window
    @Test
    fun onTypedValueConfirm_withAValidWeight_setsTheValue_closesTheDialog_andRecentersTheWindow() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)

        viewModel.onWeightClick()
        assertEquals("", viewModel.uiState.value.typingDialog?.initialText)
        viewModel.onTypedValueConfirm("62,5")

        assertEquals(62_500, viewModel.uiState.value.weight.value)
        assertNull(viewModel.uiState.value.typingDialog)
        assertEquals(weightWindow(62_500), viewModel.uiState.value.weight.window)

        viewModel.onWeightClick()
        viewModel.onTypedValueConfirm("70.25")
        assertEquals(70_250, viewModel.uiState.value.weight.value)
        assertEquals(weightWindow(70_250), viewModel.uiState.value.weight.window)
    }

    // SF02-A09 / SF02-R15: the repetitions dialog follows the same rules, from 1 to 999
    @Test
    fun onTypedValueConfirm_forTheRepetitions_rejects0And1000_andAcceptsAWholeNumber() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)

        viewModel.onRepsClick()
        viewModel.onTypedValueConfirm("0")
        assertEquals(TypedValueError.Rejected(ExerciseSetError.REPS_NOT_POSITIVE), viewModel.uiState.value.typingDialog?.error)
        viewModel.onTypedValueConfirm("1000")
        assertEquals(TypedValueError.Rejected(ExerciseSetError.REPS_TOO_HIGH), viewModel.uiState.value.typingDialog?.error)
        assertNull(viewModel.uiState.value.reps.value)

        viewModel.onTypedValueConfirm("12")

        assertEquals(12, viewModel.uiState.value.reps.value)
        assertNull(viewModel.uiState.value.typingDialog)
    }

    // SF02-R15, S40-R41: closing the dialog does nothing to the value
    @Test
    fun onTypingDismiss_closesTheDialog_andKeepsTheValue() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        viewModel.onRepsClick()
        assertEquals("10", viewModel.uiState.value.typingDialog?.initialText)

        viewModel.onTypingDismiss()

        assertNull(viewModel.uiState.value.typingDialog)
        assertEquals(10, viewModel.uiState.value.reps.value)
    }

    // SF02-A09 / SF02-R15, S42-R19: at 0 kg, decreasing is disabled and does nothing
    @Test
    fun onWeightDecrease_atZeroKg_isDisabled_andDoesNothing() = runTest {
        val pullUp = newExercise("Dominadas", Equipment.BODYWEIGHT)
        val viewModel = newLogger(pullUp.id)
        assertFalse(viewModel.uiState.value.weight.canDecrease)
        assertTrue(viewModel.uiState.value.weight.canIncrease)

        viewModel.onWeightDecrease()

        assertEquals(0, viewModel.uiState.value.weight.value)
        viewModel.onWeightIncrease()
        assertEquals(250, viewModel.uiState.value.weight.value)
        assertTrue(viewModel.uiState.value.weight.canDecrease)
    }

    // SF02-R15, S42-R19: the control never leaves 0 to 1000 kg nor 1 to 999 repetitions
    @Test
    fun theButtons_neverLeaveTheLimits() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 1_000_000, 999))
        val viewModel = newLogger(bench.id)
        assertFalse(viewModel.uiState.value.weight.canIncrease)
        assertFalse(viewModel.uiState.value.reps.canIncrease)

        viewModel.onWeightIncrease()
        viewModel.onRepsIncrease()

        assertEquals(1_000_000, viewModel.uiState.value.weight.value)
        assertEquals(999, viewModel.uiState.value.reps.value)
        assertEquals(weightWindow(1_000_000), viewModel.uiState.value.weight.window)

        viewModel.onRepsClick()
        viewModel.onTypedValueConfirm("1")
        assertFalse(viewModel.uiState.value.reps.canDecrease)
        viewModel.onRepsDecrease()
        assertEquals(1, viewModel.uiState.value.reps.value)
    }

    // SF02-A10 / SF02-R15, S42-A14: "+" gives 60,25 kg and the window, still centered on its stop, does not move
    @Test
    fun onWeightIncrease_adds250Grams_andRecentersTheWindowOnTheNewValue() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        assertEquals(ValueWindow(50_000, 70_000, 1_250), viewModel.uiState.value.weight.window)

        viewModel.onWeightIncrease()
        assertEquals(60_250, viewModel.uiState.value.weight.value)
        assertEquals(ValueWindow(50_000, 70_000, 1_250), viewModel.uiState.value.weight.window)

        repeat(4) { viewModel.onWeightIncrease() }
        assertEquals(61_250, viewModel.uiState.value.weight.value)
        assertEquals(ValueWindow(51_250, 71_250, 1_250), viewModel.uiState.value.weight.window)
    }

    // SF02-A10 / SF02-R15, S42-A14: while dragging the window does not move; on release it recenters on the new value
    @Test
    fun onWeightSlide_snapsToTheStops_andTheWindowOnlyMovesOnRelease() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)

        viewModel.onWeightSlide(69_000)
        assertEquals(68_750, viewModel.uiState.value.weight.value)
        viewModel.onWeightSlide(70_000)

        assertEquals(70_000, viewModel.uiState.value.weight.value)
        assertEquals(ValueWindow(50_000, 70_000, 1_250), viewModel.uiState.value.weight.window)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onWeightSlideFinished()

        assertEquals(70_000, viewModel.uiState.value.weight.value)
        assertEquals(ValueWindow(60_000, 80_000, 1_250), viewModel.uiState.value.weight.window)
    }

    // SF02-A10 / SF02-R15: the repetitions slider has stops of 1 and recenters on release
    @Test
    fun onRepsSlide_givesWholeRepetitions_andTheWindowMovesOnRelease() = runTest {
        val bench = newExercise()
        newSession(oct8, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)

        viewModel.onRepsSlide(18)
        assertEquals(18, viewModel.uiState.value.reps.value)
        assertEquals(ValueWindow(1, 20, 1), viewModel.uiState.value.reps.window)
        viewModel.onRepsSlideFinished()

        assertEquals(ValueWindow(8, 28, 1), viewModel.uiState.value.reps.window)
        assertEquals(18, viewModel.uiState.value.reps.value)
    }

    // SF02-R15, Spec 42 "Sin valor propuesto": the window is centered on 20 kg and "+" starts from its center
    @Test
    fun withoutAProposal_theWindowIsCenteredOn20Kg_andThePlusButtonStartsFromItsCenter() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)

        assertEquals(weightWindow(20_000), viewModel.uiState.value.weight.window)
        assertEquals(repsWindow(10), viewModel.uiState.value.reps.window)

        viewModel.onWeightIncrease()
        viewModel.onRepsDecrease()

        assertEquals(20_250, viewModel.uiState.value.weight.value)
        assertEquals(9, viewModel.uiState.value.reps.value)
    }

    // --- Saved set announcement (SF02-R13, S40-R27) ---

    // SF02-R13, S40-R27: a saved set is announced with its number, once
    @Test
    fun onSaveClick_announcesTheSavedSetWithItsNumber_untilItIsHandled() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)
        fix(viewModel, weightGrams = 60_000, reps = 10)
        viewModel.onSaveClick()
        viewModel.onSaveClick()
        viewModel.onSavedAnnouncementHandled()
        fix(viewModel, weightGrams = 62_500, reps = 8)

        viewModel.onSaveClick()

        assertEquals(SavedSet(number = 3, weightGrams = 62_500, reps = 8), viewModel.uiState.value.savedAnnouncement)
        viewModel.onSavedAnnouncementHandled()
        assertNull(viewModel.uiState.value.savedAnnouncement)
    }

    // --- Correct, delete and archived (SF02-R16) ---

    // SF02-A11 / SF02-R13, S40-A20: orders 1, 3 and 4 are numbered 1, 2 and 3, and only this exercise's sets are listed
    @Test
    fun todaySets_areNumberedByPosition_notByOrder_andOnlyThisExercisesSetsAreListed() = runTest {
        val bench = newExercise()
        val squat = newExercise("Sentadilla")
        newSession(
            oct11,
            set(bench.id, 1, 60_000, 10),
            set(squat.id, 2, 80_000, 8),
            set(bench.id, 3, 62_500, 8),
            set(bench.id, 4, 62_500, 6),
        )

        val viewModel = newLogger(bench.id)

        val rows = viewModel.uiState.value.todaySets
        assertEquals(listOf(1, 2, 3), rows.map { it.number })
        assertEquals(listOf(60_000, 62_500, 62_500), rows.map { it.weightGrams })
    }

    // SF02-A11 / SF02-R16, S42-R17: tapping a set puts the panel in correction mode with its values
    @Test
    fun onSetClick_putsThePanelInCorrection_withTheValuesOfTheSet() = runTest {
        val bench = newExercise()
        newSession(oct11, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 62_500, 8))
        val viewModel = newLogger(bench.id)
        val second = viewModel.uiState.value.todaySets[1]

        viewModel.onSetClick(second.setId)

        val state = viewModel.uiState.value
        assertEquals(second.setId, state.editingSetId)
        assertEquals(62_500, state.weight.value)
        assertEquals(8, state.reps.value)
        assertEquals(weightWindow(62_500), state.weight.window)
        assertFalse(state.hasUnsavedChanges)
    }

    // SF02-A11 / SF02-R16, S40-A26: saving the correction changes the set in place and returns to a new set
    @Test
    fun onSaveClick_inCorrection_changesTheSetInPlace_andReturnsToANewSet() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 62_500, 8))
        val viewModel = newLogger(bench.id)
        val before = sessions.setsOfSession(session.id)
        viewModel.onSetClick(before[1].id)
        viewModel.onWeightClick()
        viewModel.onTypedValueConfirm("65")
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onSaveClick()

        val after = sessions.setsOfSession(session.id)
        assertEquals(before[1].copy(weightGrams = 65_000), after[1])
        assertEquals(before[0], after[0])
        val state = viewModel.uiState.value
        assertNull(state.editingSetId)
        assertEquals(65_000, state.weight.value)
        assertEquals(8, state.reps.value)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(listOf(60_000, 65_000), state.todaySets.map { it.weightGrams })
        assertNull(state.savedAnnouncement)
    }

    // SF02-A11 / SF02-R16: "Cancelar corrección" returns to a new set with the proposed values and writes nothing
    @Test
    fun onCancelCorrectionClick_returnsToANewSetWithTheProposedValues() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 62_500, 8))
        val viewModel = newLogger(bench.id)
        val before = sessions.setsOfSession(session.id)
        viewModel.onSetClick(before[0].id)
        viewModel.onWeightIncrease()

        viewModel.onCancelCorrectionClick()

        val state = viewModel.uiState.value
        assertNull(state.editingSetId)
        assertEquals(62_500, state.weight.value)
        assertEquals(8, state.reps.value)
        assertFalse(state.hasUnsavedChanges)
        assertEquals(before, sessions.setsOfSession(session.id))
    }

    // SF02-A11 / SF02-R16, S40-A26: deleting asks to confirm, and the session stays even without sets
    @Test
    fun onDeleteSetConfirm_deletesTheSet_andTheSessionStaysEvenWithoutSets() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 62_500, 8))
        val viewModel = newLogger(bench.id)
        val rows = viewModel.uiState.value.todaySets
        viewModel.onSetClick(rows[1].setId)

        viewModel.onDeleteSetClick()

        assertEquals(LoggerDialog.DeleteSet(setNumber = 2), viewModel.uiState.value.confirmDialog)
        assertEquals(2, sessions.setsOfSession(session.id).size)

        viewModel.onDeleteSetConfirm()

        assertNull(viewModel.uiState.value.confirmDialog)
        assertNull(viewModel.uiState.value.editingSetId)
        assertEquals(listOf(60_000), viewModel.uiState.value.todaySets.map { it.weightGrams })

        viewModel.onSetClick(viewModel.uiState.value.todaySets.single().setId)
        viewModel.onDeleteSetClick()
        viewModel.onDeleteSetConfirm()

        assertEquals(emptyList<SetRowUi>(), viewModel.uiState.value.todaySets)
        assertEquals(listOf(session), history())
        assertEquals(session.id, viewModel.uiState.value.resolvedSessionId)
    }

    // SF02-A11 / SF02-R16, S40-R41: dismissing the confirmation deletes nothing
    @Test
    fun onDialogDismiss_afterOnDeleteSetClick_deletesNothing() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        viewModel.onSetClick(viewModel.uiState.value.todaySets.single().setId)
        viewModel.onDeleteSetClick()

        viewModel.onDialogDismiss()

        assertNull(viewModel.uiState.value.confirmDialog)
        assertEquals(1, sessions.setsOfSession(session.id).size)
        assertNotNull(viewModel.uiState.value.editingSetId)
    }

    // SF02-A12 / SF02-R16, S40-A27, S40-R29: an archived exercise offers no new set, but its sets can be corrected and deleted
    @Test
    fun archivedExercise_offersNoNewSet_butItsSetsCanBeCorrectedAndDeleted() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10), set(bench.id, 2, 62_500, 8))
        exercises.delete(bench.id) // it has sets, so it is archived
        val viewModel = newLogger(bench.id)
        val state = viewModel.uiState.value
        assertEquals(true, state.exercise?.archived)
        assertTrue(state.showsArchivedNotice)

        viewModel.onSaveClick()
        assertEquals(2, sessions.setsOfSession(session.id).size)
        assertNull(viewModel.uiState.value.fieldError)

        viewModel.onSetClick(state.todaySets[0].setId)
        assertFalse(viewModel.uiState.value.showsArchivedNotice)
        viewModel.onWeightIncrease()
        viewModel.onSaveClick()
        assertEquals(listOf(60_250, 62_500), sessions.setsOfSession(session.id).map { it.weightGrams })

        viewModel.onSetClick(state.todaySets[1].setId)
        viewModel.onDeleteSetClick()
        viewModel.onDeleteSetConfirm()
        assertEquals(1, sessions.setsOfSession(session.id).size)
        assertTrue(viewModel.uiState.value.showsArchivedNotice)
    }

    // --- Note of the exercise (SF02-A19) ---

    // SF02-A19 / SF02-R13, S40-A29: editing the note of the exercise changes the note in the state
    @Test
    fun uiState_followsTheNoteOfTheExercise() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)
        assertNull(viewModel.uiState.value.exercise?.notes)

        exercises.update(bench.copy(notes = "Asiento en la posición 4"), listOf(ExerciseMuscle(bench.id, Muscle.CHEST, true)))

        val exercise = viewModel.uiState.value.exercise
        assertEquals("Asiento en la posición 4", exercise?.notes)
        assertEquals("Press de banca", exercise?.name)
        assertEquals(Equipment.BARBELL, exercise?.equipment)
    }

    // --- Failures (SF02-A14) ---

    // SF02-A14 / SF02-R22, S40-A38, S30-R23: the first set fails, the values stay, and a second attempt saves
    @Test
    fun onSaveClick_whenCreatingTheSessionFails_showsGenericError_keepsTheValues_andARetrySaves() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)
        fix(viewModel, weightGrams = 62_500, reps = 8)
        sessions.failNextWrite()

        viewModel.onSaveClick()

        val failed = viewModel.uiState.value
        assertTrue(failed.genericError)
        assertFalse(failed.isSaving)
        assertEquals(62_500, failed.weight.value)
        assertEquals(8, failed.reps.value)
        assertTrue(failed.hasUnsavedChanges)
        assertEquals(emptyList<WorkoutSession>(), history())

        viewModel.onGenericErrorHandled()
        assertFalse(viewModel.uiState.value.genericError)
        viewModel.onSaveClick()

        assertFalse(viewModel.uiState.value.genericError)
        assertEquals(1, sessions.setsOfSession(history().single().id).size)
    }

    // SF02-A14 / SF02-R22, S30-R23: the same for a set added to an existing session
    @Test
    fun onSaveClick_whenAddingASetFails_showsGenericError_keepsTheValues_andARetrySaves() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        sessions.failNextWrite()

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.genericError)
        assertEquals(60_000, viewModel.uiState.value.weight.value)
        assertEquals(1, sessions.setsOfSession(session.id).size)

        viewModel.onGenericErrorHandled()
        viewModel.onSaveClick()

        assertEquals(2, sessions.setsOfSession(session.id).size)
        assertFalse(viewModel.uiState.value.genericError)
    }

    // SF02-A14 / SF02-R22, S30-R23: the same for a correction, which stays in correction mode
    @Test
    fun onSaveClick_whenCorrectingFails_showsGenericError_staysInCorrection_andARetrySaves() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        val setId = viewModel.uiState.value.todaySets.single().setId
        viewModel.onSetClick(setId)
        viewModel.onWeightIncrease()
        sessions.failNextWrite()

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.genericError)
        assertEquals(setId, viewModel.uiState.value.editingSetId)
        assertEquals(60_250, viewModel.uiState.value.weight.value)
        assertEquals(60_000, sessions.setsOfSession(session.id).single().weightGrams)

        viewModel.onGenericErrorHandled()
        viewModel.onSaveClick()

        assertEquals(60_250, sessions.setsOfSession(session.id).single().weightGrams)
        assertNull(viewModel.uiState.value.editingSetId)
    }

    // SF02-A14 / SF02-R22, S30-R23: the same for deleting a set
    @Test
    fun onDeleteSetConfirm_whenTheWriteFails_showsGenericError_keepsTheSet_andARetryDeletes() = runTest {
        val bench = newExercise()
        val session = newSession(oct11, set(bench.id, 1, 60_000, 10))
        val viewModel = newLogger(bench.id)
        viewModel.onSetClick(viewModel.uiState.value.todaySets.single().setId)
        viewModel.onDeleteSetClick()
        sessions.failNextWrite()

        viewModel.onDeleteSetConfirm()

        assertTrue(viewModel.uiState.value.genericError)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(1, sessions.setsOfSession(session.id).size)
        assertNotNull(viewModel.uiState.value.editingSetId)

        viewModel.onGenericErrorHandled()
        viewModel.onDeleteSetClick()
        viewModel.onDeleteSetConfirm()

        assertEquals(emptyList<ExerciseSet>(), sessions.setsOfSession(session.id))
    }

    // SF02-R22, S30-R22: a rejection of the domain is a field error, not the generic one
    @Test
    fun onSaveClick_whenTheDomainRejectsTheSession_showsTheFieldError_andWritesNothing() = runTest {
        val bench = newExercise()
        val yesterdayClock = AddSetUseCase(exercises, sessions, clockAt("2026-10-10T15:00:00Z"))
        val viewModel = newLogger(bench.id, addSetUseCase = yesterdayClock)
        fix(viewModel, weightGrams = 60_000, reps = 10)

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals(LoggerFieldError.RejectedSession(WorkoutSessionError.DATE_IN_FUTURE), state.fieldError)
        assertFalse(state.genericError)
        assertEquals(emptyList<WorkoutSession>(), history())
        assertEquals(60_000, state.weight.value)
    }

    // --- Discard dialog (SF02-R10, S40-R40) ---

    // SF02-A17 / S40-R40: back with changes asks to discard, and "Seguir editando" closes the dialog
    @Test
    fun onDiscardRequest_opensTheDiscardDialog_andOnDialogDismissClosesIt() = runTest {
        val bench = newExercise()
        val viewModel = newLogger(bench.id)

        viewModel.onDiscardRequest()
        assertEquals(LoggerDialog.Discard, viewModel.uiState.value.confirmDialog)

        viewModel.onDialogDismiss()
        assertNull(viewModel.uiState.value.confirmDialog)
    }

    // --- Helpers ---

    /** Builds the ViewModel and starts an empty collector, since `uiState` comes from `stateIn` (S31-R11). */
    private fun TestScope.newLogger(
        exerciseId: Long,
        sessionId: Long = 0L,
        addSetUseCase: AddSetUseCase = addSet,
        sessionRepository: SessionRepository = sessions,
    ): SetLoggerViewModel {
        val viewModel = SetLoggerViewModel(exerciseId, sessionId, exercises, sessionRepository, addSetUseCase, correctSet, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    /** Fixes the weight and the repetitions the way the user does, by typing them. */
    private fun fix(viewModel: SetLoggerViewModel, weightGrams: Int? = null, reps: Int? = null) {
        if (weightGrams != null) {
            viewModel.onWeightClick()
            viewModel.onTypedValueConfirm(formatKilograms(weightGrams))
        }
        if (reps != null) {
            viewModel.onRepsClick()
            viewModel.onTypedValueConfirm(reps.toString())
        }
    }

    private suspend fun newExercise(name: String = "Press de banca", equipment: Equipment = Equipment.BARBELL): Exercise {
        val exercise = Exercise(id = 0, name = name, equipment = equipment, notes = null)
        val result = exercises.create(exercise, listOf(ExerciseMuscle(0, Muscle.CHEST, isPrimary = true)))
        return exercise.copy(id = (result as ExerciseWriteResult.Saved).id)
    }

    private fun set(exerciseId: Long, order: Int, weightGrams: Int, reps: Int) =
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

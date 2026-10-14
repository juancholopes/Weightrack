package com.juancholopes.weightrack.feature.session.ui.logger

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.session.domain.ExerciseSetError
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests of [SetLoggerContent] with a plain `UiState` and lambdas that record their calls: no ViewModel,
 * repository or database is involved (S31-R14, S31-R18). The host applies the few state changes the ViewModel
 * would make so that flows can be followed.
 */
class SetLoggerContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Every call the screen made, in order, as "event" or "event:argument". */
    private val calls = mutableListOf<String>()

    private var uiState by mutableStateOf(loadedState)

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun show(initial: SetLoggerUiState, fontScale: Float? = null, progress: Boolean = false) {
        uiState = initial
        composeRule.setContent {
            val logger = @Composable {
                WeightrackTheme {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                        onBackCompleted = { calls += "shell-back" },
                    )
                    SetLoggerContent(
                        uiState = uiState,
                        onWeightDecrease = { calls += "weight-decrease" },
                        onWeightIncrease = { calls += "weight-increase" },
                        onWeightSlide = { calls += "weight-slide:$it" },
                        onWeightSlideFinished = { calls += "weight-slide-finished" },
                        onWeightClick = { calls += "weight-click" },
                        onRepsDecrease = { calls += "reps-decrease" },
                        onRepsIncrease = { calls += "reps-increase" },
                        onRepsSlide = { calls += "reps-slide:$it" },
                        onRepsSlideFinished = { calls += "reps-slide-finished" },
                        onRepsClick = { calls += "reps-click" },
                        onTypedTextChange = { calls += "typed-change" },
                        onTypedValueConfirm = {
                            calls += "typed-confirm:$it"
                            uiState = uiState.copy(
                                typingDialog = uiState.typingDialog?.copy(
                                    error = TypedValueError.Rejected(ExerciseSetError.WEIGHT_NOT_MULTIPLE_OF_STEP),
                                ),
                            )
                        },
                        onTypingDismiss = { calls += "typing-dismiss" },
                        onSetClick = { calls += "set-click:$it" },
                        onCancelCorrectionClick = { calls += "cancel-correction" },
                        onSaveClick = { calls += "save" },
                        onDeleteSetClick = { calls += "delete-set" },
                        onDeleteSetConfirm = { calls += "delete-confirm" },
                        onDiscardRequest = { calls += "discard-request" },
                        onDialogDismiss = { calls += "dialog-dismiss" },
                        onSavedAnnouncementHandled = {
                            calls += "announcement-handled"
                            uiState = uiState.copy(savedAnnouncement = null)
                        },
                        onGenericErrorHandled = {
                            calls += "generic-error-handled"
                            uiState = uiState.copy(genericError = false)
                        },
                        onBack = { calls += "back" },
                        onEditExercise = { calls += "edit-exercise:$it" },
                        onOpenProgress = if (progress) { id -> calls += "progress:$id" } else null,
                        lastPerformance = { exerciseId, sessionId, modifier ->
                            Text(text = "last:$exerciseId:$sessionId", modifier = modifier)
                        },
                    )
                }
            }
            if (fontScale == null) {
                logger()
            } else {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) { logger() }
            }
        }
    }

    private fun top(node: androidx.compose.ui.test.SemanticsNodeInteraction) = node.getUnclippedBoundsInRoot().top

    private fun hasClickAction() = SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)

    // SF02-A16 / S42-R15, S42-A12: the order is note, last time, list of today, weight, repetitions and save
    @Test
    fun focusOrder_followsTheSpec() {
        show(loadedState)

        val order = listOf(
            top(composeRule.onNodeWithText("Asiento en la posición 4")),
            top(composeRule.onNodeWithText("last:1:9")),
            top(composeRule.onNodeWithText(text(R.string.logger_sets_title_today))),
            top(composeRule.onNodeWithContentDescription(text(R.string.logger_set_row_description, 1, "60 kilogramos, 10 repeticiones"))),
            top(composeRule.onNodeWithText(text(R.string.logger_weight_label))),
            top(composeRule.onNodeWithText(text(R.string.logger_reps_label))),
            top(composeRule.onNodeWithText(text(R.string.logger_save_set))),
        )

        assertEquals("The screen does not follow the focus order of S42-R15: $order", order.sorted(), order)
    }

    // SF02-A16 / S42-R16: the slot receives the exercise and the resolved session (null before the first set)
    @Test
    fun lastPerformanceSlot_receivesTheResolvedSession() {
        show(loadedState)
        composeRule.onNodeWithText("last:1:9").assertIsDisplayed()

        uiState = uiState.copy(resolvedSessionId = null)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("last:1:null").assertIsDisplayed()
    }

    // SF02-A16 / S40-A03, S40-A25: with proposed values one tap saves, and no text field is on the screen
    @Test
    fun save_withProposedValues_isOneTap_andFocusesNoTextField() {
        show(loadedState)

        composeRule.onNodeWithText(text(R.string.logger_save_set)).performClick()

        assertEquals(listOf("save"), calls)
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }

    // SF02-A16 / S40-A23: a value not fixed shows its hint and reads "Sin fijar"; the message goes next to the panel
    @Test
    fun unsetWeight_showsTheHint_andTheMissingValueMessage() {
        show(
            loadedState.copy(
                weight = ValueInput(null, weightWindow(WEIGHT_DEFAULT_REFERENCE_GRAMS), canDecrease = true, canIncrease = true),
                fieldError = LoggerFieldError.WeightMissing,
            ),
        )

        composeRule.onNodeWithText(text(R.string.logger_weight_unset_hint)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.logger_value_description, text(R.string.logger_weight_label), text(R.string.logger_unset_state)))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_error_weight_missing)).assertIsDisplayed()
    }

    // SF02-A16 / SF02-R13, S40-R27: the new set shows with its number and a live region announces it
    @Test
    fun newSet_appearsWithItsNumber_andIsAnnounced() {
        show(loadedState)

        uiState = uiState.copy(
            todaySets = uiState.todaySets + SetRowUi(setId = 13L, number = 3, weightGrams = 60_000, reps = 10),
            savedAnnouncement = SavedSet(number = 3, weightGrams = 60_000, reps = 10),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(text(R.string.logger_set_row_description, 3, "60 kilogramos, 10 repeticiones"))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.logger_saved_announcement, 3, "60 kilogramos, 10 repeticiones")).assertExists()
        assertEquals(listOf("announcement-handled"), calls)
    }

    // SF02-A16 / S42-R20, S42-A16: number, − and + and the slider expose label and value with unit; the number clicks
    @Test
    fun weightControls_exposeLabelAndValueWithUnit() {
        show(loadedState)

        composeRule.onNode(
            hasContentDescription(text(R.string.logger_value_description, text(R.string.logger_weight_label), "60 kilogramos")) and hasClickAction(),
        ).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.logger_weight_decrease)).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.logger_weight_increase)).assertIsDisplayed().performClick()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "60 kilogramos")).assertIsDisplayed()

        assertEquals(listOf("weight-click", "weight-decrease", "weight-increase"), calls)
    }

    // SF02-A16 / S42-R20, S42-A16: the same for the repetitions, with their own unit and step
    @Test
    fun repsControls_exposeLabelAndValueWithUnit() {
        show(loadedState)

        composeRule.onNode(
            hasContentDescription(text(R.string.logger_value_description, text(R.string.logger_reps_label), "10 repeticiones")) and hasClickAction(),
        ).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.logger_reps_decrease)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.logger_reps_increase)).performClick()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "10 repeticiones")).assertIsDisplayed()

        assertEquals(listOf("reps-click", "reps-decrease", "reps-increase"), calls)
    }

    // SF02-A15 / S42-R19, S42-A15: at the limit the button that would leave the range is disabled
    @Test
    fun atTheLimit_theButtonIsDisabled() {
        show(loadedState.copy(weight = ValueInput(0, weightWindow(0), canDecrease = false, canIncrease = true)))

        composeRule.onNodeWithContentDescription(text(R.string.logger_weight_decrease)).assertIsNotEnabled()
    }

    // SF02-A16 / S42-R19: the typing dialog keeps its live text, sends it to confirm and stays open on an error
    @Test
    fun typingDialog_keepsTheTypedText_andShowsTheError() {
        show(loadedState.copy(typingDialog = TypingDialog(ValueField.WEIGHT, initialText = "60")))

        composeRule.onNodeWithText(text(R.string.logger_typing_weight_title)).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("62,3")
        composeRule.onNodeWithText(text(R.string.logger_typing_weight_confirm)).performClick()
        composeRule.waitForIdle()

        assertTrue(calls.contains("typed-change"))
        assertEquals("typed-confirm:62,3", calls.last())
        composeRule.onNodeWithText(text(R.string.session_error_weight_not_multiple)).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertTextContains("62,3")
    }

    // SF02-A16: the typing dialog of the repetitions is closed by its "Cancelar"
    @Test
    fun typingDialog_ofReps_canBeDismissed() {
        show(loadedState.copy(typingDialog = TypingDialog(ValueField.REPS, initialText = "10")))

        composeRule.onNodeWithText(text(R.string.logger_typing_reps_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_typing_dismiss)).performClick()

        assertEquals(listOf("typing-dismiss"), calls)
    }

    // SF02-A17 / S40-R40, S42-A05: with changes, back asks to discard instead of leaving
    @Test
    fun back_withChanges_asksToDiscard() {
        show(loadedState.copy(hasUnsavedChanges = true))

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("discard-request"), calls)
    }

    // SF02-A17: the arrow of the top bar does the same as back
    @Test
    fun backArrow_withChanges_asksToDiscard_andWithoutThemLeaves() {
        show(loadedState.copy(hasUnsavedChanges = true))
        composeRule.onNodeWithContentDescription(text(R.string.logger_back_description)).performClick()

        uiState = uiState.copy(hasUnsavedChanges = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(text(R.string.logger_back_description)).performClick()

        assertEquals(listOf("discard-request", "back"), calls)
    }

    // SF02-A17: without changes the handler is off and back stays with the shell, with no dialog
    @Test
    fun back_withoutChanges_staysWithTheShell() {
        show(loadedState)

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("shell-back"), calls)
    }

    // SF02-A17 / S40-F21: "Descartar" closes the dialog and leaves
    @Test
    fun discardDialog_confirm_dismissesAndLeaves() {
        show(loadedState.copy(hasUnsavedChanges = true, confirmDialog = LoggerDialog.Discard))

        composeRule.onNodeWithText(text(R.string.session_discard_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.session_discard_dialog_confirm)).performClick()

        assertEquals(listOf("dialog-dismiss", "back"), calls)
    }

    // SF02-A11 / S42-R17, S42-A13: tapping a set asks for its correction; the panel then offers its three actions
    @Test
    fun correction_panelOffersSaveChanges_delete_andCancel() {
        show(loadedState)
        composeRule.onNodeWithContentDescription(text(R.string.logger_set_row_description, 2, "60 kilogramos, 10 repeticiones")).performClick()

        uiState = uiState.copy(editingSetId = 12L)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.logger_correcting, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_save_changes)).performClick()
        composeRule.onNodeWithText(text(R.string.logger_delete_set)).performClick()
        composeRule.onNodeWithText(text(R.string.logger_cancel_correction)).performClick()
        composeRule.onNodeWithText(text(R.string.logger_save_set)).assertDoesNotExist()

        assertEquals(listOf("set-click:12", "save", "delete-set", "cancel-correction"), calls)
    }

    // SF02-A11 / S40-F13: the delete dialog names the set and the exercise, and confirms
    @Test
    fun deleteDialog_namesTheSet_andConfirms() {
        show(loadedState.copy(confirmDialog = LoggerDialog.DeleteSet(setNumber = 2)))

        composeRule.onNodeWithText(text(R.string.logger_delete_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_delete_dialog_text, "60 kg × 10", "Press de banca")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_delete_dialog_confirm)).performClick()

        assertEquals(listOf("delete-confirm"), calls)
    }

    // SF02-A12 / S40-R29: an archived exercise offers no new set, says why, and its sets can still be corrected
    @Test
    fun archivedExercise_offersNoNewSet_butItsSetsAreTappable() {
        show(loadedState.copy(exercise = exercise.copy(archived = true)))

        composeRule.onNodeWithText(text(R.string.logger_archived_notice)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.logger_save_set)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.logger_set_row_description, 1, "60 kilogramos, 10 repeticiones")).performClick()

        assertEquals(listOf("set-click:11"), calls)
    }

    // SF02-R13 / D13 de la Spec 40: the fixed note shows only when the exercise has one
    @Test
    fun fixedNote_isHiddenWithoutNote() {
        show(loadedState.copy(exercise = exercise.copy(notes = null)))

        composeRule.onNodeWithText(text(R.string.logger_fixed_note_label)).assertDoesNotExist()
    }

    // SF02-R13, S40-R23: the date is always visible: "Hoy · dom 11 oct", and in a past session its short date
    @Test
    fun date_isAlwaysShown_todayAndPast() {
        show(loadedState)
        composeRule.onNodeWithText(text(R.string.session_today_header, "dom 11 oct")).assertIsDisplayed()

        uiState = uiState.copy(sessionDate = LAST_SESSION)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("jue 8 oct").assertCountEquals(2)
    }

    // SF02-R13, S42-R24 (S42-A20): "Editar ejercicio" always; "Ver estadísticas" only with a lambda
    @Test
    fun topBar_editsTheExercise_andHidesStatsWithoutLambda() {
        show(loadedState)

        composeRule.onNodeWithContentDescription(text(R.string.logger_edit_exercise)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.logger_menu_description)).assertDoesNotExist()

        assertEquals(listOf("edit-exercise:1"), calls)
    }

    // SF02-R13, S42-R24: with the lambda, the menu offers "Ver estadísticas"
    @Test
    fun topBar_withProgress_offersViewStats() {
        show(loadedState, progress = true)

        composeRule.onNodeWithContentDescription(text(R.string.logger_menu_description)).performClick()
        composeRule.onNodeWithText(text(R.string.logger_view_stats)).performClick()

        assertEquals(listOf("progress:1"), calls)
    }

    // SF02-R22, S30-R23: an unexpected failure shows the generic message and is reported as handled
    @Test
    fun genericError_showsTheMessage_andIsHandled() {
        show(loadedState.copy(genericError = true))

        composeRule.onNodeWithText(text(R.string.session_error_generic)).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 8_000) { calls.contains("generic-error-handled") }
    }

    // SF02-A21 / SF02-R23, S42-A23, S41-A11: at 200 % "Guardar serie" is visible and tappable
    @Test
    fun fontScale200_saveButtonIsDisplayedAndClickable() {
        show(loadedState, fontScale = 2f)

        composeRule.onNodeWithText(text(R.string.logger_save_set)).assertIsDisplayed().performClick()

        assertEquals(listOf("save"), calls)
    }

    private companion object {
        /** A Sunday, as in the examples of Spec 42. */
        val TODAY: LocalDate = LocalDate.of(2026, 10, 11)

        /** A Thursday. */
        val LAST_SESSION: LocalDate = LocalDate.of(2026, 10, 8)

        val exercise = LoggerExerciseUi(
            id = 1L,
            name = "Press de banca",
            equipment = Equipment.BARBELL,
            notes = "Asiento en la posición 4",
            archived = false,
        )

        val loadedState = SetLoggerUiState(
            isLoading = false,
            exercise = exercise,
            today = TODAY,
            sessionDate = TODAY,
            resolvedSessionId = 9L,
            todaySets = listOf(
                SetRowUi(setId = 11L, number = 1, weightGrams = 60_000, reps = 10),
                SetRowUi(setId = 12L, number = 2, weightGrams = 60_000, reps = 10),
            ),
            weight = ValueInput(60_000, weightWindow(60_000), canDecrease = true, canIncrease = true),
            reps = ValueInput(10, repsWindow(10), canDecrease = true, canIncrease = true),
        )
    }
}

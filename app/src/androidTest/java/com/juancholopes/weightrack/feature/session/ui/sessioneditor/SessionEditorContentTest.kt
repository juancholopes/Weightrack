package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests of [SessionEditorContent] with a plain `UiState` and lambdas that record their calls: no ViewModel,
 * repository or database is involved (S31-R14, S31-R18). The host applies the few state changes the ViewModel
 * would make so that flows can be followed.
 */
class SessionEditorContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Every call the screen made, in order, as "event" or "event:argument". */
    private val calls = mutableListOf<String>()

    private var uiState by mutableStateOf(formState)

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    /**
     * A day of the month in the open picker. Material 3 exposes each day as one button whose text is its full date in
     * the language of the phone ("viernes, 9 de octubre de 2026", prefixed with "Hoy, " for the real today), never the
     * bare number, so it is found by the number as a whole word, which holds in any language. The headline of the
     * picker also writes the selected day, but it is not a button.
     */
    private fun pickerDay(day: Int): SemanticsNodeInteraction {
        val number = Regex("\\b$day\\b")
        val isDayButton = SemanticsMatcher("is a button whose text has the day $day") { node ->
            node.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { number.containsMatchIn(it.text) }
        }
        return composeRule.onNode(isDayButton)
    }

    private fun show(initial: SessionEditorUiState) {
        uiState = initial
        composeRule.setContent {
            WeightrackTheme {
                NavigationBackHandler(
                    state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                    onBackCompleted = { calls += "shell-back" },
                )
                SessionEditorContent(
                    uiState = uiState,
                    onDateClick = { calls += "date-click" },
                    onDatePickerDismiss = { calls += "date-picker-dismiss" },
                    onDateSelected = { calls += "date-selected:$it" },
                    onNotesChange = {
                        calls += "notes:$it"
                        uiState = uiState.copy(notes = it)
                    },
                    onSaveClick = { calls += "save" },
                    onDiscardRequest = { calls += "discard-request" },
                    onDialogDismiss = { calls += "dialog-dismiss" },
                    onGenericErrorHandled = {
                        calls += "generic-error-handled"
                        uiState = uiState.copy(genericError = false)
                    },
                    onClose = { calls += "close" },
                )
            }
        }
    }

    // SF02-A13 / SF02-R19 (S42-P05): the date is a field that reads its value and opens the picker
    @Test
    fun dateField_showsTheDate_readsItInFull_andOpensThePicker() {
        show(formState)

        composeRule.onNodeWithText("dom 11 oct").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.session_editor_date_description, "domingo 11 de octubre"))
            .performClick()

        assertEquals(listOf("date-click"), calls)
    }

    // SF02-A13 / SF02-R19, S42-A17 (S10-R32): tomorrow cannot be chosen in the picker; today and the past can
    @Test
    fun datePicker_disablesTomorrow() {
        show(formState.copy(showDatePicker = true))

        pickerDay(12).assertIsNotEnabled()
        pickerDay(11).assertIsEnabled()
        pickerDay(10).assertIsEnabled()
    }

    // SF02-A13 / SF02-R19: choosing a day and confirming reports that date
    @Test
    fun datePicker_choosingADay_reportsIt() {
        show(formState.copy(showDatePicker = true))

        pickerDay(9).performClick()
        composeRule.onNodeWithText(text(R.string.session_editor_date_picker_confirm)).performClick()

        assertEquals(listOf("date-selected:2026-10-09"), calls)
    }

    // SF02-R19: cancelling the picker reports it
    @Test
    fun datePicker_cancel_reportsIt() {
        show(formState.copy(showDatePicker = true))

        composeRule.onNodeWithText(text(R.string.session_editor_date_picker_dismiss)).performClick()

        assertEquals(listOf("date-picker-dismiss"), calls)
    }

    // SF02-A13 / SF02-R19, S40-A28: a future date is rejected on save with its message next to the date
    @Test
    fun futureDateError_showsNextToTheDate() {
        show(formState.copy(date = TODAY.plusDays(1), fieldError = WorkoutSessionError.DATE_IN_FUTURE))

        composeRule.onNodeWithText(text(R.string.session_error_date_in_future)).assertIsDisplayed()
    }

    // SF02-R19, S40-R05: a too long note shows its message under the note
    @Test
    fun notesError_showsUnderTheNote() {
        show(formState.copy(fieldError = WorkoutSessionError.NOTES_TOO_LONG))

        composeRule.onNodeWithText(text(R.string.session_error_notes_too_long)).assertIsDisplayed()
    }

    // SF02-R19: the note is typed in its field and reported on every change
    @Test
    fun notes_typing_reportsTheText() {
        show(formState)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Me sentí bien")

        assertEquals(listOf("notes:Me sentí bien"), calls)
        composeRule.onNode(hasSetTextAction()).assertTextContains("Me sentí bien")
    }

    // SF02-R19: "Guardar sesión" reports the tap, and it is disabled while saving (S40-R07)
    @Test
    fun save_reportsTheTap_andIsDisabledWhileSaving() {
        show(formState)
        composeRule.onNodeWithText(text(R.string.session_editor_save)).performClick()

        uiState = uiState.copy(isSaving = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.session_editor_save)).assertIsNotEnabled()
        assertEquals(listOf("save"), calls)
    }

    // SF02-A17 / SF02-R19, S42-R06, S40-A36: with changes, back asks to discard
    @Test
    fun back_withChanges_asksToDiscard() {
        show(formState.copy(hasUnsavedChanges = true))

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("discard-request"), calls)
    }

    // SF02-A17: without changes the handler is off and back stays with the shell, with no dialog
    @Test
    fun back_withoutChanges_staysWithTheShell() {
        show(formState)

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("shell-back"), calls)
    }

    // SF02-A17: the close button does the same as back: it asks with changes and leaves without them
    @Test
    fun closeButton_asksWithChanges_andLeavesWithout() {
        show(formState.copy(hasUnsavedChanges = true))
        composeRule.onNodeWithContentDescription(text(R.string.session_editor_close_description)).performClick()

        uiState = uiState.copy(hasUnsavedChanges = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(text(R.string.session_editor_close_description)).performClick()

        assertEquals(listOf("discard-request", "close"), calls)
    }

    // SF02-A17 / S40-F21: "Descartar" closes the dialog and leaves; "Seguir editando" only closes the dialog
    @Test
    fun discardDialog_confirmLeaves_andKeepEditingStays() {
        show(formState.copy(hasUnsavedChanges = true, showDiscardDialog = true))

        composeRule.onNodeWithText(text(R.string.session_discard_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.session_discard_dialog_dismiss)).performClick()
        composeRule.onNodeWithText(text(R.string.session_discard_dialog_confirm)).performClick()

        assertEquals(listOf("dialog-dismiss", "dialog-dismiss", "close"), calls)
    }

    // SF02-R22, S30-R23: an unexpected failure shows the generic message and is reported as handled
    @Test
    fun genericError_showsTheMessage_andIsHandled() {
        show(formState.copy(genericError = true))

        composeRule.onNodeWithText(text(R.string.session_error_generic)).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 8_000) { calls.contains("generic-error-handled") }
    }

    // S42-R25: nothing of the form shows before the data arrives
    @Test
    fun loading_showsNoForm() {
        show(SessionEditorUiState(today = TODAY, date = TODAY))

        composeRule.onNodeWithText(text(R.string.session_editor_save)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.session_editor_date_label)).assertDoesNotExist()
    }

    private companion object {
        /** A Sunday, as in the examples of Spec 42. */
        val TODAY: LocalDate = LocalDate.of(2026, 10, 11)

        val formState = SessionEditorUiState(isLoading = false, date = TODAY, today = TODAY)
    }
}

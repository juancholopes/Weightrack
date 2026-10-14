package com.juancholopes.weightrack.feature.catalog.ui.editor

import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.ui.labelRes
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests of [ExerciseEditorContent] with a plain `UiState` and lambdas that record their calls: no
 * ViewModel, repository or database is involved (S31-R14, S31-R18). The host applies the same few state
 * changes the ViewModel would (typing, opening and closing dialogs) so that flows can be followed.
 */
class ExerciseEditorContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Every call the screen made, in order, as "event" or "event:argument". */
    private val calls = mutableListOf<String>()

    private var uiState by mutableStateOf(ExerciseEditorUiState())

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun muscleLabel(muscle: Muscle) = text(muscle.labelRes())

    private fun equipmentLabel(equipment: Equipment) = text(equipment.labelRes())

    /**
     * Shows the editor. [shellBack] is what an outer `NavigationBackHandler` does, standing in for the
     * shell's back: it only runs when the editor's own handler is disabled.
     */
    private fun show(initial: ExerciseEditorUiState, fontScale: Float? = null) {
        uiState = initial
        composeRule.setContent {
            val editor = @Composable {
                WeightrackTheme {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                        onBackCompleted = { calls += "shell-back" },
                    )
                    ExerciseEditorContent(
                        uiState = uiState,
                        onNameChange = {
                            calls += "name:$it"
                            uiState = uiState.copy(name = it)
                        },
                        onEquipmentSelect = {
                            calls += "equipment:$it"
                            uiState = uiState.copy(equipment = it)
                        },
                        onMuscleToggle = { calls += "muscle:$it" },
                        onPrimaryChange = { muscle, isPrimary -> calls += "primary:$muscle:$isPrimary" },
                        onNotesChange = {
                            calls += "notes:$it"
                            uiState = uiState.copy(notes = it)
                        },
                        onSaveClick = { calls += "save" },
                        onArchiveClick = { calls += "archive" },
                        onDeleteClick = {
                            calls += "delete"
                            uiState = uiState.copy(confirmDialog = EditorDialog.Delete(uiState.name))
                        },
                        onDeleteConfirm = {
                            calls += "delete-confirm"
                            uiState = uiState.copy(confirmDialog = null)
                        },
                        onUnarchiveConflictClick = { calls += "unarchive-conflict" },
                        onUseOtherNameClick = {
                            calls += "use-other-name"
                            uiState = uiState.copy(archivedConflict = null)
                        },
                        onDiscardRequest = {
                            calls += "discard-request"
                            uiState = uiState.copy(confirmDialog = EditorDialog.Discard)
                        },
                        onDialogDismiss = {
                            calls += "dialog-dismiss"
                            uiState = uiState.copy(confirmDialog = null)
                        },
                        onErrorFocusHandled = {
                            calls += "error-focus-handled"
                            uiState = uiState.copy(focusOnError = false)
                        },
                        onGenericErrorHandled = {
                            calls += "generic-error-handled"
                            uiState = uiState.copy(genericError = false)
                        },
                        onClose = { calls += "close" },
                    )
                }
            }
            if (fontScale == null) {
                editor()
            } else {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) { editor() }
            }
        }
    }

    private fun nameField() = composeRule.onAllNodes(hasSetTextAction())[0]

    private fun notesField() = composeRule.onAllNodes(hasSetTextAction())[1]

    /** The chip of an equipment or muscle; the chosen-muscle rows carry the same name, so they are excluded. */
    private fun chip(label: String) = composeRule.onNode(hasText(label) and !hasAnyContentDescription())

    private fun hasAnyContentDescription() =
        SemanticsMatcher("has a content description") { SemanticsProperties.ContentDescription in it.config }

    private fun primaryRow(muscle: Muscle) =
        composeRule.onNode(hasContentDescription(text(R.string.editor_primary_switch_description, muscleLabel(muscle))))

    private fun inDialog(matcher: SemanticsMatcher) = matcher and hasAnyAncestor(isDialog())

    private fun saveButton() = composeRule.onNodeWithText(text(R.string.editor_save))

    private fun SemanticsNodeInteraction.assertTouchTargetIsAtLeast48dp(): SemanticsNodeInteraction {
        val density = composeRule.activity.resources.displayMetrics.density
        val bounds = fetchSemanticsNode().touchBoundsInRoot
        val minimum = 48 * density
        assertTrue("Touch height ${bounds.height} is below 48 dp ($minimum px)", bounds.height >= minimum - 1f)
        assertTrue("Touch width ${bounds.width} is below 48 dp ($minimum px)", bounds.width >= minimum - 1f)
        return this
    }

    private val filledState = ExerciseEditorUiState(
        isLoading = false,
        isNew = false,
        name = "Press de banca",
        equipment = Equipment.BARBELL,
        muscles = listOf(
            MuscleChoice(Muscle.CHEST, isPrimary = true),
            MuscleChoice(Muscle.TRICEPS, isPrimary = false),
        ),
        notes = "Asiento en la posición 4",
        hasSets = true,
    )

    private val newState = ExerciseEditorUiState(isLoading = false, isNew = true)

    // SF01-A11 / SF01-R15, S40-R49
    @Test
    fun primaryMark_isReadAsTextAndTogglesPerMuscle() {
        show(filledState)

        composeRule.onAllNodesWithText(text(R.string.editor_primary_label)).assertCountEquals(2)
        primaryRow(Muscle.CHEST).assertIsOn()
        primaryRow(Muscle.TRICEPS).assertIsOff()

        primaryRow(Muscle.TRICEPS).performScrollTo().performClick()

        assertEquals(listOf("primary:TRICEPS:true"), calls)
    }

    // SF01-A11 / SF01-R04, SF01-R15, S40-R49
    @Test
    fun nameError_isExposedAsErrorSemanticsAndLiveRegion() {
        val message = text(R.string.catalog_error_name_blank)
        show(filledState.copy(name = "", fieldError = ExerciseError.NAME_BLANK))

        composeRule.onNodeWithText(message).assertIsDisplayed()
        nameField().assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
        composeRule.onNodeWithText(message, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    // SF01-A11 / SF01-R04: a single error, under its group when the group is the one at fault
    @Test
    fun groupError_isShownOnceUnderItsGroupWithErrorSemanticsAndLiveRegion() {
        val message = text(R.string.catalog_error_equipment_missing)
        show(newState.copy(equipment = null, fieldError = ExerciseError.EQUIPMENT_MISSING))

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(message).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
        composeRule.onNodeWithText(message)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertCountEquals(1)
    }

    // SF01-A11 / SF01-R06
    @Test
    fun archiveAction_withSets_isShownAndActsAtOnce() {
        show(filledState.copy(hasSets = true))

        composeRule.onNodeWithText(text(R.string.editor_delete)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.editor_archive)).performScrollTo().assertIsDisplayed().performClick()

        assertEquals(listOf("archive"), calls)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    // SF01-A11 / SF01-R06
    @Test
    fun deleteAction_withoutSets_isShownAndAsksToConfirm() {
        show(filledState.copy(hasSets = false))

        composeRule.onNodeWithText(text(R.string.editor_archive)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.editor_delete)).performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf("delete"), calls)

        composeRule.onNodeWithText(text(R.string.editor_delete_dialog_title, "Press de banca")).assertIsDisplayed()
        composeRule.onNode(inDialog(hasText(text(R.string.editor_delete_dialog_confirm)))).performClick()

        assertEquals(listOf("delete", "delete-confirm"), calls)
    }

    // SF01-A11 / SF01-R06
    @Test
    fun deleteDialog_keep_dismissesWithoutDeleting() {
        show(filledState.copy(hasSets = false, confirmDialog = EditorDialog.Delete("Press de banca")))

        composeRule.onNode(inDialog(hasText(text(R.string.editor_delete_dialog_dismiss)))).performClick()

        assertEquals(listOf("dialog-dismiss"), calls)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    // SF01-A11 / SF01-R06: neither action while hasSets is null (creating or loading)
    @Test
    fun noDestructiveAction_whileHasSetsIsNull() {
        show(newState.copy(hasSets = null))

        composeRule.onNodeWithText(text(R.string.editor_archive)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.editor_delete)).assertDoesNotExist()
        saveButton().assertIsDisplayed()
    }

    // SF01-A11 / SF01-R15, S41-R15 (font at 200 %)
    @Test
    fun saveButton_atFontScale200_isDisplayedAndClickable() {
        show(filledState, fontScale = 2f)

        saveButton().assertIsDisplayed().performClick()

        assertEquals(listOf("save"), calls)
    }

    // SF01-A11 / SF01-R15 (keyboard applied with Modifier.fitInside(WindowInsetsRulers.Ime.current))
    @Test
    fun saveButton_withKeyboardOpen_staysAboveTheKeyboard() {
        val activity = composeRule.activity
        composeRule.runOnUiThread {
            activity.enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            // The manifest of MainActivity declares adjustResize; the test activity does not.
            @Suppress("DEPRECATION")
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        show(filledState)

        nameField().performClick()
        val decorView = activity.window.decorView
        val deadline = SystemClock.uptimeMillis() + KEYBOARD_TIMEOUT_MILLIS
        while (!isKeyboardVisible() && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_MILLIS)
        }
        assertTrue("The keyboard did not open", isKeyboardVisible())
        // The keyboard animates in; wait for the layout to settle before measuring.
        composeRule.waitForIdle()
        SystemClock.sleep(SETTLE_MILLIS)
        composeRule.waitForIdle()

        val keyboardTop = decorView.height -
            ViewCompat.getRootWindowInsets(decorView)!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val saveBottom = saveButton().assertIsDisplayed().fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("Save ends at $saveBottom, below the keyboard top $keyboardTop", saveBottom <= keyboardTop + 1f)
    }

    private fun isKeyboardVisible(): Boolean {
        val decorView = composeRule.activity.window.decorView
        return ViewCompat.getRootWindowInsets(decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    }

    // SF01-A11 / SF01-R15, S40-R46
    @Test
    fun touchTargets_measureAtLeast48dp() {
        show(filledState)

        composeRule.onNodeWithContentDescription(text(R.string.editor_close_description)).assertTouchTargetIsAtLeast48dp()
        chip(equipmentLabel(Equipment.CABLE)).performScrollTo().assertTouchTargetIsAtLeast48dp()
        chip(muscleLabel(Muscle.BICEPS)).performScrollTo().assertTouchTargetIsAtLeast48dp()
        primaryRow(Muscle.CHEST).performScrollTo().assertTouchTargetIsAtLeast48dp()
        composeRule.onNodeWithText(text(R.string.editor_archive)).performScrollTo().assertTouchTargetIsAtLeast48dp()
        saveButton().assertTouchTargetIsAtLeast48dp()
    }

    // SF01-A11 / SF01-R02: the form reports each edit to its callback
    @Test
    fun fields_reportEditsToTheirCallbacks() {
        show(newState)

        nameField().performTextInput("Remo")
        notesField().performScrollTo().performTextInput("Agarre ancho")
        chip(equipmentLabel(Equipment.CABLE)).performScrollTo().performClick()
        chip(muscleLabel(Muscle.LATS)).performScrollTo().performClick()
        saveButton().performClick()

        assertEquals(
            listOf("name:Remo", "notes:Agarre ancho", "equipment:CABLE", "muscle:LATS", "save"),
            calls,
        )
    }

    // SF01-A11 / SF01-R09: a save in progress cannot be repeated
    @Test
    fun saveButton_whileSaving_isDisabled() {
        show(filledState.copy(isSaving = true))

        saveButton().assertIsNotEnabled()
    }

    // SF01-A11: nothing of the form is shown before the stored values arrive
    @Test
    fun form_whileLoading_isNotShown() {
        show(ExerciseEditorUiState(isLoading = true, isNew = false))

        saveButton().assertDoesNotExist()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }

    // SF01-A11 / SF01-R15, S42-P03: title by mode
    @Test
    fun title_followsTheMode() {
        show(filledState)

        composeRule.onNodeWithText(text(R.string.editor_title_edit)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertDoesNotExist()
    }

    // SF01-A12 / SF01-R08, S40-A36
    @Test
    fun back_withUnsavedChanges_opensDiscardDialogAndKeepEditingKeepsWhatWasTyped() {
        show(filledState.copy(hasUnsavedChanges = true))

        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.editor_discard_dialog_title)).assertIsDisplayed()
        assertEquals(listOf("discard-request"), calls)

        composeRule.onNode(inDialog(hasText(text(R.string.editor_discard_dialog_dismiss)))).performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        nameField().assertTextContains("Press de banca")
        assertEquals(listOf("discard-request", "dialog-dismiss"), calls)
    }

    // SF01-A12 / SF01-R08, S40-A36: "Descartar" closes and writes nothing
    @Test
    fun discardDialog_discard_closesWithoutWriting() {
        show(filledState.copy(hasUnsavedChanges = true, confirmDialog = EditorDialog.Discard))

        composeRule.onNode(inDialog(hasText(text(R.string.editor_discard_dialog_confirm)))).performClick()

        assertTrue("A write or a back was reported: $calls", calls.none { it == "save" || it == "shell-back" })
        assertEquals(1, calls.count { it == "close" })
    }

    // SF01-A12 / SF01-R08, S40-A36: without changes, back is left to the shell and no dialog opens
    @Test
    fun back_withoutUnsavedChanges_leavesBackToTheShellWithoutDialog() {
        show(filledState.copy(hasUnsavedChanges = false))

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("shell-back"), calls)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    // SF01-A12 / SF01-R08: the close button does what back does
    @Test
    fun closeButton_withUnsavedChanges_opensDiscardDialog() {
        show(filledState.copy(hasUnsavedChanges = true))

        composeRule.onNodeWithContentDescription(text(R.string.editor_close_description)).performClick()

        composeRule.onNodeWithText(text(R.string.editor_discard_dialog_title)).assertIsDisplayed()
        assertEquals(listOf("discard-request"), calls)
    }

    // SF01-A12 / SF01-R08
    @Test
    fun closeButton_withoutUnsavedChanges_closesWithoutDialog() {
        show(filledState.copy(hasUnsavedChanges = false))

        composeRule.onNodeWithContentDescription(text(R.string.editor_close_description)).performClick()

        assertEquals(listOf("close"), calls)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    // SF01-A12 / SF01-R05, S40-R17: the archived-name dialog offers both ways out
    @Test
    fun archivedConflictDialog_offersUnarchiveAndAnotherName() {
        show(newState.copy(name = "Press de banca", archivedConflict = ArchivedConflict(7L, "Press de banca")))

        composeRule.onNodeWithText(text(R.string.editor_archived_dialog_title, "Press de banca")).assertIsDisplayed()
        composeRule.onNode(inDialog(hasText(text(R.string.editor_archived_dialog_confirm)))).performClick()
        assertEquals(listOf("unarchive-conflict"), calls)

        composeRule.onNode(inDialog(hasText(text(R.string.editor_archived_dialog_dismiss)))).performClick()
        assertEquals(listOf("unarchive-conflict", "use-other-name"), calls)
        nameField().assertTextContains("Press de banca")
    }

    // SF01-A11 / SF01-R09, S30-R23: the generic error is a snackbar, then its effect is handled
    @Test
    fun genericError_showsSnackbarAndThenIsHandled() {
        show(filledState.copy(genericError = true))

        composeRule.onNodeWithText(text(R.string.catalog_error_generic)).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(SNACKBAR_WAIT_MILLIS)
        composeRule.waitForIdle()

        assertEquals(listOf("generic-error-handled"), calls)
    }

    // SF01-A11 / SF01-R04, S40-R47: focus goes to the field in error
    @Test
    fun focusOnError_movesFocusToTheNameField() {
        show(filledState.copy(name = "", fieldError = ExerciseError.NAME_BLANK, focusOnError = true))

        composeRule.waitForIdle()

        nameField().assertIsFocused()
        assertEquals(listOf("error-focus-handled"), calls)
    }

    // SF01-A11 / SF01-R04, S40-R47: for a group, to its first chip
    @Test
    fun focusOnError_forTheEquipmentGroup_focusesItsFirstChip() {
        show(newState.copy(name = "Remo", equipment = null, fieldError = ExerciseError.EQUIPMENT_MISSING, focusOnError = true))

        composeRule.waitForIdle()

        chip(equipmentLabel(Equipment.MACHINE)).assertIsFocused()
        assertEquals(listOf("error-focus-handled"), calls)
    }

    // SF01-A11 / SF01-R04: the notes error sits next to the notes field
    @Test
    fun notesError_isExposedOnTheNotesField() {
        val message = text(R.string.catalog_error_notes_too_long)
        show(filledState.copy(fieldError = ExerciseError.NOTES_TOO_LONG))

        composeRule.onNodeWithText(message).performScrollTo().assertIsDisplayed()
        notesField().assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
        nameField().assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    private companion object {
        const val KEYBOARD_TIMEOUT_MILLIS = 10_000L
        const val POLL_MILLIS = 50L
        const val SETTLE_MILLIS = 800L
        const val SNACKBAR_WAIT_MILLIS = 6_000L
    }
}

package com.juancholopes.weightrack.feature.catalog.ui.archived

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests of [ArchivedExercisesContent] with a plain `UiState` and lambdas that record their calls: no
 * ViewModel, repository or database is involved (S31-R14, S31-R18). The host applies the same change the
 * ViewModel makes when unarchiving: the row leaves the list and the notice carries the name.
 */
class ArchivedExercisesContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Every call the screen made, in order, as "event" or "event:argument". */
    private val calls = mutableListOf<String>()

    private var uiState by mutableStateOf(ArchivedExercisesUiState())

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun show(initial: ArchivedExercisesUiState, fontScale: Float? = null) {
        uiState = initial
        composeRule.setContent {
            val content = @Composable {
                WeightrackTheme {
                    ArchivedExercisesContent(
                        uiState = uiState,
                        onUnarchiveClick = { id ->
                            calls += "unarchive:$id"
                            val exercise = uiState.items.first { it.id == id }
                            uiState = uiState.copy(items = uiState.items - exercise, notice = exercise.name)
                        },
                        onNoticeHandled = {
                            calls += "notice-handled"
                            uiState = uiState.copy(notice = null)
                        },
                        onGenericErrorHandled = {
                            calls += "generic-error-handled"
                            uiState = uiState.copy(genericError = false)
                        },
                        onBack = { calls += "back" },
                    )
                }
            }
            if (fontScale == null) {
                content()
            } else {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) { content() }
            }
        }
    }

    private fun SemanticsNodeInteraction.assertTouchTargetIsAtLeast48dp(): SemanticsNodeInteraction {
        val density = composeRule.activity.resources.displayMetrics.density
        val bounds = fetchSemanticsNode().touchBoundsInRoot
        val minimum = 48 * density
        assertTrue("Touch height ${bounds.height} is below 48 dp ($minimum px)", bounds.height >= minimum - 1f)
        assertTrue("Touch width ${bounds.width} is below 48 dp ($minimum px)", bounds.width >= minimum - 1f)
        return this
    }

    private val pressDeBanca = Exercise(id = 3L, name = "Press de banca", equipment = Equipment.BARBELL, notes = null, archived = true)
    private val remoConMancuerna = Exercise(id = 8L, name = "Remo con mancuerna", equipment = Equipment.DUMBBELL, notes = null, archived = true)
    private val loaded = ArchivedExercisesUiState(isLoading = false, items = listOf(pressDeBanca, remoConMancuerna))

    private fun unarchiveButton(exercise: Exercise) =
        composeRule.onNodeWithContentDescription(text(R.string.archived_unarchive_description, exercise.name))

    // SF01-A13 / SF01-R10, S42-R25: no empty state before the first data
    @Test
    fun emptyState_whileLoading_isNotShown() {
        show(ArchivedExercisesUiState(isLoading = true))

        composeRule.onNodeWithText(text(R.string.archived_empty)).assertDoesNotExist()
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(1)
    }

    // SF01-A13 / SF01-R10, S42-R25: loaded and empty, it explains what appears here
    @Test
    fun emptyState_afterLoadingWithNoItems_showsItsText() {
        show(ArchivedExercisesUiState(isLoading = false, items = emptyList()))

        composeRule.onNodeWithText(text(R.string.archived_empty)).assertIsDisplayed()
    }

    // SF01-A13 / SF01-R10: one row per exercise, by name, each with its own "Desarchivar"
    @Test
    fun list_showsEachNameWithItsUnarchiveButton() {
        show(loaded)

        composeRule.onNodeWithText("Press de banca").assertIsDisplayed()
        composeRule.onNodeWithText("Remo con mancuerna").assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.archived_unarchive)).assertCountEquals(2)
        composeRule.onNodeWithText(text(R.string.archived_empty)).assertDoesNotExist()

        unarchiveButton(remoConMancuerna).performClick()

        assertEquals(listOf("unarchive:8"), calls)
    }

    // SF01-A13 / SF01-R10, S40-R20: after unarchiving, the row leaves and this screen shows the notice
    @Test
    fun unarchive_showsNoticeWithTheNameAndThenIsHandled() {
        show(loaded)

        unarchiveButton(pressDeBanca).performClick()

        composeRule.onNodeWithText("Press de banca").assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.archived_notice_returned, "Press de banca")).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(SNACKBAR_WAIT_MILLIS)
        composeRule.waitForIdle()

        assertEquals(listOf("unarchive:3", "notice-handled"), calls)
    }

    // SF01-A13 / SF01-R10, S40-R20: unarchiving the last one shows the notice and then the empty state
    @Test
    fun unarchive_ofTheLastItem_showsNoticeAndEmptyState() {
        show(loaded.copy(items = listOf(pressDeBanca)))

        unarchiveButton(pressDeBanca).performClick()

        composeRule.onNodeWithText(text(R.string.archived_notice_returned, "Press de banca")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.archived_empty)).assertIsDisplayed()
    }

    // SF01-A13 / SF01-R10, S30-R23: an unexpected failure is a snackbar with its own text
    @Test
    fun genericError_showsSnackbarAndThenIsHandled() {
        show(loaded.copy(genericError = true))

        composeRule.onNodeWithText(text(R.string.archived_error_generic)).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(SNACKBAR_WAIT_MILLIS)
        composeRule.waitForIdle()

        assertEquals(listOf("generic-error-handled"), calls)
    }

    // SF01-A13 / SF01-R10: the back button leaves through onBack
    @Test
    fun backButton_callsOnBack() {
        show(loaded)

        composeRule.onNodeWithContentDescription(text(R.string.archived_back_description)).performClick()

        assertEquals(listOf("back"), calls)
    }

    // SF01-A13 / SF01-R15, S40-R46
    @Test
    fun touchTargets_measureAtLeast48dp() {
        show(loaded)

        composeRule.onNodeWithContentDescription(text(R.string.archived_back_description)).assertTouchTargetIsAtLeast48dp()
        unarchiveButton(pressDeBanca).assertTouchTargetIsAtLeast48dp()
    }

    // SF01-A13 / S41-R15: at 200 % the action of each row is still shown and works
    @Test
    fun unarchiveButton_atFontScale200_isDisplayedAndClickable() {
        show(loaded, fontScale = 2f)

        unarchiveButton(pressDeBanca).assertIsDisplayed().performClick()

        assertEquals(listOf("unarchive:3"), calls)
    }

    private companion object {
        const val SNACKBAR_WAIT_MILLIS = 6_000L
    }
}

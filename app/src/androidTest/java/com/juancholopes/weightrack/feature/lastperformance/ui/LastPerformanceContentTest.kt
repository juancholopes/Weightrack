package com.juancholopes.weightrack.feature.lastperformance.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests of [LastPerformanceContent] with a plain `UiState` and a lambda that records its calls: no ViewModel,
 * repository or database is involved (S31-R14, S31-R18). Dates and sets use the formats of `Formats.kt` (S42-R26).
 */
class LastPerformanceContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var uiState by mutableStateOf<LastPerformanceUiState>(LastPerformanceUiState.Loading)

    /** How many times "Ver estadísticas" was tapped. */
    private var progressOpened = 0

    private val oct8 = LocalDate.of(2026, 10, 8)
    private val oct10 = LocalDate.of(2026, 10, 10)

    /** Sunday. */
    private val oct11 = LocalDate.of(2026, 10, 11)

    private val threeSets = listOf(SetValues(60_000, 10), SetValues(60_000, 10), SetValues(60_000, 8))
    private val thursday = LastPerformanceUiState.Shown(date = oct8, today = oct11, sets = threeSets)

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun show(initial: LastPerformanceUiState, withProgress: Boolean = false, fontScale: Float? = null) {
        uiState = initial
        composeRule.setContent {
            val content = @Composable {
                WeightrackTheme {
                    LastPerformanceContent(
                        uiState = uiState,
                        onOpenProgress = if (withProgress) ({ progressOpened++ }) else null,
                        modifier = Modifier.testTag(SECTION_TAG),
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

    private val headingNode get() = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    // The merged tree holds only the node TalkBack reads; the texts the user sees are its children in the unmerged tree.
    private fun visibleText(value: String) = composeRule.onNodeWithText(value, useUnmergedTree = true)

    // SF03-A06 / SF03-R05, S40-A31: the date of the session and all its sets, in order, with the unit only on the first
    @Test
    fun shown_showsTheDateAndTheSetsInOrder() {
        show(thursday)

        visibleText("Última vez · jue 8 oct").assertIsDisplayed()
        visibleText("60 kg × 10 · 60 × 10 · 60 × 8").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.last_performance_first_time), useUnmergedTree = true).assertDoesNotExist()
    }

    // SF03-A06 / SF03-R05, S42-R26: the day before today and today read "Ayer" and "Hoy"
    @Test
    fun shown_withTheDayBeforeToday_saysAyer() {
        show(LastPerformanceUiState.Shown(date = oct10, today = oct11, sets = threeSets))

        visibleText("Última vez · Ayer").assertIsDisplayed()
    }

    // SF03-A06 / SF03-R05, S42-R26: a date of another year keeps its year
    @Test
    fun shown_withADateOfAnotherYear_keepsTheYear() {
        show(LastPerformanceUiState.Shown(date = LocalDate.of(2025, 10, 9), today = oct11, sets = listOf(SetValues(62_500, 8))))

        visibleText("Última vez · jue 9 oct 2025").assertIsDisplayed()
        visibleText("62,5 kg × 8").assertIsDisplayed()
    }

    // SF03-A06 / SF03-R06, S40-R49, S42-P02: one heading and one node that reads the sets with their units
    @Test
    fun shown_isOneHeadingNode_readWithUnits() {
        show(thursday)

        headingNode.assertIsDisplayed()
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertCountEquals(1)
        val reading = headingNode.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString(" ")
        assertEquals(
            "Última vez, jueves 8 de octubre: 60 kilogramos, 10 repeticiones; 60 kilogramos, 10 repeticiones; " +
                "60 kilogramos, 8 repeticiones",
            reading,
        )
        assertEquals(0, headingNode.fetchSemanticsNode().children.size)
    }

    // SF03-A06 / SF03-R06, S40-R49: with the year of another one and a single repetition, the units use the singular
    @Test
    fun shown_readsTheSingularAndTheYear() {
        show(LastPerformanceUiState.Shown(date = LocalDate.of(2025, 10, 9), today = oct11, sets = listOf(SetValues(1_000, 1))))

        val reading = headingNode.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString(" ")
        assertEquals("Última vez, jueves 9 de octubre de 2025: 1 kilogramo, 1 repetición", reading)
    }

    // SF03-A06 / SF03-R05, S40-R35: without earlier sets it says it is the first time, and shows no date
    @Test
    fun firstTime_saysItIsTheFirstTime() {
        show(LastPerformanceUiState.FirstTime)

        composeRule.onNodeWithText(text(R.string.last_performance_first_time)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Última vez", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    // SF03-A06 / SF03-R05, S42-R25: while loading it shows no text of the other states
    @Test
    fun loading_showsNoText() {
        show(LastPerformanceUiState.Loading)

        composeRule.onAllNodesWithText("Última vez", substring = true, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Primera vez", substring = true, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(hasText("kg", substring = true), useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithText(text(R.string.last_performance_error), useUnmergedTree = true).assertDoesNotExist()
    }

    // SF03-A06 / SF03-R05: while loading it reserves the height of a header line and a sets line, so the panel does not jump
    @Test
    fun loading_takesTheHeightOfTheShownSection() {
        show(thursday)
        val shownHeight = composeRule.onNodeWithTag(SECTION_TAG).getUnclippedBoundsInRoot().height

        uiState = LastPerformanceUiState.Loading
        composeRule.waitForIdle()
        val loadingHeight = composeRule.onNodeWithTag(SECTION_TAG).getUnclippedBoundsInRoot().height

        assertTrue("The loading section is empty", loadingHeight > 0.dp)
        assertTrue("Loading is $loadingHeight tall and the sets $shownHeight", (loadingHeight - shownHeight).value in -1f..1f)
    }

    // SF03-A06 / SF03-R05, SF03-R08, D3: the failure is a short text in the section
    @Test
    fun error_showsItsText() {
        show(LastPerformanceUiState.Error)

        composeRule.onNodeWithText(text(R.string.last_performance_error)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Primera vez", substring = true).assertCountEquals(0)
    }

    // SF03-A07 / SF03-R07, S42-A20, S42-R24: with onOpenProgress = null "Ver estadísticas" does not appear in any state
    @Test
    fun viewStats_withoutOnOpenProgress_isNotShown() {
        val states = listOf(
            thursday,
            LastPerformanceUiState.FirstTime,
            LastPerformanceUiState.Loading,
            LastPerformanceUiState.Error,
        )
        show(states.first(), withProgress = false)

        states.forEach { state ->
            uiState = state
            composeRule.waitForIdle()
            composeRule.onNodeWithText(text(R.string.last_performance_view_stats)).assertDoesNotExist()
        }
    }

    // SF03-A07 / SF03-R07: with onOpenProgress, "Ver estadísticas" appears and calls it
    @Test
    fun viewStats_withOnOpenProgress_isShownAndCallsIt() {
        show(thursday, withProgress = true)

        composeRule.onNodeWithText(text(R.string.last_performance_view_stats)).assertIsDisplayed().performClick()

        assertEquals(1, progressOpened)
        // The link is outside the node of the reading, so the reading stays a single heading node.
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertCountEquals(1)
    }

    // SF03-A06 / SF03-R05, S41-A11: at 200 % the header and the sets are still there
    @Test
    fun shown_atTwiceTheFontSize_showsTheDateAndTheSets() {
        show(thursday, fontScale = 2f)

        visibleText("Última vez · jue 8 oct").assertIsDisplayed()
        visibleText("60 kg × 10 · 60 × 10 · 60 × 8").assertIsDisplayed()
    }

    private companion object {
        const val SECTION_TAG = "lastPerformanceSection"
    }
}

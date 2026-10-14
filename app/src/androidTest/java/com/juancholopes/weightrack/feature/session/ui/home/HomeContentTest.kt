package com.juancholopes.weightrack.feature.session.ui.home

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.ui.labelRes
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests of [HomeContent] with a plain `UiState` and lambdas that record their calls: no ViewModel, repository
 * or database is involved (S31-R14, S31-R18). The host applies the few state changes the ViewModel would make
 * (selected tab, notice handled) so that flows can be followed.
 */
class HomeContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Every call the screen made, in order, as "event" or "event:argument". */
    private val calls = mutableListOf<String>()

    private var uiState by mutableStateOf(HomeUiState(today = TODAY))

    private fun text(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun show(initial: HomeUiState, fontScale: Float? = null, progress: Boolean = false, history: Boolean = false) {
        uiState = initial
        composeRule.setContent {
            val home = @Composable {
                WeightrackTheme {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                        onBackCompleted = { calls += "shell-back" },
                    )
                    HomeContent(
                        uiState = uiState,
                        onTabSelect = {
                            calls += "tab:$it"
                            uiState = uiState.copy(selectedTab = it)
                        },
                        onQueryChange = {
                            calls += "query:$it"
                            uiState = uiState.copy(query = it)
                        },
                        onClearSearch = {
                            calls += "clear-search"
                            uiState = uiState.copy(query = "")
                        },
                        onMuscleFilterSelect = { calls += "muscle:$it" },
                        onEquipmentFilterSelect = { calls += "equipment:$it" },
                        onStartAnotherSessionClick = { calls += "start-another" },
                        onUndoArchiveClick = { calls += "undo:$it" },
                        onNoticeHandled = {
                            calls += "notice-handled"
                            uiState = uiState.copy(notice = null)
                        },
                        onGenericErrorHandled = {
                            calls += "generic-error-handled"
                            uiState = uiState.copy(genericError = false)
                        },
                        onOpenLogger = { calls += "logger:$it" },
                        onCreateExercise = { prefill, log -> calls += "create:$prefill:$log" },
                        onEditExercise = { calls += "edit-exercise:$it" },
                        onEditSession = { calls += "edit-session:$it" },
                        onOpenArchived = { calls += "archived" },
                        onOpenProgress = if (progress) { id -> calls += "progress:$id" } else null,
                        onOpenHistory = if (history) ({ calls += "history" }) else null,
                    )
                }
            }
            if (fontScale == null) {
                home()
            } else {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) { home() }
            }
        }
    }

    /** The one node of a card: it is found by what it reads, because the card clears its children's semantics. */
    private fun card(name: String) = composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction())

    private fun hasClickAction() =
        SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.OnClick)

    /**
     * The floating button, found by what TalkBack reads: Material 3 clears the semantics of the label of an extended
     * FAB and takes its name from the icon's description, so there is no node with its text in the merged tree.
     */
    private fun createFab() =
        composeRule.onNode(hasContentDescription(text(R.string.home_create_exercise)) and hasClickAction())

    private fun menuButton(name: String) =
        composeRule.onNodeWithContentDescription(text(R.string.card_menu_description, name), useUnmergedTree = true)

    private val bench = ExerciseCardUi(
        exerciseId = 1L,
        name = "Press de banca",
        primaryMuscle = Muscle.CHEST,
        equipment = Equipment.BARBELL,
        archived = false,
        peakWeightGrams = 60_000,
        peakDate = LAST_SESSION,
        setsToday = null,
    )
    private val benchToday = bench.copy(setsToday = 3, peakDate = TODAY)
    private val row = bench.copy(exerciseId = 2L, name = "Remo en polea", primaryMuscle = Muscle.LATS, equipment = Equipment.CABLE)
    private val squat = bench.copy(
        exerciseId = 3L,
        name = "Sentadilla",
        primaryMuscle = Muscle.QUADRICEPS,
        peakWeightGrams = null,
        peakDate = null,
    )

    private val todayState = HomeUiState(
        isLoading = false,
        today = TODAY,
        todaySession = TodaySessionUi(sessionId = 9L, date = TODAY, notes = "Me sentí con energía"),
        todayCards = listOf(benchToday),
        lastSessionDate = LAST_SESSION,
        lastSessionCards = listOf(row),
        exerciseCards = listOf(bench, row, squat),
        canStartAnotherSession = true,
    )

    private val exercisesState = todayState.copy(selectedTab = HomeTab.EXERCISES)

    // SF02-A15 / SF02-R10, S42-R03 (the home opens on Hoy)
    @Test
    fun tabs_openOnToday_andSelectingExercisesReportsIt() {
        show(todayState)

        composeRule.onNodeWithText(text(R.string.home_tab_today)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.home_tab_exercises)).performClick()

        assertEquals(listOf("tab:EXERCISES"), calls)
        composeRule.onNodeWithText(text(R.string.home_tab_exercises)).assertIsSelected()
    }

    // SF02-A15 / SF02-R21, S41-R12 (the tabs have the Tab role, in a selectable group)
    @Test
    fun tabs_haveTheTabRole() {
        show(todayState)

        composeRule.onNode(
            hasText(text(R.string.home_tab_exercises)) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
        ).assertIsDisplayed()
    }

    // SF02-A15 / SF02-R11, S42-R14 (S42-A11): the floating button in Hoy logs after saving
    @Test
    fun createButton_inToday_opensTheEditorWithLogAfterSave() {
        show(todayState)

        createFab().performClick()

        assertEquals(listOf("create::true"), calls)
    }

    // SF02-A15 / SF02-R11, S42-R14: the floating button in Ejercicios goes back to the list
    @Test
    fun createButton_inExercises_opensTheEditorWithoutLogAfterSave() {
        show(exercisesState)

        createFab().performClick()

        assertEquals(listOf("create::false"), calls)
    }

    // SF02-A15 / SF02-R08, S40-R14 (S42-A09, S40-A12): no results offers to create with the text typed
    @Test
    fun search_withoutResults_offersCreateWithThePrefillAndClear() {
        show(exercisesState.copy(query = "remo con barra", exerciseCards = emptyList()))

        composeRule.onNodeWithText(text(R.string.home_no_results, "remo con barra")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_create_named, "remo con barra")).performClick()
        composeRule.onNodeWithText(text(R.string.home_clear_search)).performClick()

        assertEquals(listOf("create:remo con barra:true", "clear-search"), calls)
    }

    // SF02-A15 / SF02-R08 (typing in the search field reports the text)
    @Test
    fun search_typing_reportsTheText() {
        show(exercisesState)

        composeRule.onNodeWithText(text(R.string.home_search_label)).performTextInput("elev")

        assertEquals(listOf("query:elev"), calls)
    }

    // SF02-A15 / SF02-R08 (the two filters are chips with a menu; "Todos" clears one)
    @Test
    fun filters_chooseAnEquipmentAndAMuscle() {
        show(exercisesState)

        composeRule.onNodeWithText(text(R.string.home_filter_equipment)).performClick()
        composeRule.onNodeWithText(text(Equipment.BARBELL.labelRes())).performClick()
        composeRule.onNodeWithText(text(R.string.home_filter_muscle)).performClick()
        composeRule.onNodeWithText(text(Muscle.LATS.labelRes())).performClick()

        assertEquals(listOf("equipment:BARBELL", "muscle:LATS"), calls)
    }

    // SF02-A15 / SF02-R09, S42-R11 (S42-A12): tapping a card opens the logger with its id
    @Test
    fun tapCard_opensTheLoggerWithItsExerciseId() {
        show(exercisesState)

        card("Remo en polea").performClick()

        assertEquals(listOf("logger:2"), calls)
    }

    // SF02-A15 / SF02-R09 (S42-A21): a card is one node that reads name, muscle, equipment, date and weight with unit
    @Test
    fun card_isOneNode_thatReadsNameMuscleEquipmentDateAndWeight() {
        show(exercisesState)

        val description = card("Press de banca").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(" ")

        listOf("Press de banca", "Pecho", "Barra", "jueves 8 de octubre", "60 kilogramos").forEach {
            assertTrue("The card does not read \"$it\": $description", description.contains(it))
        }
        // The title is inside the card node: it is not a second node of its own.
        composeRule.onAllNodesWithText("Press de banca").assertCountEquals(1)
    }

    // SF02-A15 / SF02-R09: a card without sets says so and has no number
    @Test
    fun card_withoutSets_readsNoSets() {
        show(exercisesState)

        val description = card("Sentadilla").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(" ")

        assertTrue(description.contains(text(R.string.card_no_sets)))
        assertTrue("A card without sets reads no weight: $description", !description.contains("kilogramos"))
    }

    // SF02-A15 / SF02-R09, S42-A09 (the menu has "Editar ejercicio"; stats only with a lambda)
    @Test
    fun cardMenu_withoutProgress_offersOnlyEditExercise() {
        show(exercisesState)

        menuButton("Press de banca").performClick()

        composeRule.onNodeWithText(text(R.string.card_action_view_stats)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.card_action_edit_exercise)).performClick()
        assertEquals(listOf("edit-exercise:1"), calls)
    }

    // SF02-A15 / SF02-R09, S42-R24 (with the lambda, "Ver estadísticas" appears and opens progress)
    @Test
    fun cardMenu_withProgress_offersViewStatsToo() {
        show(exercisesState, progress = true)

        menuButton("Press de banca").performClick()
        composeRule.onNodeWithText(text(R.string.card_action_view_stats)).performClick()

        assertEquals(listOf("progress:1"), calls)
    }

    // SF02-A15 / SF02-R06, S42-R24 (S42-A20): no history icon until F04 gives the lambda
    @Test
    fun historyAction_isHiddenWithoutLambda_andShownWithIt() {
        show(todayState)
        composeRule.onNodeWithContentDescription(text(R.string.home_history)).assertDoesNotExist()
    }

    // SF02-A15 / SF02-R06, S42-R24
    @Test
    fun historyAction_withLambda_opensHistory() {
        show(todayState, history = true)

        composeRule.onNodeWithContentDescription(text(R.string.home_history)).performClick()

        assertEquals(listOf("history"), calls)
    }

    // SF02-A15 / SF02-R06: the header of Hoy, the note and "Editar sesión"
    @Test
    fun today_withASession_showsDateNoteAndEditSession() {
        show(todayState)

        composeRule.onNodeWithText(text(R.string.session_today_header, "dom 11 oct")).assertIsDisplayed()
        composeRule.onNodeWithText("Me sentí con energía").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.home_edit_session)).performClick()

        assertEquals(listOf("edit-session:9"), calls)
    }

    // SF02-A15 / SF02-R06 (S40-A08): the cards of today say how many sets they have
    @Test
    fun today_cardsSayHowManySetsToday() {
        show(todayState)

        composeRule.onNodeWithText("3 series hoy").assertIsDisplayed()
    }

    // SF02-A15 / SF02-R06, SF02-R09 (D4): "De tu última sesión" with its date, hidden when it has no cards
    @Test
    fun lastSession_showsItsHeader_andHidesItWhenEmpty() {
        show(todayState)
        composeRule.onNodeWithText(text(R.string.home_last_session_header, "jue 8 oct")).assertIsDisplayed()

        uiState = uiState.copy(lastSessionCards = emptyList())
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.home_last_session_header, "jue 8 oct")).assertDoesNotExist()
    }

    // SF02-A15 / SF02-R06 (S42-A10): a session without sets shows the empty text, the cards of the last session stay
    @Test
    fun today_sessionWithoutSets_showsTheEmptyText() {
        show(todayState.copy(todayCards = emptyList()))

        composeRule.onNodeWithText(text(R.string.home_today_empty)).assertIsDisplayed()
        card("Remo en polea").assertIsDisplayed()
    }

    // SF02-A15 / SF02-R06: the link to the catalog switches the tab
    @Test
    fun today_seeAllExercises_switchesToTheExercisesTab() {
        show(todayState)

        composeRule.onNodeWithText(text(R.string.home_see_all_exercises)).performClick()

        assertEquals(listOf("tab:EXERCISES"), calls)
    }

    // SF02-A15 / SF02-R07 (S40-A08, S42-A10): the home menu
    @Test
    fun homeMenu_withASession_offersArchivedAndStartAnother() {
        show(todayState)

        composeRule.onNodeWithContentDescription(text(R.string.home_menu_description)).performClick()
        composeRule.onNodeWithText(text(R.string.home_menu_start_another_session)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.home_menu_description)).performClick()
        composeRule.onNodeWithText(text(R.string.home_menu_archived)).performClick()

        assertEquals(listOf("start-another", "archived"), calls)
    }

    // SF02-A15 / SF02-R07 (S40-A08): without a session of today "Empezar otra sesión" is not offered
    @Test
    fun homeMenu_withoutASession_doesNotOfferStartAnother() {
        show(todayState.copy(todaySession = null, todayCards = emptyList(), canStartAnotherSession = false))

        composeRule.onNodeWithContentDescription(text(R.string.home_menu_description)).performClick()

        composeRule.onNodeWithText(text(R.string.home_menu_archived)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_menu_start_another_session)).assertDoesNotExist()
    }

    // SF02-A15 / SF02-R10, S42-R06 (S42-A03): back in Ejercicios returns to Hoy
    @Test
    fun back_inExercises_returnsToToday() {
        show(exercisesState)

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("tab:TODAY"), calls)
    }

    // SF02-A15 / SF02-R10, S42-R06: back in Hoy is not intercepted, it goes to the shell
    @Test
    fun back_inToday_staysWithTheShell() {
        show(todayState)

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf("shell-back"), calls)
    }

    // SF02-A15 / S42-R25 (S42-A21): no empty state before the first data
    @Test
    fun loading_showsNoEmptyState() {
        show(HomeUiState(today = TODAY, isLoading = true))

        composeRule.onNodeWithText(text(R.string.home_catalog_empty)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.home_today_empty)).assertDoesNotExist()
    }

    // SF02-A15 / SF02-R08, S40-R12 (S40-A12): an empty catalog offers to create an exercise
    @Test
    fun emptyCatalog_showsTheEmptyStateWithItsAction() {
        show(HomeUiState(isLoading = false, today = TODAY, catalogIsEmpty = true))

        composeRule.onNodeWithText(text(R.string.home_catalog_empty)).assertIsDisplayed()
        // Two ways to create the first exercise: the action of the empty state (its text) and the floating button.
        createFab().assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.home_create_exercise)).assertCountEquals(1)
        composeRule.onNodeWithText(text(R.string.home_create_exercise)).performClick()

        assertEquals(listOf("create::true"), calls)
    }

    // SF02-A15 / SF02-R11 (SF02-A06, S42-A07): the archived notice shows "Deshacer", which undoes it
    @Test
    fun notice_archived_offersUndo() {
        show(todayState.copy(notice = HomeNotice.Archived(exerciseId = 1L, name = "Press de banca", wasDeleteRequest = false)))

        composeRule.onNodeWithText(text(R.string.catalog_notice_archived, "Press de banca")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.catalog_notice_undo)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("undo:1", "notice-handled"), calls)
    }

    // SF02-A15 / SF02-R11 (S40-F06): a delete that archived says so
    @Test
    fun notice_archivedByADeleteRequest_saysItHasSets() {
        show(todayState.copy(notice = HomeNotice.Archived(1L, "Press de banca", wasDeleteRequest = true)))

        composeRule.onNodeWithText(text(R.string.catalog_notice_archived_has_sets, "Press de banca")).assertIsDisplayed()
    }

    // SF02-A15 / SF02-R11: a deleted notice has no action
    @Test
    fun notice_deleted_hasNoUndo() {
        show(todayState.copy(notice = HomeNotice.Deleted("Remo con mancuerna")))

        composeRule.onNodeWithText(text(R.string.catalog_notice_deleted, "Remo con mancuerna")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.catalog_notice_undo)).assertDoesNotExist()
    }

    // SF02-A21 / SF02-R23, S42-A23, S41-A11: at 200 % the main action and a card of Hoy are visible and tappable
    @Test
    fun fontScale200_inToday_createButtonAndCardAreDisplayedAndClickable() {
        show(todayState, fontScale = 2f)

        createFab().assertIsDisplayed().performClick()
        card("Press de banca").assertIsDisplayed().performClick()

        assertEquals(listOf("create::true", "logger:1"), calls)
    }

    private companion object {
        /** A Sunday, as in the examples of Spec 42. */
        val TODAY: LocalDate = LocalDate.of(2026, 10, 11)

        /** A Thursday. */
        val LAST_SESSION: LocalDate = LocalDate.of(2026, 10, 8)
    }
}

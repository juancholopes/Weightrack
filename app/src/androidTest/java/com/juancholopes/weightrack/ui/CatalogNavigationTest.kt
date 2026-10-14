package com.juancholopes.weightrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.MainActivity
import com.juancholopes.weightrack.R
import org.junit.Rule
import org.junit.Test

/**
 * Navigation from the home into the exercise editor, in the real app (SF01-R07, S42-R07). The tests only
 * walk paths that write nothing: the home reads the catalog, as the smoke test already does, and a new editor
 * reads nothing until it saves (S31-R18). Saving, archiving and deleting results are
 * covered on the JVM by `EditorResultsTest`; the archived list is not opened here because it reads the
 * database.
 */
class CatalogNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    /** The floating "Crear ejercicio", always on the home: Material 3 names it by its icon's description, not by text. */
    private fun openEditorFromHome() {
        composeRule.onNodeWithContentDescription(text(R.string.home_create_exercise)).performClick()
        composeRule.waitForIdle()
    }

    // SF01-A14 / SF01-R07, SF02-R02
    @Test
    fun createExercise_fromHome_opensTheNewExerciseEditor() {
        composeRule.onNodeWithText(text(R.string.home_tab_today)).assertIsDisplayed()

        openEditorFromHome()

        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_tab_today)).assertDoesNotExist()
    }

    // SF01-A14 / S42-A06 (the stack is saved with its keys, so it comes back after a recreation)
    @Test
    fun recreate_withTheEditorOpen_keepsTheEditorOnTopOfHome() {
        openEditorFromHome()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertIsDisplayed()

        // The home is still below the editor: back returns to it.
        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.home_tab_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertDoesNotExist()
    }

    // SF01-A14 / SF01-R07, S42-R06 (an editor with no changes goes back without asking)
    @Test
    fun back_fromTheNewEditorWithoutChanges_returnsToHome() {
        openEditorFromHome()

        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.home_tab_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.editor_title_new)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.editor_discard_dialog_title)).assertDoesNotExist()
    }
}

package com.juancholopes.weightrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** The only end-to-end test of the first version (S31-R17), adapted by F00 to its start screen. */
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // S31-A06 / S31-R17, SF00-A05 / SF00-R07
    @Test
    fun launch_showsAppName() {
        val activity = composeRule.activity

        composeRule.onNodeWithText(activity.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.start_message)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.start_create_exercise)).assertDoesNotExist()
        composeRule.onNodeWithText(activity.getString(R.string.start_open_archived)).assertDoesNotExist()
    }
}

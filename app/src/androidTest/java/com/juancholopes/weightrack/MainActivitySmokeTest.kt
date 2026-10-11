package com.juancholopes.weightrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** The only end-to-end test of the first version (S31-R17). F00 adapts it to its start screen. */
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // S31-A06 / S31-R17
    @Test
    fun launch_showsAppName() {
        val appName = composeRule.activity.getString(R.string.app_name)

        composeRule.onNodeWithText(appName).assertIsDisplayed()
    }
}

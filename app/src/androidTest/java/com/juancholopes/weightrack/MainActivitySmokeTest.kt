package com.juancholopes.weightrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * The only end-to-end test of the first version (S31-R17), adapted by F02 to its root: the app opens on the home,
 * in the Hoy tab (SF02-R02, S42-A02). It reads the real database, so it only checks what the home shows whatever
 * the catalog holds: the name of the app, the two tabs and the main action, the floating "Crear ejercicio", which
 * Material 3 exposes by the description of its icon and not as text.
 */
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // S31-A06 / S31-R17, SF02-A18 / SF02-R02 (S42-A02): the app opens on the home, on Hoy
    @Test
    fun launch_opensTheHomeOnToday() {
        val activity = composeRule.activity

        composeRule.onNodeWithText(activity.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.home_tab_today)).assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText(activity.getString(R.string.home_tab_exercises)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(activity.getString(R.string.home_create_exercise)).assertIsDisplayed()
    }
}

package com.juancholopes.weightrack.ui

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.test.espresso.Espresso
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.testing.FakeExerciseRepository
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** A destination that only exists in this test. */
@Serializable
private data object TestKey : NavKey

/**
 * Back navigation of the shell (S31-R16). The tests build [WeightrackNavDisplay] with their own entries and
 * a start screen backed by a fake repository, so they never open the app database (S31-R18).
 */
class ShellNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var backStack: NavBackStack<NavKey>

    private fun setShell(vararg keys: NavKey) {
        composeRule.setContent {
            WeightrackTheme {
                backStack = rememberNavBackStack(*keys)
                WeightrackNavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider {
                        entry<StartKey> { StartScreen(viewModel = viewModel(factory = fakeStartFactory)) }
                        entry<TestKey> { Text(TEST_SCREEN_TEXT) }
                    },
                )
            }
        }
    }

    // SF00-A06 / SF00-R04, SF00-R09
    @Test
    fun back_withEntryAboveStart_popsToStart() {
        setShell(StartKey, TestKey)
        composeRule.onNodeWithText(TEST_SCREEN_TEXT).assertIsDisplayed()

        Espresso.pressBack()
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(StartKey), backStack.toList())
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.start_message)).assertIsDisplayed()
    }

    // SF00-A07 / SF00-R04, S42-A03
    @Test
    fun back_onStart_leavesForeground() {
        setShell(StartKey)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.start_message)).assertIsDisplayed()

        Espresso.pressBackUnconditionally()

        // D3: from API 31 a root launcher activity goes to the background instead of finishing.
        val state = awaitLeavingForeground()
        assertTrue("Activity is still in the foreground: $state", state in LEFT_FOREGROUND)
        assertEquals(listOf<NavKey>(StartKey), backStack.toList())
    }

    /** The activity stops or finishes after the back press returns, so its state is polled for a while. */
    private fun awaitLeavingForeground(): Lifecycle.State {
        val deadline = SystemClock.uptimeMillis() + STATE_TIMEOUT_MILLIS
        var state = composeRule.activityRule.scenario.state
        while (state !in LEFT_FOREGROUND && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(POLL_MILLIS)
            state = composeRule.activityRule.scenario.state
        }
        return state
    }

    private companion object {
        const val TEST_SCREEN_TEXT = "Test screen"
        const val STATE_TIMEOUT_MILLIS = 5_000L
        const val POLL_MILLIS = 50L
        val LEFT_FOREGROUND = setOf(Lifecycle.State.CREATED, Lifecycle.State.DESTROYED)

        val fakeStartFactory = viewModelFactory {
            initializer { StartViewModel(FakeExerciseRepository()) }
        }
    }
}

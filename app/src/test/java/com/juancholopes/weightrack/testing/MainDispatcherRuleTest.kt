package com.juancholopes.weightrack.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainDispatcherRuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // S31-A05 / S31-R10: without the rule, Dispatchers.Main fails on the JVM (there is no Looper)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun coroutineLaunchedOnMain_runsOnTheTestScheduler() = runTest {
        // The same context viewModelScope uses.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var state = "loading"

        scope.launch {
            delay(5_000)
            state = "loaded"
        }
        assertEquals("loading", state)
        advanceUntilIdle()

        assertEquals("loaded", state)
        scope.cancel()
    }
}

package com.juancholopes.weightrack.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.juancholopes.weightrack.feature.session.ui.HomeKey

/**
 * Root of the shell (SF00-R04). It owns the stack and registers every feature with one bridge line
 * (SF00-R08): F01 adds `catalogDestinations(backStack, ...)` and F02 `sessionDestinations(backStack)`.
 */
@Composable
fun WeightrackApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeKey)
    // Saveable state, not an event (S42-R09, D6): the home takes it when it is on top and tells the shell it is consumed.
    val pendingNotice = rememberSaveable { mutableStateOf<PendingNotice?>(null) }
    WeightrackNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            sessionDestinations(
                backStack,
                notice = { pendingNotice.value },
                onNoticeConsumed = { pendingNotice.value = null },
            )
            catalogDestinations(backStack, onNotice = { pendingNotice.value = it })
        },
        modifier = modifier,
    )
}

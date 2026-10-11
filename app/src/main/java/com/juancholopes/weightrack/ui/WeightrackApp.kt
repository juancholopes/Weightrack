package com.juancholopes.weightrack.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Root of the shell (SF00-R04). It owns the stack and registers every feature with one bridge line
 * (SF00-R08): F01 adds `catalogDestinations(backStack)` and F02 `sessionDestinations(backStack)`.
 */
@Composable
fun WeightrackApp(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(StartKey)
    WeightrackNavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            startDestination(backStack)
        },
        modifier = modifier,
    )
}

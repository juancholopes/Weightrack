package com.juancholopes.weightrack.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import com.juancholopes.weightrack.ui.theme.WeightrackTheme

/**
 * The single stack of the app on screen (SF00-R04, SF00-R09, S30-R37). Back removes the top entry; with one
 * entry left, the system takes back and the app leaves the foreground. Each entry keeps its saveable
 * state and its own ViewModels, which are cleared when it leaves the stack.
 *
 * Nothing wraps it and it adds no insets: every screen brings its own bars and insets (S42-R10).
 */
@Composable
fun WeightrackNavDisplay(
    backStack: NavBackStack<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    val motion = WeightrackTheme.motion
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { forwardTransition(motion) },
        popTransitionSpec = { popTransition(motion) },
        predictivePopTransitionSpec = if (motion.reducedMotion) {
            { _ -> reducedMotionTransition(motion) }
        } else {
            defaultPredictivePopTransitionSpec()
        },
        entryProvider = entryProvider,
    )
}

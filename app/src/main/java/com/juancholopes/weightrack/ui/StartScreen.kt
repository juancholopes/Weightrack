package com.juancholopes.weightrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import kotlinx.serialization.Serializable

/** Root of the stack until F02 replaces it with `HomeKey` and deletes this file (Spec 42). */
@Serializable
data object StartKey : NavKey

/**
 * Bridge of the start screen (SF00-R08). F01 passes `onCreateExercise` and `onOpenArchived` here as
 * operations on [backStack]. F00 has nowhere to go yet, so both stay `null`.
 */
fun EntryProviderScope<NavKey>.startDestination(backStack: NavBackStack<NavKey>) {
    entry<StartKey> { StartScreen() }
}

/**
 * Temporary start screen (SF00-R07). It reads its state from [StartViewModel] (SF00-R16). With a `null`
 * callback, its button is not shown.
 */
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    onCreateExercise: (() -> Unit)? = null,
    onOpenArchived: (() -> Unit)? = null,
    viewModel: StartViewModel = viewModel(factory = StartViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StartContent(
        uiState = uiState,
        onCreateExercise = onCreateExercise,
        onOpenArchived = onOpenArchived,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartContent(
    uiState: StartUiState,
    onCreateExercise: (() -> Unit)?,
    onOpenArchived: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Text(text = stringResource(R.string.start_message), style = MaterialTheme.typography.bodyLarge)
            if (uiState is StartUiState.Ready) {
                Text(
                    text = pluralStringResource(
                        R.plurals.start_active_exercises,
                        uiState.activeExerciseCount,
                        uiState.activeExerciseCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            onCreateExercise?.let { onClick ->
                Button(onClick = onClick) { Text(stringResource(R.string.start_create_exercise)) }
            }
            onOpenArchived?.let { onClick ->
                OutlinedButton(onClick = onClick) { Text(stringResource(R.string.start_open_archived)) }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun StartContentPreview() {
    WeightrackTheme {
        StartContent(
            uiState = StartUiState.Ready(activeExerciseCount = 3),
            onCreateExercise = {},
            onOpenArchived = {},
        )
    }
}

package com.juancholopes.weightrack.feature.catalog.ui.archived

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.EmptyState

/**
 * Archived exercises, S42-P04 (SF01-R10): a list by name with "Desarchivar" in each row. Stateless: it draws
 * [uiState] and reports events through its lambdas (S30-R31).
 *
 * It shows its empty state only when `!isLoading && items.isEmpty()`, never before the first data (S42-R25).
 * The notice of an unarchived exercise and the failure message are snackbars of this screen's own host
 * (S40-R20, S42-R10); each is reported as handled once it has been shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedExercisesContent(
    uiState: ArchivedExercisesUiState,
    onUnarchiveClick: (Long) -> Unit,
    onNoticeHandled: () -> Unit,
    onGenericErrorHandled: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val noticeMessage = uiState.notice?.let { stringResource(R.string.archived_notice_returned, it) }
    val currentOnNoticeHandled by rememberUpdatedState(onNoticeHandled)
    LaunchedEffect(noticeMessage) {
        if (noticeMessage != null) {
            snackbarHostState.showSnackbar(message = noticeMessage, duration = SnackbarDuration.Short)
            currentOnNoticeHandled()
        }
    }

    val genericMessage = stringResource(R.string.archived_error_generic)
    val currentOnGenericErrorHandled by rememberUpdatedState(onGenericErrorHandled)
    LaunchedEffect(uiState.genericError) {
        if (uiState.genericError) {
            snackbarHostState.showSnackbar(message = genericMessage, duration = SnackbarDuration.Short)
            currentOnGenericErrorHandled()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.archived_back_description),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            uiState.items.isEmpty() -> EmptyState(
                text = stringResource(R.string.archived_empty),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
                items(items = uiState.items, key = { it.id }) { exercise ->
                    ArchivedRow(exercise = exercise, onUnarchiveClick = { onUnarchiveClick(exercise.id) })
                }
            }
        }
    }
}

/** One archived exercise. The button names the exercise for TalkBack, since every row says "Desarchivar". */
@Composable
private fun ArchivedRow(exercise: Exercise, onUnarchiveClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.archived_unarchive_description, exercise.name)
    ListItem(
        headlineContent = { Text(exercise.name) },
        modifier = modifier,
        trailingContent = {
            TextButton(
                onClick = onUnarchiveClick,
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                Text(stringResource(R.string.archived_unarchive))
            }
        },
    )
}

@ThemePreviews
@Composable
private fun ArchivedExercisesContentListPreview() {
    WeightrackTheme {
        ArchivedExercisesContent(
            uiState = ArchivedExercisesUiState(
                isLoading = false,
                items = listOf(
                    Exercise(id = 3L, name = "Press de banca", equipment = Equipment.BARBELL, notes = null, archived = true),
                    Exercise(id = 8L, name = "Remo con mancuerna", equipment = Equipment.DUMBBELL, notes = null, archived = true),
                ),
            ),
            onUnarchiveClick = {},
            onNoticeHandled = {},
            onGenericErrorHandled = {},
            onBack = {},
        )
    }
}

@ThemePreviews
@Composable
private fun ArchivedExercisesContentEmptyPreview() {
    WeightrackTheme {
        ArchivedExercisesContent(
            uiState = ArchivedExercisesUiState(isLoading = false, items = emptyList()),
            onUnarchiveClick = {},
            onNoticeHandled = {},
            onGenericErrorHandled = {},
            onBack = {},
        )
    }
}

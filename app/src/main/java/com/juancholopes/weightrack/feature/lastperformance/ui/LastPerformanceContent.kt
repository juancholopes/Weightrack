package com.juancholopes.weightrack.feature.lastperformance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.formatSet
import com.juancholopes.weightrack.ui.theme.formatShortDate
import com.juancholopes.weightrack.ui.theme.rememberDateNames
import com.juancholopes.weightrack.ui.theme.spokenDate
import com.juancholopes.weightrack.ui.theme.spokenSet
import java.time.LocalDate

/** Between the sets of the line "60 kg × 10 · 60 × 10 · 60 × 8", and between the sets of its reading. */
private const val SETS_SEPARATOR = " · "
private const val SPOKEN_SETS_SEPARATOR = "; "

/** A no-break space: a line of text that draws nothing but has the height of its style. */
private const val BLANK_LINE = " "

private const val LOADING_HEADER_WIDTH = 0.35f
private const val LOADING_SETS_WIDTH = 0.7f

/**
 * The "Última vez" of the set logger, S42-P02 (SF03-R05 to SF03-R08): the date of the session and its sets in order
 * ("60 kg × 10 · 60 × 10 · 60 × 8"), the first-time text, or the failure text. Stateless: it draws [uiState] and, only
 * when [onOpenProgress] is not `null`, adds "Ver estadísticas" (SF03-R07, S42-R24). It is read as one heading node
 * (S40-R49). While loading it reserves the height of a header line and a sets line, so the panel does not jump.
 */
@Composable
fun LastPerformanceContent(
    uiState: LastPerformanceUiState,
    onOpenProgress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.xs)) {
        when (uiState) {
            LastPerformanceUiState.Loading -> LoadingLines()
            LastPerformanceUiState.FirstTime -> Text(
                text = stringResource(R.string.last_performance_first_time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is LastPerformanceUiState.Shown -> ShownLines(uiState)
            LastPerformanceUiState.Error -> Text(
                text = stringResource(R.string.last_performance_error),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        // Material 3 buttons already measure at least 48 dp where they are touched (S40-R46).
        onOpenProgress?.let { open ->
            TextButton(onClick = open) { Text(stringResource(R.string.last_performance_view_stats)) }
        }
    }
}

/**
 * The header and the line of sets as a single node for TalkBack: a heading that reads the date and every set with its
 * unit, "Última vez, jueves 8 de octubre: 60 kilogramos, 10 repeticiones; ..." (SF03-R06, S42-P02).
 */
@Composable
private fun ShownLines(state: LastPerformanceUiState.Shown) {
    val names = rememberDateNames()
    val header = stringResource(R.string.last_performance_header, formatShortDate(state.date, state.today, names))
    val spoken = stringResource(
        R.string.last_performance_spoken,
        spokenDate(state.date, state.today, names),
        state.sets.joinToString(SPOKEN_SETS_SEPARATOR) { spokenSet(it.weightGrams, it.reps) },
    )
    Column(
        modifier = Modifier.clearAndSetSemantics {
            heading()
            contentDescription = spoken
        },
    ) {
        Text(text = header, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = formatSets(state.sets),
            style = WeightrackTheme.numbers.inline,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The weight with its unit in the first set only, as the list of sets of one exercise allows (Spec 42 "Formatos"). */
private fun formatSets(sets: List<SetValues>): String =
    sets.mapIndexed { index, set -> formatSet(set.weightGrams, set.reps, withUnit = index == 0) }
        .joinToString(SETS_SEPARATOR)

/**
 * Two blank lines with the styles of the header and of the sets, so they are as tall as [ShownLines] and the panel
 * does not jump when the data arrives (SF03-R05). They have no semantics: there is nothing to read yet.
 */
@Composable
private fun LoadingLines() {
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = MaterialTheme.shapes.small
    Column(modifier = Modifier.clearAndSetSemantics {}) {
        Text(
            text = BLANK_LINE,
            modifier = Modifier.fillMaxWidth(LOADING_HEADER_WIDTH).background(color, shape),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = BLANK_LINE,
            modifier = Modifier.fillMaxWidth(LOADING_SETS_WIDTH).background(color, shape),
            style = WeightrackTheme.numbers.inline,
        )
    }
}

@ThemePreviews
@Composable
private fun LastPerformanceContentPreview() {
    val today = LocalDate.of(2026, 10, 11)
    WeightrackTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(WeightrackTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(WeightrackTheme.spacing.xl),
            ) {
                LastPerformanceContent(
                    uiState = LastPerformanceUiState.Shown(
                        date = LocalDate.of(2026, 10, 8),
                        today = today,
                        sets = listOf(SetValues(60_000, 10), SetValues(60_000, 10), SetValues(60_000, 8)),
                    ),
                    onOpenProgress = null,
                )
                LastPerformanceContent(
                    uiState = LastPerformanceUiState.Shown(
                        date = LocalDate.of(2026, 10, 10),
                        today = today,
                        sets = listOf(SetValues(62_500, 8), SetValues(65_000, 6), SetValues(65_000, 5), SetValues(67_500, 3)),
                    ),
                    onOpenProgress = {},
                )
                LastPerformanceContent(uiState = LastPerformanceUiState.FirstTime, onOpenProgress = null)
                LastPerformanceContent(uiState = LastPerformanceUiState.Error, onOpenProgress = null)
                LastPerformanceContent(uiState = LastPerformanceUiState.Loading, onOpenProgress = null)
            }
        }
    }
}

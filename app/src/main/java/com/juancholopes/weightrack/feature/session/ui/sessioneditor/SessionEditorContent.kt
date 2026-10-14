package com.juancholopes.weightrack.feature.session.ui.sessioneditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.session.domain.WorkoutSessionError
import com.juancholopes.weightrack.feature.session.ui.messageRes
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.ConfirmDialog
import com.juancholopes.weightrack.ui.theme.formatShortDate
import com.juancholopes.weightrack.ui.theme.rememberDateNames
import com.juancholopes.weightrack.ui.theme.spokenDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Form of the data of a session, S42-P05 (SF02-R19): a date field that opens a `DatePickerDialog` with the days
 * after today disabled (S10-R32), a note and "Guardar sesión". Stateless: it draws [uiState] and reports every
 * event through its lambdas (S30-R31).
 *
 * It owns the snackbar of an unexpected failure. Back is handled by the one handler below, enabled only with
 * unsaved changes (S42-R06): the discard dialog is decided by `uiState.showDiscardDialog`; without changes, back and
 * predictive back stay with the shell. The keyboard is applied with a single method, `fitInside` on the IME
 * rulers (skill `edge-to-edge`), and the save button stays pinned below the scrolling form.
 *
 * @param onClose leave the screen without saving: the close button without changes, or "Descartar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorContent(
    uiState: SessionEditorUiState,
    onDateClick: () -> Unit,
    onDatePickerDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDiscardRequest: () -> Unit,
    onDialogDismiss: () -> Unit,
    onGenericErrorHandled: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    val genericMessage = stringResource(R.string.session_error_generic)
    val currentOnGenericErrorHandled by rememberUpdatedState(onGenericErrorHandled)
    LaunchedEffect(uiState.genericError) {
        if (uiState.genericError) {
            snackbarHostState.showSnackbar(message = genericMessage, duration = SnackbarDuration.Short)
            currentOnGenericErrorHandled()
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = uiState.hasUnsavedChanges,
        onBackCompleted = onDiscardRequest,
    )
    val onCloseRequest = if (uiState.hasUnsavedChanges) onDiscardRequest else onClose

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onCloseRequest) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.session_editor_close_description),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fitInside(WindowInsetsRulers.Ime.current),
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.l, vertical = spacing.s),
                    verticalArrangement = Arrangement.spacedBy(spacing.l),
                ) {
                    DateField(uiState = uiState, onClick = onDateClick)
                    NotesField(uiState = uiState, onNotesChange = onNotesChange)
                }
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.l, vertical = spacing.m),
                ) {
                    Text(stringResource(R.string.session_editor_save))
                }
            }
        }
    }

    if (uiState.showDatePicker) {
        SessionDatePicker(
            date = uiState.date,
            today = uiState.today,
            onSelected = onDateSelected,
            onDismiss = onDatePickerDismiss,
        )
    }
    if (uiState.showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.session_discard_dialog_title),
            text = stringResource(R.string.session_discard_dialog_text),
            confirmLabel = stringResource(R.string.session_discard_dialog_confirm),
            dismissLabel = stringResource(R.string.session_discard_dialog_dismiss),
            // Close the dialog first so it does not linger over the screen while it leaves.
            onConfirm = {
                onDialogDismiss()
                onClose()
            },
            onDismiss = onDialogDismiss,
            isDestructive = true,
        )
    }
}

/**
 * The date as a field that looks like the others and opens the picker. It is one node for TalkBack: "Fecha de la
 * sesión, domingo 11 de octubre", with the action "Elegir fecha". Its error goes right under it (S40-R05).
 */
@Composable
private fun DateField(uiState: SessionEditorUiState, onClick: () -> Unit) {
    val spacing = WeightrackTheme.spacing
    val names = rememberDateNames()
    val description = stringResource(
        R.string.session_editor_date_description,
        spokenDate(uiState.date, uiState.today, names),
    )
    val clickLabel = stringResource(R.string.session_editor_date_click_label)
    val message = uiState.fieldError?.takeIf { it == WorkoutSessionError.DATE_IN_FUTURE }?.let { stringResource(it.messageRes()) }
    val borderColor = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    message?.let { error(it) }
                },
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = FieldMinHeight)
                    .padding(horizontal = spacing.l, vertical = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.session_editor_date_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatShortDate(uiState.date, uiState.today, names, relative = false),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Icon(painter = painterResource(R.drawable.ic_calendar_today), contentDescription = null)
            }
        }
        message?.let {
            Text(
                text = it,
                modifier = Modifier
                    .padding(horizontal = spacing.l)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NotesField(uiState: SessionEditorUiState, onNotesChange: (String) -> Unit) {
    val message = uiState.fieldError?.takeIf { it == WorkoutSessionError.NOTES_TOO_LONG }?.let { stringResource(it.messageRes()) }
    OutlinedTextField(
        value = uiState.notes,
        onValueChange = onNotesChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { message?.let { error(it) } },
        label = { Text(stringResource(R.string.session_editor_notes_label)) },
        isError = message != null,
        supportingText = message?.let { text ->
            { Text(text = text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        },
        minLines = 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

/**
 * The date picker with every day after [today] disabled (`selectableDates`, S10-R32). The picker works in UTC
 * milliseconds at the start of the day, so the conversions use UTC and never the zone of the phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDatePicker(
    date: LocalDate,
    today: LocalDate,
    onSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectableDates = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toUtcDate() <= today

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = pickerState.selectedDateMillis?.toUtcDate()
                    if (chosen != null) onSelected(chosen) else onDismiss()
                },
            ) {
                Text(stringResource(R.string.session_editor_date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.session_editor_date_picker_dismiss)) }
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = stringResource(R.string.session_editor_date_picker_title),
                    modifier = Modifier.padding(start = WeightrackTheme.spacing.xl, top = WeightrackTheme.spacing.l),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            showModeToggle = false,
        )
    }
}

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val FieldMinHeight = 56.dp

/**
 * A preview has no activity, so the back handler gets a root dispatcher of its own; in the app the activity
 * provides it.
 */
@Composable
private fun SessionEditorPreview(uiState: SessionEditorUiState) {
    WeightrackTheme {
        val owner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            SessionEditorContent(
                uiState = uiState,
                onDateClick = {},
                onDatePickerDismiss = {},
                onDateSelected = {},
                onNotesChange = {},
                onSaveClick = {},
                onDiscardRequest = {},
                onDialogDismiss = {},
                onGenericErrorHandled = {},
                onClose = {},
            )
        }
    }
}

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 11)

@ThemePreviews
@Composable
private fun SessionEditorContentPreview() {
    SessionEditorPreview(
        SessionEditorUiState(
            isLoading = false,
            date = PreviewToday,
            today = PreviewToday,
            notes = "Me sentí con energía",
        ),
    )
}

@ThemePreviews
@Composable
private fun SessionEditorContentFutureDatePreview() {
    SessionEditorPreview(
        SessionEditorUiState(
            isLoading = false,
            date = PreviewToday.plusDays(1),
            today = PreviewToday,
            fieldError = WorkoutSessionError.DATE_IN_FUTURE,
            hasUnsavedChanges = true,
        ),
    )
}

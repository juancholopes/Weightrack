package com.juancholopes.weightrack.feature.catalog.ui.editor

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseError
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.ui.labelRes
import com.juancholopes.weightrack.feature.catalog.ui.messageRes
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.ConfirmDialog

/** The fields (or groups) of the form that an [ExerciseError] can belong to; each has one place for its message. */
private enum class EditorField { NAME, EQUIPMENT, MUSCLES, NOTES }

private fun ExerciseError.field(): EditorField = when (this) {
    ExerciseError.NAME_BLANK,
    ExerciseError.NAME_TOO_LONG,
    ExerciseError.NAME_TAKEN,
    ExerciseError.NAME_TAKEN_BY_ARCHIVED,
    -> EditorField.NAME
    ExerciseError.EQUIPMENT_MISSING -> EditorField.EQUIPMENT
    ExerciseError.MUSCLES_MISSING,
    ExerciseError.PRIMARY_MUSCLE_MISSING,
    ExerciseError.MUSCLE_REPEATED,
    -> EditorField.MUSCLES
    ExerciseError.NOTES_TOO_LONG -> EditorField.NOTES
}

/** Where focus lands for each field; a group takes it on its first chip (SF01-R04, S40-R47). */
private class FieldFocus {
    private val name = FocusRequester()
    private val equipment = FocusRequester()
    private val muscles = FocusRequester()
    private val notes = FocusRequester()

    fun of(field: EditorField): FocusRequester = when (field) {
        EditorField.NAME -> name
        EditorField.EQUIPMENT -> equipment
        EditorField.MUSCLES -> muscles
        EditorField.NOTES -> notes
    }
}

/**
 * Create and edit form of an exercise, S42-P03 (SF01-R02 to SF01-R09, SF01-R14, SF01-R15). Stateless: it
 * draws [uiState] and reports every event through its lambdas (S30-R31).
 *
 * It owns the effects that only touch this screen: focus on the field in error (S40-R47) and the snackbar
 * of an unexpected failure (S41 patterns). Navigation effects belong to `ExerciseEditorScreen`.
 *
 * Back is handled by the one handler below, enabled only with unsaved changes (S42-R06); without them,
 * back and predictive back stay with the shell. The keyboard is applied with a single method, `fitInside`
 * on the IME rulers (skill `edge-to-edge`), and Save stays pinned below the scrolling form, so it is
 * visible with the keyboard open and at 200 % font.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorContent(
    uiState: ExerciseEditorUiState,
    onNameChange: (String) -> Unit,
    onEquipmentSelect: (Equipment) -> Unit,
    onMuscleToggle: (Muscle) -> Unit,
    onPrimaryChange: (Muscle, Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onUnarchiveConflictClick: () -> Unit,
    onUseOtherNameClick: () -> Unit,
    onDiscardRequest: () -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorFocusHandled: () -> Unit,
    onGenericErrorHandled: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val focus = remember { FieldFocus() }

    val genericMessage = stringResource(R.string.catalog_error_generic)
    val currentOnGenericErrorHandled by rememberUpdatedState(onGenericErrorHandled)
    LaunchedEffect(uiState.genericError) {
        if (uiState.genericError) {
            snackbarHostState.showSnackbar(message = genericMessage, duration = SnackbarDuration.Short)
            currentOnGenericErrorHandled()
        }
    }

    val currentOnErrorFocusHandled by rememberUpdatedState(onErrorFocusHandled)
    LaunchedEffect(uiState.focusOnError) {
        if (uiState.focusOnError) {
            uiState.fieldError?.let { focus.of(it.field()).requestFocus() }
            currentOnErrorFocusHandled()
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
                title = {
                    val title = if (uiState.isNew) R.string.editor_title_new else R.string.editor_title_edit
                    Text(stringResource(title))
                },
                navigationIcon = {
                    IconButton(onClick = onCloseRequest) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.editor_close_description),
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
                EditorForm(
                    uiState = uiState,
                    focus = focus,
                    onNameChange = onNameChange,
                    onEquipmentSelect = onEquipmentSelect,
                    onMuscleToggle = onMuscleToggle,
                    onPrimaryChange = onPrimaryChange,
                    onNotesChange = onNotesChange,
                    onArchiveClick = onArchiveClick,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.l, vertical = spacing.m),
                ) {
                    Text(stringResource(R.string.editor_save))
                }
            }
        }
    }

    EditorDialogs(
        uiState = uiState,
        onDeleteConfirm = onDeleteConfirm,
        onUnarchiveConflictClick = onUnarchiveConflictClick,
        onUseOtherNameClick = onUseOtherNameClick,
        onDialogDismiss = onDialogDismiss,
        onClose = onClose,
    )
}

/** The scrolling part of the editor: name, equipment, muscles, note and the one archive-or-delete action. */
@Composable
private fun EditorForm(
    uiState: ExerciseEditorUiState,
    focus: FieldFocus,
    onNameChange: (String) -> Unit,
    onEquipmentSelect: (Equipment) -> Unit,
    onMuscleToggle: (Muscle) -> Unit,
    onPrimaryChange: (Muscle, Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    // Only one error exists at a time (D18); it is shown where its field is.
    val error = uiState.fieldError
    val nameMessage = error?.takeIf { it.field() == EditorField.NAME }?.let { stringResource(it.messageRes()) }
    val equipmentMessage = error?.takeIf { it.field() == EditorField.EQUIPMENT }?.let { stringResource(it.messageRes()) }
    val musclesMessage = error?.takeIf { it.field() == EditorField.MUSCLES }?.let { stringResource(it.messageRes()) }
    val notesMessage = error?.takeIf { it.field() == EditorField.NOTES }?.let { stringResource(it.messageRes()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.l, vertical = spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus.of(EditorField.NAME))
                .errorSemantics(nameMessage),
            label = { Text(stringResource(R.string.editor_name_label)) },
            isError = nameMessage != null,
            supportingText = nameMessage?.let { message -> { ErrorText(message) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SectionLabel(stringResource(R.string.editor_equipment_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                Equipment.entries.forEachIndexed { index, equipment ->
                    FilterChip(
                        selected = uiState.equipment == equipment,
                        onClick = { onEquipmentSelect(equipment) },
                        label = { Text(stringResource(equipment.labelRes())) },
                        modifier = if (index == 0) Modifier.groupFocus(focus.of(EditorField.EQUIPMENT)) else Modifier,
                    )
                }
            }
            equipmentMessage?.let { GroupError(it) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SectionLabel(stringResource(R.string.editor_muscles_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                Muscle.entries.forEachIndexed { index, muscle ->
                    FilterChip(
                        selected = uiState.muscles.any { it.muscle == muscle },
                        onClick = { onMuscleToggle(muscle) },
                        label = { Text(stringResource(muscle.labelRes())) },
                        modifier = if (index == 0) Modifier.groupFocus(focus.of(EditorField.MUSCLES)) else Modifier,
                    )
                }
            }
            uiState.muscles.forEach { choice ->
                PrimaryRow(
                    muscle = choice.muscle,
                    isPrimary = choice.isPrimary,
                    onPrimaryChange = { onPrimaryChange(choice.muscle, it) },
                )
            }
            musclesMessage?.let { GroupError(it) }
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus.of(EditorField.NOTES))
                .errorSemantics(notesMessage),
            label = { Text(stringResource(R.string.editor_notes_label)) },
            isError = notesMessage != null,
            supportingText = notesMessage?.let { message -> { ErrorText(message) } },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        // One action by `hasSets`; none while it is unknown (SF01-R06). Archiving is reversible and acts at
        // once; deleting is not, so it asks first (the dialog is in EditorDialogs).
        when (uiState.hasSets) {
            true -> TextButton(onClick = onArchiveClick, enabled = !uiState.isSaving) {
                Text(stringResource(R.string.editor_archive))
            }
            false -> TextButton(
                onClick = onDeleteClick,
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.editor_delete))
            }
            null -> Unit
        }
    }
}

/**
 * A chosen muscle with its "Principal" switch. The whole row is the switch (48 dp or more), "Principal" is
 * written text and the description names the muscle, so the mark never depends on color (S40-R49).
 */
@Composable
private fun PrimaryRow(
    muscle: Muscle,
    isPrimary: Boolean,
    onPrimaryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val muscleLabel = stringResource(muscle.labelRes())
    val description = stringResource(R.string.editor_primary_switch_description, muscleLabel)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .toggleable(value = isPrimary, role = Role.Switch, onValueChange = onPrimaryChange)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        Text(text = muscleLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.editor_primary_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The row carries the click and the state; the switch only draws them.
        Switch(checked = isPrimary, onCheckedChange = null)
    }
}

/** The dialogs the editor opens, all decided by the `UiState` (S42-R25). Back or a tap outside dismisses them. */
@Composable
private fun EditorDialogs(
    uiState: ExerciseEditorUiState,
    onDeleteConfirm: () -> Unit,
    onUnarchiveConflictClick: () -> Unit,
    onUseOtherNameClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onClose: () -> Unit,
) {
    uiState.archivedConflict?.let { conflict ->
        ConfirmDialog(
            title = stringResource(R.string.editor_archived_dialog_title, conflict.name),
            text = stringResource(R.string.editor_archived_dialog_text),
            confirmLabel = stringResource(R.string.editor_archived_dialog_confirm),
            dismissLabel = stringResource(R.string.editor_archived_dialog_dismiss),
            onConfirm = onUnarchiveConflictClick,
            onDismiss = onUseOtherNameClick,
        )
    }
    when (val dialog = uiState.confirmDialog) {
        is EditorDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.editor_delete_dialog_title, dialog.exerciseName),
            text = stringResource(R.string.editor_delete_dialog_text),
            confirmLabel = stringResource(R.string.editor_delete_dialog_confirm),
            dismissLabel = stringResource(R.string.editor_delete_dialog_dismiss),
            onConfirm = onDeleteConfirm,
            onDismiss = onDialogDismiss,
            isDestructive = true,
        )
        is EditorDialog.Discard -> ConfirmDialog(
            title = stringResource(R.string.editor_discard_dialog_title),
            text = stringResource(R.string.editor_discard_dialog_text),
            confirmLabel = stringResource(R.string.editor_discard_dialog_confirm),
            dismissLabel = stringResource(R.string.editor_discard_dialog_dismiss),
            // Close the dialog first so it does not linger over the screen while it leaves.
            onConfirm = {
                onDialogDismiss()
                onClose()
            },
            onDismiss = onDialogDismiss,
            // Owner decision: losing what was typed is destructive, so it gets the error color too.
            isDestructive = true,
        )
        null -> Unit
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The message under a group (equipment, muscles): error semantics and a live region, never color alone. */
@Composable
private fun GroupError(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.semantics {
            error(message)
            liveRegion = LiveRegionMode.Polite
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/** The `supportingText` of a field in error: the live region announces it as it appears. */
@Composable
private fun ErrorText(message: String) {
    Text(text = message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

/**
 * Where focus lands for a group: its first chip. A chip is clickable, and clickables take focus only from a
 * keyboard or d-pad, so without `focusable` a request made after a touch would be ignored.
 */
private fun Modifier.groupFocus(requester: FocusRequester): Modifier = focusRequester(requester).focusable()

/** Puts the real message on the field; Material only sets a generic "invalid input" one (S40-R49). */
private fun Modifier.errorSemantics(message: String?): Modifier =
    if (message == null) this else semantics { error(message) }

private val MinTouchTarget = 48.dp

/**
 * A preview has no activity, so the back handler gets a root dispatcher of its own; in the app the
 * activity provides it.
 */
@Composable
private fun EditorPreview(uiState: ExerciseEditorUiState) {
    WeightrackTheme {
        val owner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            ExerciseEditorContent(
                uiState = uiState,
                onNameChange = {},
                onEquipmentSelect = {},
                onMuscleToggle = {},
                onPrimaryChange = { _, _ -> },
                onNotesChange = {},
                onSaveClick = {},
                onArchiveClick = {},
                onDeleteClick = {},
                onDeleteConfirm = {},
                onUnarchiveConflictClick = {},
                onUseOtherNameClick = {},
                onDiscardRequest = {},
                onDialogDismiss = {},
                onErrorFocusHandled = {},
                onGenericErrorHandled = {},
                onClose = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ExerciseEditorContentEditPreview() {
    EditorPreview(
        ExerciseEditorUiState(
            isLoading = false,
            isNew = false,
            name = "Press de banca",
            equipment = Equipment.BARBELL,
            muscles = listOf(
                MuscleChoice(Muscle.CHEST, isPrimary = true),
                MuscleChoice(Muscle.TRICEPS, isPrimary = false),
            ),
            notes = "Asiento en la posición 4",
            hasSets = true,
        ),
    )
}

@ThemePreviews
@Composable
private fun ExerciseEditorContentNewWithErrorPreview() {
    EditorPreview(
        ExerciseEditorUiState(
            isLoading = false,
            isNew = true,
            name = "Remo con mancuerna",
            muscles = listOf(MuscleChoice(Muscle.LATS, isPrimary = true)),
            fieldError = ExerciseError.EQUIPMENT_MISSING,
        ),
    )
}

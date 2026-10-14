package com.juancholopes.weightrack.feature.session.ui.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.session.ui.messageRes
import com.juancholopes.weightrack.ui.theme.DateNames
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WEIGHT_UNIT
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.ConfirmDialog
import com.juancholopes.weightrack.ui.theme.formatAdditionalWeight
import com.juancholopes.weightrack.ui.theme.formatKilograms
import com.juancholopes.weightrack.ui.theme.formatReps
import com.juancholopes.weightrack.ui.theme.formatSet
import com.juancholopes.weightrack.ui.theme.formatShortDate
import com.juancholopes.weightrack.ui.theme.rememberDateNames
import com.juancholopes.weightrack.ui.theme.spokenAdditionalWeight
import com.juancholopes.weightrack.ui.theme.spokenReps
import com.juancholopes.weightrack.ui.theme.spokenSet
import com.juancholopes.weightrack.ui.theme.spokenWeight
import java.time.LocalDate

/**
 * Set logger, S42-P02 (SF02-R13, SF02-R15, SF02-R16, SF02-R23): the sets of one exercise in one session, and a
 * panel at the bottom to add, correct and delete them. Stateless: it draws [uiState] and reports every event
 * through its lambdas (S30-R31).
 *
 * The top part scrolls and follows the focus order of S42-R15: the fixed note, the slot of the last time, the
 * list of this session and then the panel with the weight, the repetitions and "Guardar serie". The panel stays
 * at the bottom. When it does not fit, as with a very large font, the repetitions control moves into the
 * scrolling part and the save button stays visible (S42-R28).
 *
 * It owns the effects that only touch this screen: the live-region announcement of a saved set (S40-R27) and
 * the snackbar of an unexpected failure. Back is handled by the one handler below, enabled only with unsaved
 * changes (S42-R06, D13): the discard dialog is decided by `uiState.confirmDialog`.
 *
 * @param onBack leave the screen; the shell removes the entry.
 * @param onOpenProgress `null` until F05: with it, the menu of the top bar offers "Ver estadísticas".
 * @param lastPerformance the slot of F03 (S42-R16): it receives the exercise, the resolved session and the
 * modifier the screen gives it. This feature never imports the feature that fills it (S30-R10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLoggerContent(
    uiState: SetLoggerUiState,
    onWeightDecrease: () -> Unit,
    onWeightIncrease: () -> Unit,
    onWeightSlide: (Int) -> Unit,
    onWeightSlideFinished: () -> Unit,
    onWeightClick: () -> Unit,
    onRepsDecrease: () -> Unit,
    onRepsIncrease: () -> Unit,
    onRepsSlide: (Int) -> Unit,
    onRepsSlideFinished: () -> Unit,
    onRepsClick: () -> Unit,
    onTypedTextChange: () -> Unit,
    onTypedValueConfirm: (String) -> Unit,
    onTypingDismiss: () -> Unit,
    onSetClick: (Long) -> Unit,
    onCancelCorrectionClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteSetClick: () -> Unit,
    onDeleteSetConfirm: () -> Unit,
    onDiscardRequest: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSavedAnnouncementHandled: () -> Unit,
    onGenericErrorHandled: () -> Unit,
    onBack: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenProgress: ((Long) -> Unit)?,
    lastPerformance: @Composable (exerciseId: Long, sessionId: Long?, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val names = rememberDateNames()
    val exercise = uiState.exercise
    val isBodyweight = exercise?.equipment == Equipment.BODYWEIGHT

    val genericMessage = stringResource(R.string.session_error_generic)
    val currentOnGenericErrorHandled by rememberUpdatedState(onGenericErrorHandled)
    LaunchedEffect(uiState.genericError) {
        if (uiState.genericError) {
            snackbarHostState.showSnackbar(message = genericMessage, duration = SnackbarDuration.Short)
            currentOnGenericErrorHandled()
        }
    }

    // The live region is always on screen, so TalkBack announces its text when it changes (S40-R27).
    var announcement by remember { mutableStateOf("") }
    val saved = uiState.savedAnnouncement
    val announcementText = saved?.let {
        stringResource(R.string.logger_saved_announcement, it.number, spokenSetOf(isBodyweight, it.weightGrams, it.reps))
    }
    val currentOnAnnouncementHandled by rememberUpdatedState(onSavedAnnouncementHandled)
    LaunchedEffect(saved) {
        if (announcementText != null) {
            announcement = announcementText
            currentOnAnnouncementHandled()
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = uiState.hasUnsavedChanges,
        onBackCompleted = onDiscardRequest,
    )
    val onBackRequest = if (uiState.hasUnsavedChanges) onDiscardRequest else onBack

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = exercise?.name.orEmpty(), maxLines = 1, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = sessionDateLabel(uiState, names),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackRequest) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.logger_back_description),
                        )
                    }
                },
                actions = {
                    exercise?.let { current ->
                        IconButton(onClick = { onEditExercise(current.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.logger_edit_exercise),
                            )
                        }
                        onOpenProgress?.let { openProgress ->
                            ProgressMenu(onViewStats = { openProgress(current.id) })
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading || exercise == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val layoutDirection = LocalLayoutDirection.current
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            // A very large font or a short screen: the panel would leave no room for the list (S42-R28).
            val compact = maxHeight < FullPanelHeight * maxOf(1f, fontScale) + MinListHeight
            val weightAdjuster = adjusterOf(uiState, isBodyweight, ValueField.WEIGHT, onWeightDecrease, onWeightIncrease, onWeightSlide, onWeightSlideFinished, onWeightClick)
            val repsAdjuster = adjusterOf(uiState, isBodyweight, ValueField.REPS, onRepsDecrease, onRepsIncrease, onRepsSlide, onRepsSlideFinished, onRepsClick)
            val showsAdjusters = !uiState.showsArchivedNotice

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection) + spacing.l,
                        end = innerPadding.calculateEndPadding(layoutDirection) + spacing.l,
                        top = spacing.s,
                        bottom = spacing.l,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.m),
                ) {
                    exercise.notes?.takeIf { it.isNotBlank() }?.let { note ->
                        item(key = "note") { FixedNote(note) }
                    }
                    item(key = "last-performance") {
                        lastPerformance(exercise.id, uiState.resolvedSessionId, Modifier.fillMaxWidth())
                    }
                    item(key = "sets-title") {
                        SectionTitle(text = setsTitle(uiState, names))
                    }
                    if (uiState.todaySets.isEmpty()) {
                        item(key = "sets-empty") {
                            Text(
                                text = stringResource(R.string.logger_sets_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(items = uiState.todaySets, key = { it.setId }) { row ->
                        SetRow(
                            row = row,
                            isBodyweight = isBodyweight,
                            selected = row.setId == uiState.editingSetId,
                            onClick = { onSetClick(row.setId) },
                            modifier = Modifier.animateRow(this),
                        )
                    }
                    if (compact && showsAdjusters) {
                        item(key = "reps-adjuster") { repsAdjuster() }
                    }
                }
                LoggerPanel(
                    uiState = uiState,
                    weightAdjuster = weightAdjuster,
                    repsAdjuster = repsAdjuster.takeIf { !compact },
                    onSaveClick = onSaveClick,
                    onDeleteSetClick = onDeleteSetClick,
                    onCancelCorrectionClick = onCancelCorrectionClick,
                    bottomInset = innerPadding.calculateBottomPadding(),
                    startInset = innerPadding.calculateStartPadding(layoutDirection),
                    endInset = innerPadding.calculateEndPadding(layoutDirection),
                )
            }
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .semantics {
                        contentDescription = announcement
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }

    LoggerDialogs(
        uiState = uiState,
        isBodyweight = isBodyweight,
        onTypedTextChange = onTypedTextChange,
        onTypedValueConfirm = onTypedValueConfirm,
        onTypingDismiss = onTypingDismiss,
        onDeleteSetConfirm = onDeleteSetConfirm,
        onDialogDismiss = onDialogDismiss,
        onBack = onBack,
    )
}

/** The reading of a set: the additional weight for a bodyweight exercise, the weight otherwise. */
private fun spokenSetOf(isBodyweight: Boolean, weightGrams: Int, reps: Int): String =
    if (isBodyweight) "${spokenAdditionalWeight(weightGrams)}, ${spokenReps(reps)}" else spokenSet(weightGrams, reps)

/** "Hoy · dom 11 oct" in a session of today, and the short date of a past one (S40-R23). */
@Composable
private fun sessionDateLabel(uiState: SetLoggerUiState, names: DateNames): String {
    val date = formatShortDate(uiState.sessionDate, uiState.today, names, relative = false)
    return if (uiState.sessionDate == uiState.today) stringResource(R.string.session_today_header, date) else date
}

/** The title of the list: "Hoy" in a session of today and the date in a past one (Spec 42). */
@Composable
private fun setsTitle(uiState: SetLoggerUiState, names: DateNames): String =
    if (uiState.sessionDate == uiState.today) {
        stringResource(R.string.logger_sets_title_today)
    } else {
        formatShortDate(uiState.sessionDate, uiState.today, names, relative = false)
    }

/** Builds the control of one value from the state; the weight and the repetitions share it. */
@Composable
private fun adjusterOf(
    uiState: SetLoggerUiState,
    isBodyweight: Boolean,
    field: ValueField,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSlide: (Int) -> Unit,
    onSlideFinished: () -> Unit,
    onNumberClick: () -> Unit,
): @Composable () -> Unit {
    val isWeight = field == ValueField.WEIGHT
    val input = if (isWeight) uiState.weight else uiState.reps
    val value = input.value
    val display = when {
        value == null -> ValueDisplay(
            number = stringResource(R.string.logger_unset_value),
            unit = null,
            spoken = stringResource(R.string.logger_unset_state),
        )
        isWeight -> ValueDisplay(
            number = formatKilograms(value),
            unit = WEIGHT_UNIT,
            spoken = if (isBodyweight) spokenAdditionalWeight(value) else spokenWeight(value),
        )
        else -> ValueDisplay(number = formatReps(value), unit = null, spoken = spokenReps(value))
    }
    val label = stringResource(
        when {
            !isWeight -> R.string.logger_reps_label
            isBodyweight -> R.string.logger_weight_label_bodyweight
            else -> R.string.logger_weight_label
        },
    )
    val unsetHint = stringResource(if (isWeight) R.string.logger_weight_unset_hint else R.string.logger_reps_unset_hint)
    val decrease = stringResource(if (isWeight) R.string.logger_weight_decrease else R.string.logger_reps_decrease)
    val increase = stringResource(if (isWeight) R.string.logger_weight_increase else R.string.logger_reps_increase)
    val actions = AdjusterActions(onDecrease, onIncrease, onSlide, onSlideFinished, onNumberClick)
    return {
        ValueAdjuster(
            label = label,
            input = input,
            display = display,
            unsetHint = unsetHint,
            decreaseDescription = decrease,
            increaseDescription = increase,
            actions = actions,
        )
    }
}

/** The menu of the top bar; it exists only when F05 gives its lambda, and has only "Ver estadísticas" (S42-R24). */
@Composable
private fun ProgressMenu(onViewStats: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.logger_menu_description),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.logger_view_stats)) },
                onClick = {
                    expanded = false
                    onViewStats()
                },
            )
        }
    }
}

/** The fixed note of the exercise, seen before the first set (D13 de la Spec 40). */
@Composable
private fun FixedNote(note: String, modifier: Modifier = Modifier) {
    val spacing = WeightrackTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(text = stringResource(R.string.logger_fixed_note_label), style = MaterialTheme.typography.labelMedium)
            Text(text = note, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A new row slides in; with reduced motion it just appears (S42-R27, S41-R10). */
@Composable
private fun Modifier.animateRow(scope: LazyItemScope): Modifier = with(scope) {
    if (WeightrackTheme.motion.reducedMotion) {
        animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
    } else {
        animateItem()
    }
}

/**
 * One stored set: its number in the list and its weight and repetitions, "1  60 kg × 10". Tapping it puts the panel
 * in correction mode (S42-R17). The selected one has a filled background, and the panel also says "Corrigiendo la
 * serie 2", so the mark is never color alone.
 */
@Composable
private fun SetRow(
    row: SetRowUi,
    isBodyweight: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val description = stringResource(
        R.string.logger_set_row_description,
        row.number,
        spokenSetOf(isBodyweight, row.weightGrams, row.reps),
    )
    val clickLabel = stringResource(R.string.logger_set_row_click_label)
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.large)
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .heightIn(min = RowMinHeight)
            .padding(horizontal = spacing.l, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.number.toString(),
            modifier = Modifier.width(spacing.xxl),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (isBodyweight) {
                "${formatAdditionalWeight(row.weightGrams)} × ${formatReps(row.reps)}"
            } else {
                formatSet(row.weightGrams, row.reps)
            },
            style = WeightrackTheme.numbers.inline,
        )
    }
}

/**
 * The fixed panel at the bottom (Spec 42): the control, the save button and, while a set is corrected, "Eliminar
 * serie" and "Cancelar corrección". An archived exercise gets only its notice (SF02-R16, S40-R29). The panel
 * takes the bottom inset itself so its surface runs under the navigation bar.
 */
@Composable
private fun LoggerPanel(
    uiState: SetLoggerUiState,
    weightAdjuster: @Composable () -> Unit,
    repsAdjuster: (@Composable () -> Unit)?,
    onSaveClick: () -> Unit,
    onDeleteSetClick: () -> Unit,
    onCancelCorrectionClick: () -> Unit,
    bottomInset: Dp,
    startInset: Dp,
    endInset: Dp,
) {
    val spacing = WeightrackTheme.spacing
    val shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
    val editing = uiState.todaySets.firstOrNull { it.setId == uiState.editingSetId }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                start = startInset + spacing.l,
                end = endInset + spacing.l,
                top = spacing.l,
                bottom = bottomInset + spacing.m,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            if (uiState.showsArchivedNotice) {
                Text(
                    text = stringResource(R.string.logger_archived_notice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                editing?.let {
                    Text(
                        text = stringResource(R.string.logger_correcting, it.number),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                weightAdjuster()
                repsAdjuster?.invoke()
                uiState.fieldError?.let { error ->
                    val message = stringResource(error.messageRes())
                    Text(
                        text = message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(onClick = onSaveClick, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (editing != null) R.string.logger_save_changes else R.string.logger_save_set))
                }
                if (editing != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            onClick = onDeleteSetClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.logger_delete_set))
                        }
                        TextButton(onClick = onCancelCorrectionClick) {
                            Text(stringResource(R.string.logger_cancel_correction))
                        }
                    }
                }
            }
        }
    }
}

/** The dialogs of the logger, all decided by the `UiState` (S42-R25): typing a value, deleting a set, discarding. */
@Composable
private fun LoggerDialogs(
    uiState: SetLoggerUiState,
    isBodyweight: Boolean,
    onTypedTextChange: () -> Unit,
    onTypedValueConfirm: (String) -> Unit,
    onTypingDismiss: () -> Unit,
    onDeleteSetConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    uiState.typingDialog?.let { dialog ->
        TypingValueDialog(
            dialog = dialog,
            onTextChange = onTypedTextChange,
            onConfirm = onTypedValueConfirm,
            onDismiss = onTypingDismiss,
        )
    }
    when (val dialog = uiState.confirmDialog) {
        is LoggerDialog.DeleteSet -> {
            val set = uiState.todaySets.firstOrNull { it.number == dialog.setNumber }
            val setText = set?.let {
                if (isBodyweight) "${formatAdditionalWeight(it.weightGrams)} × ${formatReps(it.reps)}" else formatSet(it.weightGrams, it.reps)
            }.orEmpty()
            ConfirmDialog(
                title = stringResource(R.string.logger_delete_dialog_title),
                text = stringResource(R.string.logger_delete_dialog_text, setText, uiState.exercise?.name.orEmpty()),
                confirmLabel = stringResource(R.string.logger_delete_dialog_confirm),
                dismissLabel = stringResource(R.string.logger_delete_dialog_dismiss),
                onConfirm = onDeleteSetConfirm,
                onDismiss = onDialogDismiss,
                isDestructive = true,
            )
        }
        LoggerDialog.Discard -> ConfirmDialog(
            title = stringResource(R.string.session_discard_dialog_title),
            text = stringResource(R.string.session_discard_dialog_text),
            confirmLabel = stringResource(R.string.session_discard_dialog_confirm),
            dismissLabel = stringResource(R.string.session_discard_dialog_dismiss),
            // Close the dialog first so it does not linger over the screen while it leaves.
            onConfirm = {
                onDialogDismiss()
                onBack()
            },
            onDismiss = onDialogDismiss,
            isDestructive = true,
        )
        null -> Unit
    }
}

/**
 * "Escribe el peso" or "Escribe las repeticiones". The text the user types lives here, not in the `UiState`: the
 * ViewModel only learns that it changed (to clear the error) and receives the final text on confirm, so a
 * rejected value stays in the field and the dialog stays open (S40-R08). The text starts selected.
 */
@Composable
private fun TypingValueDialog(
    dialog: TypingDialog,
    onTextChange: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isWeight = dialog.field == ValueField.WEIGHT
    var typed by remember(dialog.field, dialog.initialText) {
        mutableStateOf(TextFieldValue(dialog.initialText, TextRange(0, dialog.initialText.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val errorMessage = dialog.error?.let { stringResource(it.messageRes(dialog.field)) }
    val confirm = { onConfirm(typed.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (isWeight) R.string.logger_typing_weight_title else R.string.logger_typing_reps_title),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = {
                    typed = it
                    onTextChange()
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                label = {
                    Text(stringResource(if (isWeight) R.string.logger_typing_weight_label else R.string.logger_typing_reps_label))
                },
                suffix = if (isWeight) {
                    { Text(WEIGHT_UNIT) }
                } else {
                    null
                },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { message ->
                    { Text(text = message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isWeight) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
            )
        },
        confirmButton = {
            TextButton(onClick = confirm) {
                Text(stringResource(if (isWeight) R.string.logger_typing_weight_confirm else R.string.logger_typing_reps_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.logger_typing_dismiss)) }
        },
    )
}

/** Tunable: the height the full panel needs at the normal font, and the least the list is left with. */
private val FullPanelHeight = 440.dp
private val MinListHeight = 160.dp
private val RowMinHeight = 56.dp

// --- Previews ---

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 11)

private val PreviewState = SetLoggerUiState(
    isLoading = false,
    exercise = LoggerExerciseUi(1L, "Press de banca", Equipment.BARBELL, "Asiento en la posición 4", archived = false),
    today = PreviewToday,
    sessionDate = PreviewToday,
    resolvedSessionId = 9L,
    todaySets = listOf(
        SetRowUi(11L, 1, 60_000, 10),
        SetRowUi(12L, 2, 60_000, 10),
        SetRowUi(13L, 3, 62_500, 8),
    ),
    weight = ValueInput(62_500, weightWindow(62_500), canDecrease = true, canIncrease = true),
    reps = ValueInput(10, repsWindow(10), canDecrease = true, canIncrease = true),
)

/** A preview has no activity, so the back handler gets a root dispatcher of its own; in the app the activity provides it. */
@Composable
private fun LoggerPreview(uiState: SetLoggerUiState) {
    WeightrackTheme {
        val owner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            Surface(color = MaterialTheme.colorScheme.background) {
                SetLoggerContent(
                    uiState = uiState,
                    onWeightDecrease = {},
                    onWeightIncrease = {},
                    onWeightSlide = {},
                    onWeightSlideFinished = {},
                    onWeightClick = {},
                    onRepsDecrease = {},
                    onRepsIncrease = {},
                    onRepsSlide = {},
                    onRepsSlideFinished = {},
                    onRepsClick = {},
                    onTypedTextChange = {},
                    onTypedValueConfirm = {},
                    onTypingDismiss = {},
                    onSetClick = {},
                    onCancelCorrectionClick = {},
                    onSaveClick = {},
                    onDeleteSetClick = {},
                    onDeleteSetConfirm = {},
                    onDiscardRequest = {},
                    onDialogDismiss = {},
                    onSavedAnnouncementHandled = {},
                    onGenericErrorHandled = {},
                    onBack = {},
                    onEditExercise = {},
                    onOpenProgress = null,
                    lastPerformance = { _, _, modifier ->
                        Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(
                                text = "Última vez · jue 8 oct: 60 kg × 10 · 60 × 10 · 60 × 8",
                                modifier = Modifier.padding(WeightrackTheme.spacing.l),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SetLoggerContentPreview() {
    LoggerPreview(PreviewState)
}

@ThemePreviews
@Composable
private fun SetLoggerContentCorrectionPreview() {
    LoggerPreview(PreviewState.copy(editingSetId = 12L))
}

@ThemePreviews
@Composable
private fun SetLoggerContentNewExerciseNotSetPreview() {
    LoggerPreview(
        PreviewState.copy(
            todaySets = emptyList(),
            weight = ValueInput(null, weightWindow(WEIGHT_DEFAULT_REFERENCE_GRAMS), canDecrease = true, canIncrease = true),
            reps = ValueInput(null, repsWindow(REPS_DEFAULT_REFERENCE), canDecrease = true, canIncrease = true),
        ),
    )
}

@ThemePreviews
@Composable
private fun SetLoggerContentArchivedPreview() {
    LoggerPreview(PreviewState.copy(exercise = PreviewState.exercise?.copy(archived = true)))
}

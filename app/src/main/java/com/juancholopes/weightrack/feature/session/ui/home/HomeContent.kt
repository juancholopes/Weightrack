package com.juancholopes.weightrack.feature.session.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.juancholopes.weightrack.R
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.session.ui.cardTone
import com.juancholopes.weightrack.feature.session.ui.labelRes
import com.juancholopes.weightrack.ui.theme.DateNames
import com.juancholopes.weightrack.ui.theme.ThemePreviews
import com.juancholopes.weightrack.ui.theme.WEIGHT_UNIT
import com.juancholopes.weightrack.ui.theme.WeightrackTheme
import com.juancholopes.weightrack.ui.theme.component.EmptyState
import com.juancholopes.weightrack.ui.theme.component.EmptyStateAction
import com.juancholopes.weightrack.ui.theme.component.ExerciseCard
import com.juancholopes.weightrack.ui.theme.component.ExerciseCardAction
import com.juancholopes.weightrack.ui.theme.component.PillTabs
import com.juancholopes.weightrack.ui.theme.component.WeightNumber
import com.juancholopes.weightrack.ui.theme.formatAdditionalWeight
import com.juancholopes.weightrack.ui.theme.formatKilograms
import com.juancholopes.weightrack.ui.theme.formatShortDate
import com.juancholopes.weightrack.ui.theme.rememberDateNames
import com.juancholopes.weightrack.ui.theme.spokenAdditionalWeight
import com.juancholopes.weightrack.ui.theme.spokenDate
import com.juancholopes.weightrack.ui.theme.spokenWeight
import java.time.LocalDate

/**
 * Home screen, S42-P01 (SF02-R06 to SF02-R11, SF02-R21): the pill tabs Hoy and Ejercicios, one big card per
 * exercise, the search and filters of the catalog and the floating "Crear ejercicio". Stateless: it draws
 * [uiState] and reports every event through its lambdas (S30-R31).
 *
 * It owns what only touches this screen: the snackbar of a notice and of an unexpected failure, and the one
 * back handler, enabled only in Ejercicios, where back returns to Hoy (S42-R06). In Hoy back stays with the
 * shell, which leaves the app. Navigation goes out as lambdas, never as the stack (S30-R39).
 *
 * @param onCreateExercise opens the exercise editor with a name to propose and whether to log a set after saving
 * (S42-R14): the button in Hoy and its empty state log after saving, the one in Ejercicios goes back to the list.
 * @param onOpenProgress `null` until F05: with it, each card menu offers "Ver estadísticas".
 * @param onOpenHistory `null` until F04: with it, the top bar shows the history icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onTabSelect: (HomeTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onMuscleFilterSelect: (Muscle?) -> Unit,
    onEquipmentFilterSelect: (Equipment?) -> Unit,
    onStartAnotherSessionClick: () -> Unit,
    onUndoArchiveClick: (Long) -> Unit,
    onNoticeHandled: () -> Unit,
    onGenericErrorHandled: () -> Unit,
    onOpenLogger: (Long) -> Unit,
    onCreateExercise: (prefill: String, logAfterSave: Boolean) -> Unit,
    onEditExercise: (Long) -> Unit,
    onEditSession: (Long) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenProgress: ((Long) -> Unit)?,
    onOpenHistory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = WeightrackTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val inToday = uiState.selectedTab == HomeTab.TODAY

    NoticeEffects(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUndoArchiveClick = onUndoArchiveClick,
        onNoticeHandled = onNoticeHandled,
        onGenericErrorHandled = onGenericErrorHandled,
    )

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = uiState.selectedTab == HomeTab.EXERCISES,
        onBackCompleted = { onTabSelect(HomeTab.TODAY) },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    onOpenHistory?.let { openHistory ->
                        IconButton(onClick = openHistory) {
                            Icon(
                                painter = painterResource(R.drawable.ic_history),
                                contentDescription = stringResource(R.string.home_history),
                            )
                        }
                    }
                    HomeMenu(
                        canStartAnotherSession = uiState.canStartAnotherSession,
                        onStartAnotherSessionClick = onStartAnotherSessionClick,
                        onOpenArchived = onOpenArchived,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Material 3 clears the semantics of the label of an extended FAB and takes the name TalkBack reads from
            // the icon, so the icon carries "Crear ejercicio" (S40-R46); the label stays visible for sighted users.
            ExtendedFloatingActionButton(
                onClick = { onCreateExercise("", inToday) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.home_create_exercise),
                    )
                },
                text = { Text(stringResource(R.string.home_create_exercise)) },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val startInset = innerPadding.calculateStartPadding(layoutDirection)
        val endInset = innerPadding.calculateEndPadding(layoutDirection)
        // The list runs under the navigation bar, so it receives the bottom inset as content padding (S42-R10),
        // plus the room the floating button takes so the last card is never behind it.
        val listPadding = PaddingValues(
            start = startInset + spacing.l,
            end = endInset + spacing.l,
            top = spacing.s,
            bottom = innerPadding.calculateBottomPadding() + FabClearance,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .consumeWindowInsets(innerPadding)
                .fitInside(WindowInsetsRulers.Ime.current),
        ) {
            PillTabs(
                labels = listOf(stringResource(R.string.home_tab_today), stringResource(R.string.home_tab_exercises)),
                selectedIndex = uiState.selectedTab.ordinal,
                onSelect = { onTabSelect(HomeTab.entries[it]) },
                modifier = Modifier.padding(start = startInset + spacing.l, end = endInset + spacing.l, top = spacing.xs),
            )
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val names = rememberDateNames()
                val cards = CardCallbacks(
                    onOpenLogger = onOpenLogger,
                    onEditExercise = onEditExercise,
                    onOpenProgress = onOpenProgress,
                )
                when (uiState.selectedTab) {
                    HomeTab.TODAY -> TodayTab(
                        uiState = uiState,
                        names = names,
                        cards = cards,
                        contentPadding = listPadding,
                        onTabSelect = onTabSelect,
                        onEditSession = onEditSession,
                        onCreateExercise = onCreateExercise,
                    )
                    HomeTab.EXERCISES -> ExercisesTab(
                        uiState = uiState,
                        names = names,
                        cards = cards,
                        contentPadding = listPadding,
                        onQueryChange = onQueryChange,
                        onClearSearch = onClearSearch,
                        onMuscleFilterSelect = onMuscleFilterSelect,
                        onEquipmentFilterSelect = onEquipmentFilterSelect,
                        onCreateExercise = onCreateExercise,
                    )
                }
            }
        }
    }
}

/** What a card can ask of the shell; shared by every card so each tab passes one object. */
private class CardCallbacks(
    val onOpenLogger: (Long) -> Unit,
    val onEditExercise: (Long) -> Unit,
    val onOpenProgress: ((Long) -> Unit)?,
)

/**
 * The effects that show a snackbar: the notice the shell handed over (SF02-R11) and an unexpected failure
 * (SF02-R22). Each is reported as handled once it has been shown. "Deshacer" undoes an archive and is reported
 * before the notice is handled.
 */
@Composable
private fun NoticeEffects(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onUndoArchiveClick: (Long) -> Unit,
    onNoticeHandled: () -> Unit,
    onGenericErrorHandled: () -> Unit,
) {
    val notice = uiState.notice
    val noticeMessage = when (notice) {
        is HomeNotice.Archived -> stringResource(
            if (notice.wasDeleteRequest) R.string.catalog_notice_archived_has_sets else R.string.catalog_notice_archived,
            notice.name,
        )
        is HomeNotice.Deleted -> stringResource(R.string.catalog_notice_deleted, notice.name)
        null -> null
    }
    val undoLabel = stringResource(R.string.catalog_notice_undo)
    val currentOnUndo by rememberUpdatedState(onUndoArchiveClick)
    val currentOnNoticeHandled by rememberUpdatedState(onNoticeHandled)
    LaunchedEffect(notice, noticeMessage) {
        if (notice != null && noticeMessage != null) {
            val offersUndo = notice is HomeNotice.Archived
            val result = snackbarHostState.showSnackbar(
                message = noticeMessage,
                actionLabel = if (offersUndo) undoLabel else null,
                duration = if (offersUndo) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && notice is HomeNotice.Archived) {
                currentOnUndo(notice.exerciseId)
            }
            currentOnNoticeHandled()
        }
    }

    val genericMessage = stringResource(R.string.session_error_generic)
    val currentOnGenericErrorHandled by rememberUpdatedState(onGenericErrorHandled)
    LaunchedEffect(uiState.genericError) {
        if (uiState.genericError) {
            snackbarHostState.showSnackbar(message = genericMessage, duration = SnackbarDuration.Short)
            currentOnGenericErrorHandled()
        }
    }
}

/** The menu of the top bar: "Archivados" and, with a session of today, "Empezar otra sesión" (S40-F09). */
@Composable
private fun HomeMenu(
    canStartAnotherSession: Boolean,
    onStartAnotherSessionClick: () -> Unit,
    onOpenArchived: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.home_menu_description),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canStartAnotherSession) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_menu_start_another_session)) },
                    onClick = {
                        expanded = false
                        onStartAnotherSessionClick()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_menu_archived)) },
                onClick = {
                    expanded = false
                    onOpenArchived()
                },
            )
        }
    }
}

// --- Hoy ---

/**
 * The Hoy tab (SF02-R06): the header of the session of today with its note and "Editar sesión", one card per
 * exercise with sets today, "De tu última sesión" and the link to the catalog. A session without sets, or no
 * session, shows its text instead of cards (S42-A10). With no exercises at all it is the empty state of the
 * catalog, never before the first data (S42-R25).
 */
@Composable
private fun TodayTab(
    uiState: HomeUiState,
    names: DateNames,
    cards: CardCallbacks,
    contentPadding: PaddingValues,
    onTabSelect: (HomeTab) -> Unit,
    onEditSession: (Long) -> Unit,
    onCreateExercise: (String, Boolean) -> Unit,
) {
    val spacing = WeightrackTheme.spacing
    if (uiState.catalogIsEmpty && uiState.todayCards.isEmpty() && uiState.lastSessionCards.isEmpty()) {
        CatalogEmptyState(onCreate = { onCreateExercise("", true) })
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = TodayCellMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        uiState.todaySession?.let { session ->
            fullWidthItem(key = "today-header") {
                TodayHeader(
                    title = stringResource(
                        R.string.session_today_header,
                        formatShortDate(session.date, uiState.today, names, relative = false),
                    ),
                    notes = session.notes,
                    onEditSession = { onEditSession(session.sessionId) },
                )
            }
        }
        if (uiState.todayCards.isEmpty()) {
            fullWidthItem(key = "today-empty") {
                Text(
                    text = stringResource(R.string.home_today_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items = uiState.todayCards, key = { "today-${it.exerciseId}" }) { card ->
            HomeExerciseCard(card = card, today = uiState.today, names = names, callbacks = cards)
        }
        val lastDate = uiState.lastSessionDate
        if (lastDate != null && uiState.lastSessionCards.isNotEmpty()) {
            fullWidthItem(key = "last-header") {
                SectionHeader(
                    text = stringResource(
                        R.string.home_last_session_header,
                        formatShortDate(lastDate, uiState.today, names),
                    ),
                    modifier = Modifier.padding(top = spacing.s),
                )
            }
            items(items = uiState.lastSessionCards, key = { "last-${it.exerciseId}" }) { card ->
                HomeExerciseCard(card = card, today = uiState.today, names = names, callbacks = cards)
            }
        }
        fullWidthItem(key = "see-all") {
            TextButton(onClick = { onTabSelect(HomeTab.EXERCISES) }) {
                Text(stringResource(R.string.home_see_all_exercises))
                Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
        }
    }
}

/** "Hoy · dom 11 oct", the note of the session if there is one, and "Editar sesión" (S42-P01). */
@Composable
private fun TodayHeader(title: String, notes: String?, onEditSession: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f).padding(top = WeightrackTheme.spacing.s)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            notes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEditSession) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.home_edit_session),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// --- Ejercicios ---

/**
 * The Ejercicios tab (SF02-R08): search and filters on top of the grid of cards, which is both the catalog
 * (S40-F03) and the choice of an exercise (S40-F10). No results offers to create what was typed (S40-R14).
 */
@Composable
private fun ExercisesTab(
    uiState: HomeUiState,
    names: DateNames,
    cards: CardCallbacks,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onMuscleFilterSelect: (Muscle?) -> Unit,
    onEquipmentFilterSelect: (Equipment?) -> Unit,
    onCreateExercise: (String, Boolean) -> Unit,
) {
    val spacing = WeightrackTheme.spacing
    if (uiState.catalogIsEmpty) {
        CatalogEmptyState(onCreate = { onCreateExercise("", false) })
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = ExerciseCellMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        fullWidthItem(key = "search") {
            SearchField(query = uiState.query, onQueryChange = onQueryChange, onClearSearch = onClearSearch)
        }
        fullWidthItem(key = "filters") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                FilterMenuChip(
                    label = stringResource(R.string.home_filter_equipment),
                    allLabel = stringResource(R.string.home_filter_all),
                    options = Equipment.entries,
                    selected = uiState.equipmentFilter,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = onEquipmentFilterSelect,
                )
                FilterMenuChip(
                    label = stringResource(R.string.home_filter_muscle),
                    allLabel = stringResource(R.string.home_filter_all),
                    options = Muscle.entries,
                    selected = uiState.muscleFilter,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = onMuscleFilterSelect,
                )
            }
        }
        if (uiState.exerciseCards.isEmpty()) {
            fullWidthItem(key = "no-results") {
                NoResults(
                    query = uiState.query,
                    onCreateNamed = { onCreateExercise(uiState.query, true) },
                    onClearSearch = onClearSearch,
                    onClearFilters = {
                        onEquipmentFilterSelect(null)
                        onMuscleFilterSelect(null)
                    },
                )
            }
        }
        items(items = uiState.exerciseCards, key = { "exercise-${it.exerciseId}" }) { card ->
            HomeExerciseCard(card = card, today = uiState.today, names = names, callbacks = cards)
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClearSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.home_search_label)) },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.home_search_clear_description),
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * A filter chip that opens a menu of its values with "Todos" first (S40-R14). When a value is chosen the chip
 * is selected and shows it instead of its name, so the active filter is written, not only colored.
 */
@Composable
private fun <T> FilterMenuChip(
    label: String,
    allLabel: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let { optionLabel(it) } ?: label) },
            modifier = Modifier.semantics { role = Role.DropdownList },
            trailingIcon = {
                Icon(painter = painterResource(R.drawable.ic_expand_more), contentDescription = null)
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
                modifier = Modifier.semantics { this.selected = selected == null },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier.semantics { this.selected = option == selected },
                )
            }
        }
    }
}

/** No card matched: with text typed it offers to create it or clear the search; with only filters, to remove them. */
@Composable
private fun NoResults(
    query: String,
    onCreateNamed: () -> Unit,
    onClearSearch: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val spacing = WeightrackTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (query.isNotBlank()) {
            Text(
                text = stringResource(R.string.home_no_results, query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCreateNamed) { Text(stringResource(R.string.home_create_named, query)) }
            TextButton(onClick = onClearSearch) { Text(stringResource(R.string.home_clear_search)) }
        } else {
            Text(
                text = stringResource(R.string.home_no_results_filters),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.home_clear_filters)) }
        }
    }
}

/** The empty state of a catalog without exercises, with the action that fills it (S40-R12). */
@Composable
private fun CatalogEmptyState(onCreate: () -> Unit) {
    EmptyState(
        text = stringResource(R.string.home_catalog_empty),
        modifier = Modifier.fillMaxSize(),
        action = EmptyStateAction(label = stringResource(R.string.home_create_exercise), onClick = onCreate),
    )
}

// --- Cards ---

/**
 * One exercise card with the text of S42-P01 "Tarjeta": the number is the heaviest weight of the last time
 * (the additional weight for bodyweight), the footer its date or "3 series hoy", and a card without sets says
 * "Sin series" and has no number. TalkBack gets one node that reads everything (S42-A21).
 */
@Composable
private fun HomeExerciseCard(
    card: ExerciseCardUi,
    today: LocalDate,
    names: DateNames,
    callbacks: CardCallbacks,
    modifier: Modifier = Modifier,
) {
    val equipment = stringResource(card.equipment.labelRes())
    val muscle = card.primaryMuscle?.let { stringResource(it.labelRes()) }
    val subtitle = muscle?.let { stringResource(R.string.card_subtitle, it, equipment) } ?: equipment
    val spokenSubtitle = muscle?.let { stringResource(R.string.card_spoken_subtitle, it, equipment) } ?: equipment

    val isBodyweight = card.equipment == Equipment.BODYWEIGHT
    val peak = card.peakWeightGrams
    val spokenPeak = peak?.let { if (isBodyweight) spokenAdditionalWeight(it) else spokenWeight(it) }
    val setsToday = card.setsToday?.let { pluralStringResource(R.plurals.card_sets_today, it, it) }
    val peakDate = card.peakDate
    val footer = setsToday
        ?: peakDate?.takeIf { peak != null }?.let { formatShortDate(it, today, names) }
        ?: stringResource(R.string.card_no_sets)

    val spokenTail = when {
        setsToday != null && spokenPeak != null ->
            "$setsToday. ${stringResource(R.string.card_spoken_peak_today, spokenPeak)}"
        setsToday != null -> "$setsToday."
        spokenPeak != null && peakDate != null ->
            stringResource(R.string.card_spoken_last_time, spokenDate(peakDate, today, names), spokenPeak)
        else -> "${stringResource(R.string.card_no_sets)}."
    }
    val spokenDescription = listOfNotNull(
        stringResource(R.string.card_spoken_base, card.name, spokenSubtitle),
        stringResource(R.string.card_spoken_archived).takeIf { card.archived },
        spokenTail,
    ).joinToString(" ")

    val editLabel = stringResource(R.string.card_action_edit_exercise)
    val statsLabel = stringResource(R.string.card_action_view_stats)
    val menuActions = buildList {
        add(ExerciseCardAction(editLabel) { callbacks.onEditExercise(card.exerciseId) })
        callbacks.onOpenProgress?.let { openProgress ->
            add(ExerciseCardAction(statsLabel) { openProgress(card.exerciseId) })
        }
    }

    ExerciseCard(
        title = card.name,
        subtitle = subtitle,
        tone = card.equipment.cardTone(),
        onClick = { callbacks.onOpenLogger(card.exerciseId) },
        spokenDescription = spokenDescription,
        tapLabel = stringResource(R.string.card_action_log_set),
        modifier = modifier,
        footer = footer,
        badge = stringResource(R.string.card_archived).takeIf { card.archived },
        weight = peak?.let { grams -> { CardWeight(grams = grams, isBodyweight = isBodyweight) } },
        menuActions = menuActions,
        menuDescription = stringResource(R.string.card_menu_description, card.name),
    )
}

/** "60 kg" with the number big; for bodyweight "+10 kg", or "Sin peso" when it is 0 (S10-R02). */
@Composable
private fun CardWeight(grams: Int, isBodyweight: Boolean) {
    if (isBodyweight && grams == 0) {
        Text(text = formatAdditionalWeight(0), style = MaterialTheme.typography.titleLarge)
    } else {
        WeightNumber(
            number = (if (isBodyweight) "+" else "") + formatKilograms(grams),
            unit = WEIGHT_UNIT,
            style = WeightrackTheme.numbers.card,
        )
    }
}

private fun LazyGridScope.fullWidthItem(key: Any, content: @Composable LazyGridItemScope.() -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }, content = content)
}

/** Tunable: the narrowest a card of Hoy gets, so a phone shows one wide card per row and a tablet two. */
private val TodayCellMinWidth = 300.dp

/** Tunable: the narrowest a card of Ejercicios gets: two columns on a phone (S42-P01). */
private val ExerciseCellMinWidth = 156.dp

/** The space under the last card so the floating button never covers it. */
private val FabClearance = 88.dp

// --- Previews ---

private val PreviewToday: LocalDate = LocalDate.of(2026, 10, 11)
private val PreviewLast: LocalDate = LocalDate.of(2026, 10, 8)

private val PreviewBench = ExerciseCardUi(
    exerciseId = 1L,
    name = "Press de banca",
    primaryMuscle = Muscle.CHEST,
    equipment = Equipment.BARBELL,
    archived = false,
    peakWeightGrams = 60_000,
    peakDate = PreviewLast,
    setsToday = null,
)
private val PreviewRow = PreviewBench.copy(
    exerciseId = 2L,
    name = "Remo en polea",
    primaryMuscle = Muscle.LATS,
    equipment = Equipment.CABLE,
    peakWeightGrams = 50_000,
)
private val PreviewPullUp = PreviewBench.copy(
    exerciseId = 3L,
    name = "Dominadas",
    primaryMuscle = Muscle.LATS,
    equipment = Equipment.BODYWEIGHT,
    peakWeightGrams = 0,
)
private val PreviewSquat = PreviewBench.copy(
    exerciseId = 4L,
    name = "Sentadilla",
    primaryMuscle = Muscle.QUADRICEPS,
    equipment = Equipment.MACHINE,
    peakWeightGrams = null,
    peakDate = null,
)

private val PreviewTodayState = HomeUiState(
    isLoading = false,
    today = PreviewToday,
    todaySession = TodaySessionUi(sessionId = 9L, date = PreviewToday, notes = "Me sentí con energía"),
    todayCards = listOf(PreviewBench.copy(setsToday = 3, peakDate = PreviewToday)),
    lastSessionDate = PreviewLast,
    lastSessionCards = listOf(PreviewRow),
    exerciseCards = listOf(PreviewBench, PreviewRow, PreviewPullUp, PreviewSquat),
    canStartAnotherSession = true,
)

/** A preview has no activity, so the back handler gets a root dispatcher of its own; in the app the activity provides it. */
@Composable
private fun HomePreview(uiState: HomeUiState) {
    WeightrackTheme {
        val owner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            Surface(color = MaterialTheme.colorScheme.background) {
                HomeContent(
                    uiState = uiState,
                    onTabSelect = {},
                    onQueryChange = {},
                    onClearSearch = {},
                    onMuscleFilterSelect = {},
                    onEquipmentFilterSelect = {},
                    onStartAnotherSessionClick = {},
                    onUndoArchiveClick = {},
                    onNoticeHandled = {},
                    onGenericErrorHandled = {},
                    onOpenLogger = {},
                    onCreateExercise = { _, _ -> },
                    onEditExercise = {},
                    onEditSession = {},
                    onOpenArchived = {},
                    onOpenProgress = null,
                    onOpenHistory = null,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun HomeContentTodayPreview() {
    HomePreview(PreviewTodayState)
}

@ThemePreviews
@Composable
private fun HomeContentExercisesPreview() {
    HomePreview(PreviewTodayState.copy(selectedTab = HomeTab.EXERCISES))
}

@ThemePreviews
@Composable
private fun HomeContentNoSessionPreview() {
    HomePreview(PreviewTodayState.copy(todaySession = null, todayCards = emptyList(), canStartAnotherSession = false))
}

@ThemePreviews
@Composable
private fun HomeContentNoResultsPreview() {
    HomePreview(
        PreviewTodayState.copy(selectedTab = HomeTab.EXERCISES, query = "remo con barra", exerciseCards = emptyList()),
    )
}

@ThemePreviews
@Composable
private fun HomeContentEmptyCatalogPreview() {
    HomePreview(HomeUiState(isLoading = false, today = PreviewToday, catalogIsEmpty = true))
}

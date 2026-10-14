package com.juancholopes.weightrack.feature.session.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.juancholopes.weightrack.appContainer
import com.juancholopes.weightrack.feature.catalog.domain.Equipment
import com.juancholopes.weightrack.feature.catalog.domain.Exercise
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseMuscle
import com.juancholopes.weightrack.feature.catalog.domain.ExerciseRepository
import com.juancholopes.weightrack.feature.catalog.domain.Muscle
import com.juancholopes.weightrack.feature.catalog.domain.matchesExerciseSearch
import com.juancholopes.weightrack.feature.session.domain.CreateSessionUseCase
import com.juancholopes.weightrack.feature.session.domain.ExerciseSet
import com.juancholopes.weightrack.feature.session.domain.LatestSessionPeak
import com.juancholopes.weightrack.feature.session.domain.SessionRepository
import com.juancholopes.weightrack.feature.session.domain.WorkoutSession
import com.juancholopes.weightrack.ui.PendingNotice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What the home screen remembers besides what the database holds (S30-R26: private and changed with `update`). */
private data class HomeLocal(
    val selectedTab: HomeTab = HomeTab.TODAY,
    val query: String = "",
    val muscleFilter: Muscle? = null,
    val equipmentFilter: Equipment? = null,
    val notice: HomeNotice? = null,
    val genericError: Boolean = false,
    val isWriting: Boolean = false,
)

/** The sessions the Hoy tab shows: today's, the most recent one before today, and the sets of both. */
private data class SessionsData(
    val today: LocalDate,
    val todaySession: WorkoutSession?,
    val todaySets: List<Pair<ExerciseSet, Exercise>>,
    val lastSession: WorkoutSession?,
    val lastSets: List<Pair<ExerciseSet, Exercise>>,
)

/** What the cards of the Ejercicios tab need. */
private data class CatalogData(
    val filtered: List<Exercise>,
    val isEmpty: Boolean,
    val muscles: List<ExerciseMuscle>,
    val peaks: List<LatestSessionPeak>,
)

/**
 * The home screen, S42-P01 (SF02-R06 to SF02-R11): the Hoy and Ejercicios tabs, the card menu actions that are not
 * navigation, the search and the shell's notice. Navigation (a card, "Editar sesión", "Archivados", the floating
 * button) goes from the composable to the shell without passing here (S30-R29). It reads nothing until its state is
 * collected (S30-R26).
 */
class HomeViewModel(
    private val exercises: ExerciseRepository,
    private val sessions: SessionRepository,
    private val createSession: CreateSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val local = MutableStateFlow(HomeLocal())

    val uiState: StateFlow<HomeUiState> = combine(observeSessions(), observeCatalog(), local) { sessionsData, catalog, state ->
        toUiState(sessionsData, catalog, state)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        HomeUiState(today = LocalDate.now(clock)),
    )

    fun onTabSelect(tab: HomeTab) {
        local.update { it.copy(selectedTab = tab) }
    }

    fun onQueryChange(query: String) {
        local.update { it.copy(query = query) }
    }

    /** "Limpiar búsqueda": clears the text and keeps the filters (S40-R14). */
    fun onClearSearch() {
        local.update { it.copy(query = "") }
    }

    fun onMuscleFilterSelect(muscle: Muscle?) {
        local.update { it.copy(muscleFilter = muscle) }
    }

    fun onEquipmentFilterSelect(equipment: Equipment?) {
        local.update { it.copy(equipmentFilter = equipment) }
    }

    /**
     * "Empezar otra sesión": creates a session of today right away, an explicit action (S40-R22, D4). It is only
     * offered with a session of today, and a second tap while it writes does nothing (S40-R07).
     */
    fun onStartAnotherSessionClick() {
        write {
            val hasSessionOfToday = history().firstOrNull()?.date == LocalDate.now(clock)
            if (hasSessionOfToday) createSession()
        }
    }

    /** The shell hands over the notice that another screen left (S42-R09, D6). The screen then tells the shell it was consumed. */
    fun onNotice(notice: PendingNotice) {
        val homeNotice = when (notice) {
            is PendingNotice.Archived -> HomeNotice.Archived(notice.exerciseId, notice.name, notice.wasDeleteRequest)
            is PendingNotice.Deleted -> HomeNotice.Deleted(notice.name)
        }
        local.update { it.copy(notice = homeNotice) }
    }

    fun onNoticeHandled() {
        local.update { it.copy(notice = null) }
    }

    /** "Deshacer" of an archived notice: the exercise returns to the catalog with its history (S40-R42). */
    fun onUndoArchiveClick(exerciseId: Long) {
        write { exercises.unarchive(exerciseId) }
    }

    fun onGenericErrorHandled() {
        local.update { it.copy(genericError = false) }
    }

    // --- Reads ---

    private suspend fun history(): List<WorkoutSession> = sessions.observeHistory().first()

    /**
     * SF02-R06: the first session of the history is today's if it is dated today (S10-R28); "De tu última sesión" is the
     * most recent one dated before today. "Today" comes from the clock each time the history changes.
     */
    private fun observeSessions(): Flow<SessionsData> = sessions.observeHistory().flatMapLatest { history ->
        val today = LocalDate.now(clock)
        val todaySession = history.firstOrNull()?.takeIf { it.date == today }
        val lastSession = history.firstOrNull { it.date < today }
        combine(observeSets(todaySession), observeSets(lastSession)) { todaySets, lastSets ->
            SessionsData(today, todaySession, todaySets, lastSession, lastSets)
        }
    }

    private fun observeSets(session: WorkoutSession?): Flow<List<Pair<ExerciseSet, Exercise>>> =
        session?.let { sessions.observeSessionSets(it.id) } ?: flowOf(emptyList())

    /** SF02-R08: the filtered catalog, the whole catalog to know if it is empty, and the muscles and peaks for the cards. */
    private fun observeCatalog(): Flow<CatalogData> {
        val filters = local.map { it.muscleFilter to it.equipmentFilter }.distinctUntilChanged()
        val filtered = filters.flatMapLatest { (muscle, equipment) -> exercises.observeActiveCatalog(muscle, equipment) }
        return combine(
            filtered,
            exercises.observeActiveCatalog(),
            exercises.observeAllMuscles(),
            sessions.observeLatestSessionPeaks(),
        ) { filteredCatalog, wholeCatalog, muscles, peaks ->
            CatalogData(filteredCatalog, wholeCatalog.isEmpty(), muscles, peaks)
        }
    }

    // --- Cards ---

    private fun toUiState(sessionsData: SessionsData, catalog: CatalogData, state: HomeLocal): HomeUiState {
        val primaryMuscles = catalog.muscles.primaryMuscleByExercise()
        val peaks = catalog.peaks.associateBy { it.exerciseId }
        fun card(exercise: Exercise, setsToday: Int?) = ExerciseCardUi(
            exerciseId = exercise.id,
            name = exercise.name,
            primaryMuscle = primaryMuscles[exercise.id],
            equipment = exercise.equipment,
            archived = exercise.archived,
            peakWeightGrams = peaks[exercise.id]?.heaviestWeightGrams,
            peakDate = peaks[exercise.id]?.date,
            setsToday = setsToday,
        )

        val todayByExercise = sessionsData.todaySets.groupBy { (_, exercise) -> exercise.id }
        val todayCards = todayByExercise.values.map { sets -> card(sets.first().second, setsToday = sets.size) }
        val lastSessionCards = sessionsData.lastSets
            .map { (_, exercise) -> exercise }
            .distinctBy { it.id }
            .filter { !it.archived && it.id !in todayByExercise }
            .map { card(it, setsToday = null) }
        val exerciseCards = catalog.filtered
            .filter { matchesExerciseSearch(it.name, state.query) }
            .map { card(it, setsToday = null) }

        return HomeUiState(
            isLoading = false,
            today = sessionsData.today,
            selectedTab = state.selectedTab,
            todaySession = sessionsData.todaySession?.let { TodaySessionUi(it.id, it.date, it.notes) },
            todayCards = todayCards,
            lastSessionDate = sessionsData.lastSession?.date,
            lastSessionCards = lastSessionCards,
            exerciseCards = exerciseCards,
            query = state.query,
            muscleFilter = state.muscleFilter,
            equipmentFilter = state.equipmentFilter,
            catalogIsEmpty = catalog.isEmpty,
            canStartAnotherSession = sessionsData.todaySession != null,
            notice = state.notice,
            genericError = state.genericError,
        )
    }

    /** D2: the first primary muscle in the order of the `Muscle` enum, because the table does not keep the order of choice. */
    private fun List<ExerciseMuscle>.primaryMuscleByExercise(): Map<Long, Muscle> =
        filter { it.isPrimary }
            .groupBy { it.exerciseId }
            .mapValues { (_, muscles) -> muscles.minOf { it.muscle } }

    // --- Writes ---

    /**
     * Runs one write at a time (S40-R07). An unexpected exception becomes [HomeUiState.genericError] (S30-R23);
     * cancellation is never swallowed.
     */
    private fun write(operation: suspend () -> Unit) {
        if (local.value.isWriting) return
        local.update { it.copy(isWriting = true) }
        viewModelScope.launch {
            try {
                operation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                local.update { it.copy(genericError = true) }
            } finally {
                local.update { it.copy(isWriting = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    exercises = appContainer.exerciseRepository,
                    sessions = appContainer.sessionRepository,
                    createSession = appContainer.createSessionUseCase,
                    clock = appContainer.clock,
                )
            }
        }
    }
}

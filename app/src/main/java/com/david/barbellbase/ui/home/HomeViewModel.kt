package com.david.barbellbase.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.BodyRepository
import com.david.barbellbase.data.repo.HabitRepository
import com.david.barbellbase.data.repo.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val activeSession: SessionEntity? = null,
    val nextRoutineName: String? = null,
    val nextDay: RoutineDayEntity? = null,
    val weekVolumeKg: Double = 0.0,
    val weekSessions: Int = 0,
    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val walkMin: Int = 0,
    val walkTargetMin: Int = 30,
    val recent: List<SessionEntity> = emptyList(),
    val latestWeightKg: Double? = null,
    val units: UnitSystem = UnitSystem.KG,
)

private data class HabitSnapshot(val water: Int, val waterTarget: Int, val walk: Int, val walkTarget: Int)
private data class WeekSnapshot(val volumeKg: Double, val sessions: Int)

private data class Partial(
    val active: SessionEntity?,
    val habits: HabitSnapshot,
    val week: WeekSnapshot,
    val recent: List<SessionEntity>,
    val units: UnitSystem,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val habitRepository: HabitRepository,
    private val bodyRepository: BodyRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val nextDay = workoutRepository.observeSuggestedNextDay()
    private val from = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

    private val habits = combine(
        habitRepository.waterToday,
        habitRepository.waterTarget,
        habitRepository.walkToday,
        habitRepository.walkTarget,
    ) { water, waterTarget, walk, walkTarget ->
        HabitSnapshot(water, waterTarget, walk, walkTarget)
    }

    private val week = combine(
        workoutRepository.observeVolumeSince(from),
        workoutRepository.observeSessionCountSince(from),
    ) { volume, sessions -> WeekSnapshot(volume, sessions) }

    private val partial = combine(
        workoutRepository.observeActiveSession(),
        habits,
        week,
        workoutRepository.observeRecent(4),
        settingsRepository.settings,
    ) { active, habit, weekData, recent, settings ->
        Partial(active, habit, weekData, recent, settings.units)
    }

    val state = combine(partial, nextDay, bodyRepository.observeLatest()) { p, next, body ->
        HomeState(
            activeSession = p.active,
            nextRoutineName = next?.first?.name,
            nextDay = next?.second,
            weekVolumeKg = p.week.volumeKg,
            weekSessions = p.week.sessions,
            waterMl = p.habits.water,
            waterTargetMl = p.habits.waterTarget,
            walkMin = p.habits.walk,
            walkTargetMin = p.habits.walkTarget,
            recent = p.recent,
            latestWeightKg = body?.weightKg,
            units = p.units,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun logWater(amountMl: Int) {
        viewModelScope.launch { habitRepository.logWater(amountMl) }
    }

    fun logWalk(minutes: Int) {
        viewModelScope.launch { habitRepository.logWalk(minutes) }
    }

    fun startEmpty(onStarted: (String) -> Unit) {
        viewModelScope.launch { onStarted(workoutRepository.startEmptySession("Workout")) }
    }

    fun startDay(dayId: String, onStarted: (String) -> Unit) {
        viewModelScope.launch { onStarted(workoutRepository.startFromDay(dayId)) }
    }
}

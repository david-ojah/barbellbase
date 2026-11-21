package com.david.barbellbase.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.BodyMetricEntity
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.BodyRepository
import com.david.barbellbase.data.repo.ExerciseRepository
import com.david.barbellbase.domain.Clock
import com.david.barbellbase.domain.Metrics
import com.david.barbellbase.domain.Units
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class LiftTrend(
    val exerciseId: String,
    val name: String,
    val points: List<Float>,
    val bestKg: Double,
    val latestKg: Double,
)

data class WeekBar(val label: String, val volumeKg: Double, val sessions: Int)

data class ProgressState(
    val units: UnitSystem = UnitSystem.KG,
    val weeks: List<WeekBar> = emptyList(),
    val bodyPoints: List<Float> = emptyList(),
    val bodyUnit: String = "kg",
    val latestBodyKg: Double? = null,
    val bodyChangeKg: Double? = null,
    val lifts: List<LiftTrend> = emptyList(),
    val totalSessions: Int = 0,
    val totalSets: Int = 0,
    val totalVolumeKg: Double = 0.0,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    workoutRepository: com.david.barbellbase.data.repo.WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    bodyRepository: BodyRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val weekFormatter = DateTimeFormatter.ofPattern("d MMM")

    private fun buildWeeks(sessions: List<SessionEntity>, sets: List<SetEntryEntity>, mondayFirst: Boolean): List<WeekBar> {
        val now = Clock.now()
        val currentWeekStart = Clock.startOfWeek(now, mondayFirst)
        val buckets = (7 downTo 0).map { offset ->
            val start = currentWeekStart - offset * 7L * 86_400_000L
            start
        }
        return buckets.map { start ->
            val end = start + 7L * 86_400_000L
            val volume = sets.filter { (it.completedAt ?: 0L) in start until end }
                .sumOf { Metrics.volume(it.weightKg, it.reps) }
            val count = sessions.count { it.startedAt in start until end }
            WeekBar(
                label = java.time.Instant.ofEpochMilli(start).atZone(Clock.zone())
                    .toLocalDate().format(weekFormatter),
                volumeKg = volume,
                sessions = count,
            )
        }
    }

    private fun buildLifts(sets: List<SetEntryEntity>, exercises: List<ExerciseEntity>): List<LiftTrend> {
        val byId = exercises.associateBy { it.id }
        return sets.groupBy { it.exerciseId }
            .filter { it.value.size >= 3 }
            .mapNotNull { (exerciseId, exerciseSets) ->
                val exercise = byId[exerciseId] ?: return@mapNotNull null
                val estimates = exerciseSets
                    .sortedBy { it.completedAt ?: 0L }
                    .mapNotNull { set -> Metrics.e1rm(set.weightKg, set.reps) }
                if (estimates.isEmpty()) return@mapNotNull null
                LiftTrend(
                    exerciseId = exerciseId,
                    name = exercise.name,
                    points = estimates.takeLast(20).map { it.toFloat() },
                    bestKg = estimates.max(),
                    latestKg = estimates.last(),
                )
            }
            .sortedByDescending { it.points.size }
            .take(4)
    }

    val state = combine(
        workoutRepository.observeHistory(),
        workoutRepository.observeAllCompletedSets(),
        exerciseRepository.observeAll(),
        bodyRepository.observeAll(),
        settingsRepository.settings,
    ) { sessions, sets, exercises, body, settings ->
        val weeks = buildWeeks(sessions, sets, settings.weekStartsMonday)
        val bodySeries: List<BodyMetricEntity> = body.sortedBy { it.epochDay }
        val bodyValues = bodySeries.map { Units.fromKg(it.weightKg, settings.units).toFloat() }
        val first = bodySeries.firstOrNull()?.weightKg
        val last = bodySeries.lastOrNull()?.weightKg
        ProgressState(
            units = settings.units,
            weeks = weeks,
            bodyPoints = bodyValues,
            bodyUnit = Units.label(settings.units),
            latestBodyKg = last,
            bodyChangeKg = if (first != null && last != null) last - first else null,
            lifts = buildLifts(sets, exercises),
            totalSessions = sessions.size,
            totalSets = sets.size,
            totalVolumeKg = sets.sumOf { Metrics.volume(it.weightKg, it.reps) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())
}

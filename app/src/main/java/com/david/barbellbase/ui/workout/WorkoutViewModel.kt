package com.david.barbellbase.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.Measurement
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.db.SetType
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.ExerciseRepository
import com.david.barbellbase.data.repo.WorkoutData
import com.david.barbellbase.data.repo.WorkoutRepository
import com.david.barbellbase.domain.Metrics
import com.david.barbellbase.domain.Units
import com.david.barbellbase.service.RestState
import com.david.barbellbase.service.RestTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutState(
    val workout: WorkoutData? = null,
    val units: UnitSystem = UnitSystem.KG,
    val showRpe: Boolean = false,
    val rest: RestState? = null,
    val previous: Map<String, List<SetEntryEntity>> = emptyMap(),
    val allExercises: List<ExerciseEntity> = emptyList(),
    val elapsedSec: Long = 0,
    val doneSets: Int = 0,
    val totalSets: Int = 0,
    val volumeKg: Double = 0.0,
    val finished: Boolean = false,
    val autoStartRest: Boolean = true,
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val restTimer: RestTimerController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val previous = MutableStateFlow<Map<String, List<SetEntryEntity>>>(emptyMap())
    val editingSet = MutableStateFlow<SetEntryEntity?>(null)

    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    private val withClock = combine(workoutRepository.observeWorkout(sessionId), clock) { workout, now ->
        workout to now
    }

    init {
        viewModelScope.launch {
            workoutRepository.observeWorkout(sessionId).collect { data ->
                val exerciseIds = data?.blocks?.map { it.exercise.id }.orEmpty()
                val map = exerciseIds.associateWith { id -> workoutRepository.previousSets(id) }
                previous.value = map
            }
        }
    }

    val state = combine(
        withClock,
        restTimer.state,
        settingsRepository.settings,
        previous,
        exerciseRepository.observeAll(),
    ) { (workout, now), rest, settings, prev, exercises ->
        val allSets = workout?.blocks?.flatMap { it.sets }.orEmpty()
        val done = allSets.filter { it.completedAt != null }
        WorkoutState(
            workout = workout,
            units = settings.units,
            showRpe = settings.showRpe,
            rest = rest,
            previous = prev,
            allExercises = exercises,
            elapsedSec = workout?.session?.let { ((now - it.startedAt) / 1000).coerceAtLeast(0) } ?: 0,
            doneSets = done.size,
            totalSets = allSets.size,
            volumeKg = done.sumOf { Metrics.volume(it.weightKg, it.reps) },
            finished = workout?.session?.finishedAt != null,
            autoStartRest = settings.autoStartRest,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutState())

    fun addExercise(exerciseId: String) {
        viewModelScope.launch { workoutRepository.addExercise(sessionId, exerciseId) }
    }

    fun removeExercise(exerciseId: String) {
        viewModelScope.launch { workoutRepository.removeExercise(sessionId, exerciseId) }
    }

    fun addSet(exerciseId: String) {
        viewModelScope.launch { workoutRepository.addSet(sessionId, exerciseId) }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch {
            workoutRepository.deleteSet(setId)
            if (editingSet.value?.id == setId) editingSet.value = null
        }
    }

    fun edit(set: SetEntryEntity) {
        editingSet.value = set
    }

    fun dismissEditor() {
        editingSet.value = null
    }

    fun saveSet(
        set: SetEntryEntity,
        weightInput: String,
        repsInput: String,
        rpeInput: String,
        type: SetType,
        complete: Boolean,
    ) {
        viewModelScope.launch {
            val units = state.value.units
            val measurement = measurementFor(set.exerciseId)
            val weight = if (measurement == Measurement.TIME) null else
                weightInput.replace(',', '.').toDoubleOrNull()?.let { Units.toKg(it, units) }
            val reps = if (measurement == Measurement.TIME) null else repsInput.filter { it.isDigit() }.toIntOrNull()
            val duration = if (measurement == Measurement.TIME) repsInput.filter { it.isDigit() }.toIntOrNull() else null
            val rpe = rpeInput.replace(',', '.').toDoubleOrNull()
            workoutRepository.updateSet(set.id, weight, reps, rpe, type, complete, duration)
            if (complete && state.value.autoStartRest) {
                maybeStartRest(set)
            }
            editingSet.value = null
        }
    }

    private suspend fun maybeStartRest(set: SetEntryEntity) {
        val exercise = state.value.workout?.blocks?.firstOrNull { it.exercise.id == set.exerciseId }?.exercise
        val seconds = exercise?.defaultRestSec ?: 120
        restTimer.start(seconds)
    }

    fun completeQuick(set: SetEntryEntity, weightKg: Double?, reps: Int?) {
        viewModelScope.launch {
            workoutRepository.completeSet(set.id, weightKg, reps, set.rpe, set.setType, set.durationSec)
            maybeStartRest(set)
        }
    }

    fun skipRest() = restTimer.stop()

    fun extendRest(seconds: Int) = restTimer.extend(seconds)

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.stop()
            workoutRepository.finishSession(sessionId)
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.stop()
            workoutRepository.discardSession(sessionId)
            onDone()
        }
    }

    fun setType(set: SetEntryEntity, type: SetType) {
        viewModelScope.launch { workoutRepository.setType(set.id, type) }
    }

    fun measurementFor(exerciseId: String): Measurement =
        state.value.workout?.blocks?.firstOrNull { it.exercise.id == exerciseId }?.exercise?.measurement
            ?: Measurement.WEIGHT_REPS
}

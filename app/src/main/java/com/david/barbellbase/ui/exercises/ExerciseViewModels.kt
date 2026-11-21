package com.david.barbellbase.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.Equipment
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.Measurement
import com.david.barbellbase.data.db.MuscleGroup
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.ExerciseRepository
import com.david.barbellbase.data.repo.WorkoutRepository
import com.david.barbellbase.domain.Metrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseLibraryState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val query: String = "",
    val muscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val muscle = MutableStateFlow<MuscleGroup?>(null)
    private val equipment = MutableStateFlow<Equipment?>(null)

    val state = combine(
        repository.observeAll(),
        query,
        muscle,
        equipment,
    ) { all, q, m, e ->
        ExerciseLibraryState(
            exercises = all.filter { exercise ->
                (q.isBlank() || exercise.name.contains(q, ignoreCase = true) ||
                    exercise.primaryMuscles.any { it.name.contains(q, ignoreCase = true) }) &&
                    (m == null || exercise.primaryMuscles.contains(m) || exercise.secondaryMuscles.contains(m)) &&
                    (e == null || exercise.equipment == e)
            },
            query = q,
            muscle = m,
            equipment = e,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setMuscle(value: MuscleGroup?) {
        muscle.value = value
    }

    fun setEquipment(value: Equipment?) {
        equipment.value = value
    }

    fun create(
        name: String,
        primary: List<MuscleGroup>,
        equipmentValue: Equipment,
        measurement: Measurement,
        restSec: Int,
        instructions: String,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val created = repository.createCustom(
                name = name,
                primary = primary,
                secondary = emptyList(),
                equipment = equipmentValue,
                measurement = measurement,
                defaultRestSec = restSec,
                instructions = instructions,
            )
            onCreated(created.id)
        }
    }
}

data class ExerciseDetailState(
    val exercise: ExerciseEntity? = null,
    val sets: List<SetEntryEntity> = emptyList(),
    val units: UnitSystem = UnitSystem.KG,
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    val state = combine(
        repository.observeById(exerciseId),
        workoutRepository.observeCompletedForExercise(exerciseId),
        settingsRepository.settings,
    ) { exercise, sets, settings ->
        ExerciseDetailState(exercise, sets, settings.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailState())

    fun archive() {
        val exercise = state.value.exercise ?: return
        viewModelScope.launch { repository.archive(exercise) }
    }
}

/** One point on the estimated one-rep-max history. */
data class E1rmPoint(val at: Long, val valueKg: Double)

fun e1rmHistory(sets: List<SetEntryEntity>): List<E1rmPoint> =
    sets.filter { it.completedAt != null }
        .mapNotNull { set ->
            val estimate = Metrics.e1rm(set.weightKg, set.reps)
            if (estimate == null) null else E1rmPoint(set.completedAt ?: 0L, estimate)
        }
        .sortedBy { it.at }

fun bestSet(sets: List<SetEntryEntity>): SetEntryEntity? =
    sets.filter { it.completedAt != null && it.weightKg != null }
        .maxByOrNull { it.weightKg ?: 0.0 }

package com.david.barbellbase.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.RoutineEntity
import com.david.barbellbase.data.repo.RoutineRepository
import com.david.barbellbase.data.repo.RoutineSummary
import com.david.barbellbase.data.repo.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesState(
    val summaries: List<RoutineSummary> = emptyList(),
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repo: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val state = repo.observeSummaries()
        .let { flow -> combine(flow) { RoutinesState(it[0]) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesState())

    fun create(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repo.createRoutine(name)) }
    }

    fun delete(routineId: String) {
        viewModelScope.launch { repo.deleteRoutine(routineId) }
    }

    fun startDay(dayId: String, onStarted: (String) -> Unit) {
        viewModelScope.launch { onStarted(workoutRepository.startFromDay(dayId)) }
    }
}

data class RoutineDetailState(
    val routine: RoutineEntity? = null,
    val days: List<RoutineDayEntity> = emptyList(),
    val exerciseCounts: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val routineId: String = checkNotNull(savedStateHandle["routineId"])

    val state = combine(
        repo.observeRoutine(routineId),
        repo.observeDays(routineId),
    ) { routine, days -> routine to days }
        .let { flow ->
            combine(flow, repo.observeSummaries()) { (routine, days), summaries ->
                val counts = summaries.firstOrNull { it.routine.id == routineId }?.setTargets.orEmpty()
                RoutineDetailState(routine, days, counts)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineDetailState())

    fun addDay(onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repo.addDay(routineId)) }
    }

    fun rename(name: String) {
        viewModelScope.launch { repo.renameRoutine(routineId, name) }
    }

    fun renameDay(dayId: String, name: String) {
        viewModelScope.launch { repo.renameDay(dayId, name) }
    }

    fun deleteDay(dayId: String) {
        viewModelScope.launch { repo.deleteDay(dayId) }
    }

    fun moveDay(dayId: String, delta: Int) {
        viewModelScope.launch { repo.moveDay(routineId, dayId, delta) }
    }

    fun deleteRoutine(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteRoutine(routineId)
            onDeleted()
        }
    }

    fun startDay(dayId: String, onStarted: (String) -> Unit) {
        viewModelScope.launch { onStarted(workoutRepository.startFromDay(dayId)) }
    }
}

data class DayEditorState(
    val day: RoutineDayEntity? = null,
    val routineName: String? = null,
    val items: List<TargetRow> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
)

data class TargetRow(
    val entry: com.david.barbellbase.data.db.RoutineExerciseEntity,
    val exercise: ExerciseEntity?,
)

@HiltViewModel
class DayEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RoutineRepository,
    private val exercises: com.david.barbellbase.data.repo.ExerciseRepository,
) : ViewModel() {

    private val dayId: String = checkNotNull(savedStateHandle["dayId"])

    val state = combine(
        repo.observeDayDetail(dayId),
        exercises.observeAll(),
    ) { detail, all ->
        val byId = all.associateBy { it.id }
        DayEditorState(
            day = detail?.day,
            routineName = detail?.routine?.name,
            items = detail?.items.orEmpty().map { TargetRow(it, byId[it.exerciseId]) },
            exercises = all,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditorState())

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repo.addExerciseToDay(
                dayId = dayId,
                exerciseId = exercise.id,
                targetSets = 3,
                repsMin = 8,
                repsMax = 12,
                restSec = exercise.defaultRestSec,
            )
        }
    }

    fun update(entry: com.david.barbellbase.data.db.RoutineExerciseEntity) {
        viewModelScope.launch { repo.updateDayExercise(entry) }
    }

    fun remove(entryId: String) {
        viewModelScope.launch { repo.removeDayExercise(entryId) }
    }

    fun move(entryId: String, delta: Int) {
        viewModelScope.launch { repo.moveDayExercise(dayId, entryId, delta) }
    }
}

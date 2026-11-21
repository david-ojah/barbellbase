package com.david.barbellbase.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.ExerciseRepository
import com.david.barbellbase.data.repo.WorkoutRepository
import com.david.barbellbase.domain.Metrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryRow(
    val session: SessionEntity,
    val volumeKg: Double,
    val setCount: Int,
)

data class HistoryState(
    val rows: List<HistoryRow> = emptyList(),
    val units: UnitSystem = UnitSystem.KG,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val rows = workoutRepository.observeHistory()
        .map { sessions ->
            val aggregates = workoutRepository.aggregates(sessions.map { it.id }).associateBy { it.sessionId }
            sessions.map { session ->
                val aggregate = aggregates[session.id]
                HistoryRow(
                    session = session,
                    volumeKg = aggregate?.volumeKg ?: 0.0,
                    setCount = aggregate?.setCount ?: 0,
                )
            }
        }

    val state = combine(rows, settingsRepository.settings) { list, settings ->
        HistoryState(list, settings.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    fun delete(sessionId: String) {
        viewModelScope.launch { workoutRepository.deleteSession(sessionId) }
    }
}

data class SessionBlock(
    val exercise: ExerciseEntity?,
    val sets: List<SetEntryEntity>,
)

data class SessionDetailState(
    val session: SessionEntity? = null,
    val blocks: List<SessionBlock> = emptyList(),
    val units: UnitSystem = UnitSystem.KG,
    val volumeKg: Double = 0.0,
    val setCount: Int = 0,
    val exerciseCount: Int = 0,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val state = combine(
        workoutRepository.observeSession(sessionId),
        workoutRepository.observeSessionSets(sessionId),
        exerciseRepository.observeAll(),
        settingsRepository.settings,
    ) { session, sets, exercises, settings ->
        val byId = exercises.associateBy { it.id }
        val ordered = sets.sortedWith(compareBy({ it.exerciseOrder }, { it.position }))
        val blocks = ordered.groupBy { it.exerciseId }
            .map { (exerciseId, blockSets) ->
                SessionBlock(byId[exerciseId], blockSets.sortedBy { it.position })
            }
        val completed = sets.filter { it.completedAt != null }
        SessionDetailState(
            session = session,
            blocks = blocks,
            units = settings.units,
            volumeKg = completed.sumOf { Metrics.volume(it.weightKg, it.reps) },
            setCount = completed.size,
            exerciseCount = blocks.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailState())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
            onDeleted()
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch { workoutRepository.updateSessionNotes(sessionId, notes) }
    }
}

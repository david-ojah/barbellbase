package com.david.barbellbase.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.HabitKind
import com.david.barbellbase.data.repo.DayTotal
import com.david.barbellbase.data.repo.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitsState(
    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val walkMin: Int = 0,
    val walkTargetMin: Int = 30,
    val waterSeries: List<DayTotal> = emptyList(),
    val walkSeries: List<DayTotal> = emptyList(),
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
) : ViewModel() {

    val state = combine(
        repository.waterToday,
        repository.waterTarget,
        repository.walkToday,
        repository.walkTarget,
        combine(repository.waterSeries(14), repository.walkSeries(14)) { water, walk -> water to walk },
    ) { water, waterTarget, walk, walkTarget, series ->
        HabitsState(
            waterMl = water,
            waterTargetMl = waterTarget,
            walkMin = walk,
            walkTargetMin = walkTarget,
            waterSeries = series.first,
            walkSeries = series.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsState())

    fun logWater(amountMl: Int) {
        viewModelScope.launch { repository.logWater(amountMl) }
    }

    fun logWalk(minutes: Int) {
        viewModelScope.launch { repository.logWalk(minutes) }
    }

    fun undoWater() {
        viewModelScope.launch { repository.undoLast(HabitKind.WATER) }
    }

    fun undoWalk() {
        viewModelScope.launch { repository.undoLast(HabitKind.WALK) }
    }
}

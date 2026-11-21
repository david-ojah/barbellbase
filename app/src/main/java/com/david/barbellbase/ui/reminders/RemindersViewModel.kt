package com.david.barbellbase.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.data.repo.HabitRepository
import com.david.barbellbase.domain.ReminderSchedule
import com.david.barbellbase.reminder.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersState(
    val rules: List<ReminderRuleEntity> = emptyList(),
    val exactAlarm: Boolean = false,
    val waterTodayMl: Int = 0,
    val walkTodayMin: Int = 0,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: ReminderRepository,
    habitRepository: HabitRepository,
) : ViewModel() {

    private val permissions = MutableStateFlow(false)

    val state = combine(
        repository.observeAll(),
        permissions,
        habitRepository.waterToday,
        habitRepository.walkToday,
    ) { rules, exact, water, walk ->
        RemindersState(rules, exact, water, walk)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersState())

    fun refreshPermissions() {
        permissions.value = repository.hasExactAlarmAccess()
        viewModelScope.launch { repository.rescheduleAll() }
    }

    fun toggle(rule: ReminderRuleEntity, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(rule, enabled) }
    }

    fun save(rule: ReminderRuleEntity) {
        viewModelScope.launch { repository.save(rule) }
    }

    fun delete(ruleId: Int) {
        viewModelScope.launch { repository.delete(ruleId) }
    }

    fun test(ruleId: Int) {
        viewModelScope.launch { repository.test(ruleId) }
    }

    fun snooze(ruleId: Int) {
        viewModelScope.launch { repository.snooze(ruleId, 10) }
    }

    /** Preview for a rule that is not saved yet: same maths as the real scheduler. */
    fun nextFirePreview(rule: ReminderRuleEntity): Long? =
        ReminderSchedule.nextFire(rule, System.currentTimeMillis())

    fun newRuleTemplate(): ReminderRuleEntity = ReminderRuleEntity(
        kind = com.david.barbellbase.data.db.ReminderKind.CUSTOM,
        label = "New reminder",
        intervalMinutes = 90,
        windowStartMin = 8 * 60,
        windowEndMin = 20 * 60,
        sound = com.david.barbellbase.data.db.AlertSound.CHIME,
    )
}

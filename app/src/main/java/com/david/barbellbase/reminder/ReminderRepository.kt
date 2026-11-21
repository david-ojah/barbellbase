package com.david.barbellbase.reminder

import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.data.db.defaultReminderRules
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler,
    private val settings: com.david.barbellbase.data.prefs.SettingsRepository,
) {

    fun observeAll(): Flow<List<ReminderRuleEntity>> = dao.observeAll()

    suspend fun get(id: Int): ReminderRuleEntity? = dao.getById(id)

    suspend fun ensureSeeded() {
        // Seed once only: if the user deletes every rule, do not resurrect the defaults on next launch.
        if (!settings.isRemindersSeeded()) {
            if (dao.getAll().isEmpty()) {
                defaultReminderRules().forEach { dao.insert(it) }
            }
            settings.markRemindersSeeded()
        }
        scheduler.rescheduleAll()
    }

    /** Inserts or updates a rule and immediately re-arms its alarm. Returns the rule id. */
    suspend fun save(rule: ReminderRuleEntity): Int {
        val id = if (rule.id == 0) {
            dao.insert(rule).toInt()
        } else {
            dao.upsert(rule)
            rule.id
        }
        val saved = dao.getById(id)
        if (saved != null) scheduler.reschedule(saved)
        return id
    }

    suspend fun setEnabled(rule: ReminderRuleEntity, enabled: Boolean) {
        val updated = rule.copy(enabled = enabled, nextFireAt = null)
        dao.upsert(updated)
        if (enabled) scheduler.reschedule(updated) else scheduler.cancel(rule.id)
    }

    suspend fun delete(id: Int) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    suspend fun test(id: Int) = scheduler.scheduleTest(id)

    suspend fun snooze(id: Int, minutes: Int) = scheduler.snooze(id, minutes)

    suspend fun rescheduleAll() = scheduler.rescheduleAll()

    fun hasExactAlarmAccess(): Boolean = scheduler.hasExactAccess()
}

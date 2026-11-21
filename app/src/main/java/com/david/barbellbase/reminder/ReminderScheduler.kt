package com.david.barbellbase.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.domain.ReminderSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ReminderDao,
) {
    companion object {
        const val EXTRA_RULE_ID = "rule_id"
        const val EXTRA_TEST = "is_test"
        const val EXTRA_SNOOZE = "is_snooze"
    }

    private fun alarmManager(): AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun hasExactAccess(): Boolean = alarmManager().canScheduleExactAlarms()

    /** Recomputes and persists the next firing time, then arms the alarm. */
    suspend fun reschedule(rule: ReminderRuleEntity): ReminderRuleEntity {
        if (!rule.enabled) {
            cancel(rule.id)
            dao.setNextFire(rule.id, null)
            return rule.copy(nextFireAt = null)
        }
        val next = ReminderSchedule.nextFire(rule, System.currentTimeMillis())
        dao.setNextFire(rule.id, next)
        if (next != null) scheduleAt(rule.id, next, test = false) else cancel(rule.id)
        return rule.copy(nextFireAt = next)
    }

    suspend fun rescheduleAll(): List<ReminderRuleEntity> =
        dao.getEnabled().map { reschedule(it) }

    fun scheduleAt(ruleId: Int, atMillis: Long, test: Boolean = false) {
        val manager = alarmManager()
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_RULE_ID, ruleId)
            .putExtra(EXTRA_TEST, test)
        val pending = PendingIntent.getBroadcast(
            context,
            if (test) ruleId * 10 + 1 else ruleId * 10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (hasExactAccess()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    fun scheduleTest(ruleId: Int, delaySeconds: Long = 10) {
        scheduleAt(ruleId, System.currentTimeMillis() + delaySeconds * 1000, test = true)
    }

    fun snooze(ruleId: Int, minutes: Int) {
        scheduleAt(ruleId, System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L, test = true)
    }

    fun cancel(ruleId: Int) {
        val manager = alarmManager()
        for (code in listOf(ruleId * 10, ruleId * 10 + 1)) {
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.cancel(pending)
            pending.cancel()
        }
    }

    fun statusLabel(rule: ReminderRuleEntity): String {
        val next = rule.nextFireAt ?: return if (rule.enabled) "Not scheduled" else "Off"
        return com.david.barbellbase.domain.TimeFormat.clock(next)
    }
}

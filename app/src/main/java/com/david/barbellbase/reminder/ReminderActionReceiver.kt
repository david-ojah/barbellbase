package com.david.barbellbase.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.david.barbellbase.data.db.LogSource
import com.david.barbellbase.data.db.ReminderAction
import com.david.barbellbase.data.db.ReminderEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_LOG_WATER = "com.david.barbellbase.LOG_WATER"
        const val ACTION_LOG_WALK = "com.david.barbellbase.LOG_WALK"
        const val ACTION_SNOOZE = "com.david.barbellbase.SNOOZE"
        const val ACTION_STOP = "com.david.barbellbase.STOP"

        private const val EXTRA_RULE_ID = "rule_id"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_ACTION = "action"

        private fun actionCode(action: String): Int = when (action) {
            ACTION_LOG_WATER -> 1
            ACTION_LOG_WALK -> 2
            ACTION_SNOOZE -> 3
            else -> 4
        }

        fun pending(context: Context, ruleId: Int, action: String, amount: Int = 0): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_RULE_ID, ruleId)
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_ACTION, action)
            }
            return PendingIntent.getBroadcast(
                context,
                ruleId * 100 + actionCode(action),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getIntExtra(EXTRA_RULE_ID, -1)
        if (ruleId < 0) return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: intent.action ?: return
        val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entry = context.reminderEntryPoint()
                val dao = entry.reminderDao()
                val rule = dao.getById(ruleId)
                val habit = entry.habitRepository()

                when (action) {
                    ACTION_LOG_WATER -> {
                        if (amount > 0) habit.logWater(amount, LogSource.NOTIFICATION)
                        record(dao, ruleId, ReminderAction.LOGGED)
                    }

                    ACTION_LOG_WALK -> {
                        if (amount > 0) habit.logWalk(amount, LogSource.NOTIFICATION)
                        record(dao, ruleId, ReminderAction.LOGGED)
                    }

                    ACTION_SNOOZE -> {
                        val minutes = rule?.snoozeMinutes ?: 10
                        entry.reminderScheduler().snooze(ruleId, minutes)
                        record(dao, ruleId, ReminderAction.SNOOZED)
                    }

                    else -> record(dao, ruleId, ReminderAction.DISMISSED)
                }

                if (rule != null) {
                    NotificationManagerCompat.from(context)
                        .cancel(ReminderNotifications.notificationIdFor(rule.kind, ruleId))
                }
                context.stopService(Intent(context, ReminderAlertService::class.java))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun record(dao: com.david.barbellbase.data.db.ReminderDao, ruleId: Int, action: ReminderAction) {
        dao.insertEvent(
            ReminderEventEntity(
                ruleId = ruleId,
                scheduledAt = 0,
                firedAt = System.currentTimeMillis(),
                action = action,
            ),
        )
    }
}

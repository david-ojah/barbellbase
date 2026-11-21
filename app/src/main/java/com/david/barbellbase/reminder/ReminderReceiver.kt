package com.david.barbellbase.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.david.barbellbase.data.db.AlertSound
import com.david.barbellbase.data.db.ReminderAction
import com.david.barbellbase.data.db.ReminderEventEntity
import com.david.barbellbase.data.db.ReminderKind
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.domain.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getIntExtra(ReminderScheduler.EXTRA_RULE_ID, -1)
        if (ruleId <= 0) return
        val isTest = intent.getBooleanExtra(ReminderScheduler.EXTRA_TEST, false)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handle(context, ruleId, isTest)
            } catch (_: Throwable) {
                // A failing reminder must never crash the app or block future alarms.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, ruleId: Int, isTest: Boolean) {
        val entry = context.reminderEntryPoint()
        val dao = entry.reminderDao()
        val scheduler = entry.reminderScheduler()
        val rule = dao.getById(ruleId) ?: return

        if (!rule.enabled && !isTest) {
            scheduler.cancel(ruleId)
            return
        }

        if (!isTest) {
            val now = System.currentTimeMillis()
            if (!withinWindow(rule, now)) {
                scheduler.reschedule(rule)
                return
            }
            val cap = rule.dailyCap
            if (cap != null && cap > 0) {
                val firedToday = dao.eventCountSince(ruleId, Clock.startOfDay(now))
                if (firedToday >= cap) {
                    scheduler.reschedule(rule)
                    return
                }
            }
        }

        val habit = entry.habitRepository()
        val notificationsEnabled = NotificationChannels.canPost(context)

        if (notificationsEnabled) {
            when (rule.kind) {
                ReminderKind.WATER -> {
                    val total = habit.waterTodayValue()
                    val target = rule.waterTargetMl ?: 2500
                    val amount = rule.waterAmountMl ?: 250
                    val ringing = rule.sound == AlertSound.ALARM &&
                        startAlarmService(context, rule, total, target, amount)
                    if (!ringing) {
                        NotificationManagerCompat.from(context).notify(
                            ReminderNotifications.waterId(ruleId),
                            ReminderNotifications.buildWater(
                                context = context,
                                ruleId = ruleId,
                                totalMl = total,
                                targetMl = target,
                                amountMl = amount,
                                sound = rule.sound,
                                ringing = false,
                                fallbackChannel = rule.sound == AlertSound.ALARM,
                            ),
                        )
                    }
                }

                ReminderKind.WALK -> {
                    val walked = habit.walkTodayValue()
                    NotificationManagerCompat.from(context).notify(
                        ReminderNotifications.walkId(ruleId),
                        ReminderNotifications.buildWalk(
                            context,
                            ruleId,
                            walked,
                            rule.walkTargetMin ?: 30,
                            rule.walkSuggestMin ?: 10,
                            rule.sound,
                        ),
                    )
                }

                ReminderKind.CUSTOM -> {
                    NotificationManagerCompat.from(context).notify(
                        ReminderNotifications.notificationIdFor(rule.kind, ruleId),
                        ReminderNotifications.buildCustom(context, ruleId, rule.label, rule.sound, ringing = false),
                    )
                }
            }

            dao.insertEvent(
                ReminderEventEntity(
                    ruleId = ruleId,
                    scheduledAt = rule.nextFireAt ?: System.currentTimeMillis(),
                    firedAt = System.currentTimeMillis(),
                    action = ReminderAction.FIRED,
                ),
            )
        }

        dao.setLastFired(ruleId, System.currentTimeMillis())

        if (!isTest) {
            // Re-read so the schedule is based on the freshly persisted state, then arm the next alarm.
            val fresh = dao.getById(ruleId)
            if (fresh != null) scheduler.reschedule(fresh)
        }
    }

    private fun isTestSilent(rule: ReminderRuleEntity): Boolean = rule.sound == AlertSound.SILENT

    /** Returns true when the foreground service actually started (and will post its own notification). */
    private fun startAlarmService(
        context: Context,
        rule: ReminderRuleEntity,
        total: Int,
        target: Int,
        amount: Int,
    ): Boolean {
        val intent = Intent(context, ReminderAlertService::class.java).apply {
            putExtra(ReminderAlertService.EXTRA_RULE_ID, rule.id)
            putExtra(ReminderAlertService.EXTRA_KIND, rule.kind.name)
            putExtra(ReminderAlertService.EXTRA_LABEL, rule.label)
            putExtra(ReminderAlertService.EXTRA_SOUND, rule.sound.name)
            putExtra(ReminderAlertService.EXTRA_RING_SECONDS, rule.ringSeconds)
            putExtra(ReminderAlertService.EXTRA_VIBRATE, rule.vibration)
            putExtra(ReminderAlertService.EXTRA_WATER_TOTAL, total)
            putExtra(ReminderAlertService.EXTRA_WATER_TARGET, target)
            putExtra(ReminderAlertService.EXTRA_WATER_AMOUNT, amount)
        }
        return runCatching {
            context.startForegroundService(intent)
            true
        }.getOrDefault(false)
    }

    private fun withinWindow(rule: ReminderRuleEntity, nowMillis: Long): Boolean {
        val time = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        val minutes = time.hour * 60 + time.minute
        val start = rule.windowStartMin
        val end = rule.windowEndMin
        if (start == end) return false
        return if (end > start) minutes in start until end else minutes >= start || minutes < end
    }
}

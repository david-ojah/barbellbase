package com.david.barbellbase.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** Schedules the single "rest over" alarm. Nothing keeps running in between. */
object RestAlarmScheduler {
    private const val REQUEST_CODE = 900
    const val EXTRA_END_AT = "rest_end_at"

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RestAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(context: Context, endAt: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pending(context)
        runCatching {
            if (manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending)
            }
        }.onFailure {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending) }
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pending(context)
        manager.cancel(pending)
        pending.cancel()
    }
}

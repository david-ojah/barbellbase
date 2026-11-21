package com.david.barbellbase.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.david.barbellbase.reminder.reminderEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestNotifications.done(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = context.reminderEntryPoint().settingsRepository()
                val current = settings.settings.first()
                val endAt = current.restEndsAt
                // Only clear the rest state if this alarm belongs to the timer that is still current.
                if (endAt != null && endAt <= System.currentTimeMillis() + 5_000L) {
                    settings.setRest(null, 0)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

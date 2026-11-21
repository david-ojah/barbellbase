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

class RestActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SKIP = "com.david.barbellbase.REST_SKIP"
        const val ACTION_PLUS = "com.david.barbellbase.REST_PLUS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = context.reminderEntryPoint().settingsRepository()
                val current = settings.settings.first()
                when (intent.action) {
                    ACTION_PLUS -> {
                        val end = (current.restEndsAt ?: System.currentTimeMillis()) + 15_000L
                        settings.setRest(end, current.restTotalSec)
                        RestNotifications.show(context, end)
                        RestAlarmScheduler.schedule(context, end)
                    }

                    else -> {
                        settings.setRest(null, 0)
                        RestNotifications.cancel(context)
                        RestAlarmScheduler.cancel(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

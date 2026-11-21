package com.david.barbellbase.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = applicationContext.updateEntryPoint()
        val result = entryPoint.updateRepository().check(manual = false)
        val update = result.getOrNull()

        if (update != null) {
            val preferences = entryPoint.updatePreferences()
            val stored = preferences.read()
            if (stored.dismissedTag != update.tag && stored.notifiedTag != update.tag) {
                UpdateNotifications.notify(applicationContext, update)
                preferences.markNotified(update.tag)
            }
        }

        return Result.success()
    }
}

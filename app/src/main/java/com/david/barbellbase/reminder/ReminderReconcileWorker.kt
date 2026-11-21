package com.david.barbellbase.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Safety net: once a day, verify every enabled rule still has a live alarm and repair it.
 * AlarmManager does not survive reboots, OEM battery killers, or timezone jumps on its own.
 */
class ReminderReconcileWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        return runCatching {
            runBlocking {
                applicationContext.reminderEntryPoint().reminderRepository().rescheduleAll()
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val NAME = "reminder-reconcile"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderReconcileWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(2, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}

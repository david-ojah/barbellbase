package com.david.barbellbase.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        return runCatching {
            UpdateNotifier.checkAndNotify(applicationContext, manual)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        /** Set by the push receiver to bypass the remote-config cache. */
        const val KEY_MANUAL = "manual"
    }
}

package com.david.barbellbase.data.update

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives the silent "a release was published" signal from the release
 * workflow. The message carries no metadata the app trusts; it only wakes a
 * one-time check so Remote Config stays the single source of truth.
 */
class UpdateMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != TYPE_UPDATE_AVAILABLE) return

        val request = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setInputData(workDataOf(UpdateWorker.KEY_MANUAL to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val TOPIC = "update-available"
        private const val TYPE_UPDATE_AVAILABLE = "update_available"
        private const val WORK_NAME = "update-push-check"
    }
}

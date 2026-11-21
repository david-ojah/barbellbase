package com.david.barbellbase

import android.app.Application
import com.david.barbellbase.data.sync.SyncConfig
import com.david.barbellbase.data.sync.SyncScheduler
import com.david.barbellbase.data.update.UpdateMessagingService
import com.david.barbellbase.data.update.UpdateScheduler
import com.david.barbellbase.data.update.updateEntryPoint
import com.david.barbellbase.di.Seeder
import com.david.barbellbase.reminder.NotificationChannels
import com.david.barbellbase.reminder.ReminderReconcileWorker
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BarbellBaseApp : Application() {

    @Inject
    lateinit var seeder: Seeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        SyncConfig.initialize(this)
        super.onCreate()
        NotificationChannels.createAll(this)
        SyncScheduler.schedule(this)
        UpdateScheduler.schedule(this)
        appScope.launch {
            seeder.seed()
            ReminderReconcileWorker.enqueue(this@BarbellBaseApp)
        }

        if (SyncConfig.isPushConfigured) {
            runCatching {
                FirebaseMessaging.getInstance().subscribeToTopic(UpdateMessagingService.TOPIC)
            }
        }

        // FCM is best-effort, so refresh once per app session as a fallback.
        appScope.launch { maybeCheckForUpdates() }
    }

    private suspend fun maybeCheckForUpdates() {
        if (!SyncConfig.isFirebaseConfigured) return
        val entryPoint = updateEntryPoint()
        val age = System.currentTimeMillis() - entryPoint.updatePreferences().read().checkedAt
        if (age > APP_OPEN_CHECK_INTERVAL_MS) {
            entryPoint.updateRepository().check(manual = true)
        }
    }

    private companion object {
        const val APP_OPEN_CHECK_INTERVAL_MS = 15L * 60 * 1000
    }
}

package com.david.barbellbase

import android.app.Application
import com.david.barbellbase.di.Seeder
import com.david.barbellbase.reminder.NotificationChannels
import com.david.barbellbase.reminder.ReminderReconcileWorker
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
        super.onCreate()
        NotificationChannels.createAll(this)
        appScope.launch {
            seeder.seed()
            ReminderReconcileWorker.enqueue(this@BarbellBaseApp)
        }
    }
}

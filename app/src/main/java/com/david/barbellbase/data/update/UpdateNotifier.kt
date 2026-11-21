package com.david.barbellbase.data.update

import android.content.Context
import java.time.LocalDate

/**
 * One notification path for the periodic worker and the push-triggered check.
 * A push is only a signal: the release metadata always comes from the remote
 * config check, and skipped versions stay quiet for the rest of the day.
 */
object UpdateNotifier {

    suspend fun checkAndNotify(context: Context, manual: Boolean) {
        val entryPoint = context.updateEntryPoint()
        val update = entryPoint.updateRepository().check(manual).getOrNull() ?: return
        val preferences = entryPoint.updatePreferences()
        val stored = preferences.read()
        val today = LocalDate.now().toEpochDay()
        if (!stored.isSuppressed(today) && stored.notifiedTag != update.tag) {
            UpdateNotifications.notify(context, update)
            preferences.markNotified(update.tag)
        }
    }
}

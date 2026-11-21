package com.david.barbellbase.data.repo

import androidx.room.withTransaction
import com.david.barbellbase.data.db.BodyDao
import com.david.barbellbase.data.db.HabitDao
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.db.SessionDao
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.service.RestTimerController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wipes everything the user has logged — workout sessions and sets, bodyweight entries, water/walk
 * logs and reminder history — plus any in-progress session and rest timer. Library content
 * (exercises, routines), reminder rules and preferences are kept, and armed reminders stay armed.
 */
@Singleton
class ResetRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val sessionDao: SessionDao,
    private val habitDao: HabitDao,
    private val bodyDao: BodyDao,
    private val reminderDao: ReminderDao,
    private val settings: SettingsRepository,
    private val restTimer: RestTimerController,
) {
    suspend fun resetTrackedData() {
        db.withTransaction {
            // Sets first: in-progress sessions may have sets that would otherwise cascade oddly.
            sessionDao.clearSets()
            sessionDao.clearSessions()
            bodyDao.clearAll()
            habitDao.clearAll()
            reminderDao.clearEvents()
        }
        // Drop the pointer to a session that no longer exists, and silence a running rest countdown.
        settings.setActiveSession(null)
        restTimer.stop()
    }
}

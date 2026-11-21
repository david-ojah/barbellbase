package com.david.barbellbase.reminder

import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.repo.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun reminderDao(): ReminderDao
    fun habitRepository(): HabitRepository
    fun reminderRepository(): ReminderRepository
    fun reminderScheduler(): ReminderScheduler
    fun settingsRepository(): com.david.barbellbase.data.prefs.SettingsRepository
}

fun Context.reminderEntryPoint(): ReminderEntryPoint =
    EntryPointAccessors.fromApplication(this, ReminderEntryPoint::class.java)

package com.david.barbellbase.di

import com.david.barbellbase.data.repo.ExerciseRepository
import com.david.barbellbase.reminder.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Seeder @Inject constructor(
    private val exercises: ExerciseRepository,
    private val reminders: ReminderRepository,
) {
    suspend fun seed() {
        exercises.ensureSeeded()
        reminders.ensureSeeded()
    }
}

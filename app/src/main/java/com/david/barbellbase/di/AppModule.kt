package com.david.barbellbase.di

import android.content.Context
import androidx.room.Room
import com.david.barbellbase.data.db.BodyDao
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.HabitDao
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.MIGRATION_1_2
import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.db.RoutineDao
import com.david.barbellbase.data.db.SessionDao
import com.david.barbellbase.data.db.SyncChangeDao
import com.david.barbellbase.data.db.SyncDatabaseCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BarbellBaseDatabase =
        Room.databaseBuilder(context, BarbellBaseDatabase::class.java, "barbellbase.db")
            .addMigrations(MIGRATION_1_2)
            .addCallback(SyncDatabaseCallback)
            .build()

    @Provides
    fun exerciseDao(db: BarbellBaseDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun routineDao(db: BarbellBaseDatabase): RoutineDao = db.routineDao()

    @Provides
    fun sessionDao(db: BarbellBaseDatabase): SessionDao = db.sessionDao()

    @Provides
    fun habitDao(db: BarbellBaseDatabase): HabitDao = db.habitDao()

    @Provides
    fun reminderDao(db: BarbellBaseDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun bodyDao(db: BarbellBaseDatabase): BodyDao = db.bodyDao()

    @Provides
    fun syncChangeDao(db: BarbellBaseDatabase): SyncChangeDao = db.syncChangeDao()
}

package com.david.barbellbase.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("DELETE FROM exercises")
    suspend fun clearAll()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY createdAt")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY createdAt")
    suspend fun observeRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: String): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: String): RoutineEntity?

    @Query("SELECT * FROM routine_days WHERE routineId = :routineId ORDER BY position")
    fun observeDays(routineId: String): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    suspend fun getDay(dayId: String): RoutineDayEntity?

    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    fun observeDay(dayId: String): Flow<RoutineDayEntity?>

    @Query("SELECT * FROM routine_days ORDER BY routineId, position")
    fun observeDaysForAll(): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_days WHERE routineId = :routineId ORDER BY position")
    suspend fun getDays(routineId: String): List<RoutineDayEntity>

    @Query("SELECT * FROM routine_exercises WHERE dayId = :dayId ORDER BY position")
    fun observeDayExercises(dayId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE dayId = :dayId ORDER BY position")
    suspend fun getDayExercises(dayId: String): List<RoutineExerciseEntity>

    @Query("SELECT * FROM routine_exercises ORDER BY dayId, position")
    fun observeAllDayExercises(): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT MAX(position) FROM routine_days WHERE routineId = :routineId")
    suspend fun maxDayPosition(routineId: String): Int?

    @Query("SELECT MAX(position) FROM routine_exercises WHERE dayId = :dayId")
    suspend fun maxExercisePosition(dayId: String): Int?

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Upsert
    suspend fun upsertDay(day: RoutineDayEntity)

    @Upsert
    suspend fun upsertDayExercise(exercise: RoutineExerciseEntity)

    @Upsert
    suspend fun upsertDayExercises(exercises: List<RoutineExerciseEntity>)

    @Upsert
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Upsert
    suspend fun upsertDays(days: List<RoutineDayEntity>)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: String)

    @Query("DELETE FROM routine_days WHERE id = :dayId")
    suspend fun deleteDay(dayId: String)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteDayExercise(id: String)

    @Query("SELECT * FROM routines")
    suspend fun getAllRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_days")
    suspend fun getAllDays(): List<RoutineDayEntity>

    @Query("SELECT * FROM routine_exercises")
    suspend fun getAllDayExercises(): List<RoutineExerciseEntity>

    @Query("DELETE FROM routines")
    suspend fun clearRoutines()

    @Query("DELETE FROM routine_days")
    suspend fun clearDays()

    @Query("DELETE FROM routine_exercises")
    suspend fun clearDayExercises()
}

data class SessionAggregate(
    val sessionId: String,
    val volumeKg: Double,
    val setCount: Int,
)

data class ExerciseSetStat(
    val exerciseId: String,
    val setCount: Int,
    val topWeightKg: Double?,
    val totalReps: Int?,
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC")
    suspend fun unfinishedSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastFinished(): SessionEntity?

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId ORDER BY exerciseOrder, position")
    fun observeSets(sessionId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId ORDER BY exerciseOrder, position")
    suspend fun getSets(sessionId: String): List<SetEntryEntity>

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getSet(id: String): SetEntryEntity?

    @Query(
        "SELECT * FROM set_entries WHERE exerciseId = :exerciseId AND completedAt IS NOT NULL " +
            "ORDER BY completedAt DESC LIMIT 400",
    )
    fun observeCompletedForExercise(exerciseId: String): Flow<List<SetEntryEntity>>

    @Query(
        "SELECT * FROM set_entries WHERE exerciseId = :exerciseId AND completedAt IS NOT NULL " +
            "AND sessionId = (SELECT sessionId FROM set_entries WHERE exerciseId = :exerciseId " +
            "AND completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT 1) ORDER BY position",
    )
    suspend fun lastCompletedSetsForExercise(exerciseId: String): List<SetEntryEntity>

    @Query(
        "SELECT sessionId, SUM(COALESCE(weightKg,0) * COALESCE(reps,0)) AS volumeKg, " +
            "COUNT(*) AS setCount FROM set_entries WHERE completedAt IS NOT NULL AND sessionId IN (:sessionIds) " +
            "GROUP BY sessionId",
    )
    suspend fun aggregatesFor(sessionIds: List<String>): List<SessionAggregate>

    @Query(
        "SELECT exerciseId, COUNT(*) AS setCount, MAX(weightKg) AS topWeightKg, SUM(reps) AS totalReps " +
            "FROM set_entries WHERE sessionId = :sessionId AND completedAt IS NOT NULL GROUP BY exerciseId",
    )
    suspend fun statsForSession(sessionId: String): List<ExerciseSetStat>

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertSet(set: SetEntryEntity)

    @Upsert
    suspend fun upsertSets(sets: List<SetEntryEntity>)

    @Upsert
    suspend fun upsertSessions(sessions: List<SessionEntity>)

    @Insert
    suspend fun insertSets(sets: List<SetEntryEntity>)

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT COUNT(*) FROM set_entries WHERE sessionId = :sessionId AND completedAt IS NOT NULL")
    suspend fun completedSetCount(sessionId: String): Int

    @Query("SELECT DISTINCT sessionId FROM set_entries WHERE exerciseId = :exerciseId AND completedAt IS NOT NULL")
    suspend fun sessionIdsForExercise(exerciseId: String): List<String>

    @Query("SELECT * FROM sessions")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM set_entries")
    suspend fun getAllSets(): List<SetEntryEntity>

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM set_entries")
    suspend fun clearSets()

    @Query("SELECT COALESCE(SUM(COALESCE(weightKg,0) * COALESCE(reps,0)),0) FROM set_entries WHERE completedAt >= :from")
    fun observeVolumeSince(from: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM set_entries WHERE completedAt >= :from")
    fun observeSetCountSince(from: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions WHERE startedAt >= :from AND finishedAt IS NOT NULL")
    fun observeSessionCountSince(from: Long): Flow<Int>

    @Query("SELECT * FROM set_entries WHERE completedAt IS NOT NULL ORDER BY completedAt")
    fun observeAllCompletedSets(): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt")
    fun observeAllFinished(): Flow<List<SessionEntity>>
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_logs WHERE kind = :kind AND loggedAt >= :from AND loggedAt < :to ORDER BY loggedAt")
    fun observeBetween(kind: HabitKind, from: Long, to: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE kind = :kind ORDER BY loggedAt DESC LIMIT :limit")
    fun observeRecent(kind: HabitKind, limit: Int): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE loggedAt >= :from AND loggedAt < :to ORDER BY loggedAt")
    fun observeAllBetween(from: Long, to: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT COALESCE(SUM(amountMl),0) FROM habit_logs WHERE kind = 'WATER' AND loggedAt >= :from AND loggedAt < :to")
    fun observeWaterTotal(from: Long, to: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMin),0) FROM habit_logs WHERE kind = 'WALK' AND loggedAt >= :from AND loggedAt < :to")
    fun observeWalkTotal(from: Long, to: Long): Flow<Int>

    @Insert
    suspend fun insert(log: HabitLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM habit_logs WHERE id = :id")
    suspend fun get(id: Long): HabitLogEntity?

    @Query("SELECT MAX(id) FROM habit_logs")
    suspend fun lastId(): Long?

    @Query("SELECT * FROM habit_logs WHERE kind = :kind ORDER BY id DESC LIMIT 1")
    suspend fun last(kind: HabitKind): HabitLogEntity?

    @Query("SELECT * FROM habit_logs ORDER BY loggedAt")
    suspend fun getAll(): List<HabitLogEntity>

    @Query("DELETE FROM habit_logs")
    suspend fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_rules ORDER BY kind, createdAt")
    fun observeAll(): Flow<List<ReminderRuleEntity>>

    @Query("SELECT * FROM reminder_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderRuleEntity>

    @Query("SELECT * FROM reminder_rules WHERE id = :id")
    suspend fun getById(id: Int): ReminderRuleEntity?

    @Insert
    suspend fun insert(rule: ReminderRuleEntity): Long

    @Upsert
    suspend fun upsert(rule: ReminderRuleEntity)

    @Query("UPDATE reminder_rules SET nextFireAt = :next WHERE id = :id")
    suspend fun setNextFire(id: Int, next: Long?)

    @Query("UPDATE reminder_rules SET lastFiredAt = :at WHERE id = :id")
    suspend fun setLastFired(id: Int, at: Long)

    @Query("DELETE FROM reminder_rules WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT COUNT(*) FROM reminder_events WHERE ruleId = :ruleId AND firedAt >= :since")
    suspend fun eventCountSince(ruleId: Int, since: Long): Int

    @Insert
    suspend fun insertEvent(event: ReminderEventEntity)

    @Insert
    suspend fun insertEvents(events: List<ReminderEventEntity>)

    @Upsert
    suspend fun upsertAll(rules: List<ReminderRuleEntity>)

    @Query("DELETE FROM reminder_events WHERE firedAt < :before")
    suspend fun pruneEvents(before: Long)

    @Query("SELECT * FROM reminder_rules ORDER BY kind, createdAt")
    suspend fun getAll(): List<ReminderRuleEntity>

    @Query("DELETE FROM reminder_rules WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM reminder_rules")
    suspend fun clearAll()

    @Query("DELETE FROM reminder_events")
    suspend fun clearEvents()
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_metrics ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMetricEntity?>

    @Query("DELETE FROM body_metrics WHERE epochDay = :epochDay")
    suspend fun deleteByDay(epochDay: Long)

    @Insert
    suspend fun insert(metric: BodyMetricEntity)

    @Insert
    suspend fun insertAll(metrics: List<BodyMetricEntity>)

    @Query("SELECT * FROM body_metrics ORDER BY epochDay")
    suspend fun getAll(): List<BodyMetricEntity>

    @Query("DELETE FROM body_metrics")
    suspend fun clearAll()
}

@androidx.room.Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        SessionEntity::class,
        SetEntryEntity::class,
        BodyMetricEntity::class,
        ReminderRuleEntity::class,
        ReminderEventEntity::class,
        HabitLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@androidx.room.TypeConverters(Converters::class)
abstract class BarbellBaseDatabase : androidx.room.RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao
    abstract fun habitDao(): HabitDao
    abstract fun reminderDao(): ReminderDao
    abstract fun bodyDao(): BodyDao
}

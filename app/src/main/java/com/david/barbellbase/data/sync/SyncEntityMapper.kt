package com.david.barbellbase.data.sync

import com.david.barbellbase.data.db.BodyDao
import com.david.barbellbase.data.db.BodyMetricEntity
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.HabitDao
import com.david.barbellbase.data.db.HabitLogEntity
import com.david.barbellbase.data.db.RoutineDao
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.RoutineEntity
import com.david.barbellbase.data.db.RoutineExerciseEntity
import com.david.barbellbase.data.db.SessionDao
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.db.SetEntryEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Translates typed Room rows to/from the JSON documents stored in Firestore. */
@Singleton
class SyncEntityMapper @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val sessionDao: SessionDao,
    private val bodyDao: BodyDao,
    private val habitDao: HabitDao,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun load(entityType: String, entityId: String): String? = when (entityType) {
        SyncEntityTypes.EXERCISES ->
            exerciseDao.getById(entityId)?.let { json.encodeToString(ExerciseEntity.serializer(), it) }
        SyncEntityTypes.ROUTINES ->
            routineDao.getRoutine(entityId)?.let { json.encodeToString(RoutineEntity.serializer(), it) }
        SyncEntityTypes.ROUTINE_DAYS ->
            routineDao.getDay(entityId)?.let { json.encodeToString(RoutineDayEntity.serializer(), it) }
        SyncEntityTypes.ROUTINE_EXERCISES ->
            routineDao.getDayExercise(entityId)?.let { json.encodeToString(RoutineExerciseEntity.serializer(), it) }
        SyncEntityTypes.SESSIONS ->
            sessionDao.getSession(entityId)?.let { json.encodeToString(SessionEntity.serializer(), it) }
        SyncEntityTypes.SETS ->
            sessionDao.getSet(entityId)?.let { json.encodeToString(SetEntryEntity.serializer(), it) }
        SyncEntityTypes.BODY_METRICS ->
            bodyDao.getById(entityId)?.let { json.encodeToString(BodyMetricEntity.serializer(), it) }
        SyncEntityTypes.HABIT_LOGS ->
            habitDao.get(entityId)?.let { json.encodeToString(HabitLogEntity.serializer(), it) }
        else -> null
    }

    suspend fun apply(record: SyncRecord) {
        if (record.deleted) {
            delete(record.entityType, record.entityId)
            return
        }
        val payload = record.payload ?: return
        when (record.entityType) {
            SyncEntityTypes.EXERCISES ->
                exerciseDao.upsert(json.decodeFromString(ExerciseEntity.serializer(), payload))
            SyncEntityTypes.ROUTINES ->
                routineDao.upsertRoutine(json.decodeFromString(RoutineEntity.serializer(), payload))
            SyncEntityTypes.ROUTINE_DAYS ->
                routineDao.upsertDay(json.decodeFromString(RoutineDayEntity.serializer(), payload))
            SyncEntityTypes.ROUTINE_EXERCISES ->
                routineDao.upsertDayExercise(json.decodeFromString(RoutineExerciseEntity.serializer(), payload))
            SyncEntityTypes.SESSIONS ->
                sessionDao.upsertSession(json.decodeFromString(SessionEntity.serializer(), payload))
            SyncEntityTypes.SETS ->
                sessionDao.upsertSet(json.decodeFromString(SetEntryEntity.serializer(), payload))
            SyncEntityTypes.BODY_METRICS -> {
                val metric = json.decodeFromString(BodyMetricEntity.serializer(), payload)
                bodyDao.deleteByDay(metric.epochDay)
                bodyDao.insert(metric)
            }
            SyncEntityTypes.HABIT_LOGS ->
                habitDao.insert(json.decodeFromString(HabitLogEntity.serializer(), payload))
        }
    }

    suspend fun delete(entityType: String, entityId: String) {
        when (entityType) {
            SyncEntityTypes.EXERCISES -> exerciseDao.deleteById(entityId)
            SyncEntityTypes.ROUTINES -> routineDao.deleteRoutine(entityId)
            SyncEntityTypes.ROUTINE_DAYS -> routineDao.deleteDay(entityId)
            SyncEntityTypes.ROUTINE_EXERCISES -> routineDao.deleteDayExercise(entityId)
            SyncEntityTypes.SESSIONS -> sessionDao.deleteSession(entityId)
            SyncEntityTypes.SETS -> sessionDao.deleteSet(entityId)
            SyncEntityTypes.BODY_METRICS -> bodyDao.deleteById(entityId)
            SyncEntityTypes.HABIT_LOGS -> habitDao.delete(entityId)
        }
    }
}

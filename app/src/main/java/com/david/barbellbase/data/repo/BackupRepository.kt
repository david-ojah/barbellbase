package com.david.barbellbase.data.repo

import androidx.room.withTransaction
import com.david.barbellbase.data.db.BodyDao
import com.david.barbellbase.data.db.BodyMetricEntity
import com.david.barbellbase.data.db.ExerciseDao
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.HabitDao
import com.david.barbellbase.data.db.HabitLogEntity
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.ReminderDao
import com.david.barbellbase.data.db.ReminderEventEntity
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.data.db.RoutineDao
import com.david.barbellbase.data.db.RoutineDayEntity
import com.david.barbellbase.data.db.RoutineEntity
import com.david.barbellbase.data.db.RoutineExerciseEntity
import com.david.barbellbase.data.db.SessionDao
import com.david.barbellbase.data.db.SessionEntity
import com.david.barbellbase.data.db.SetEntryEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineDays: List<RoutineDayEntity> = emptyList(),
    val routineExercises: List<RoutineExerciseEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val sets: List<SetEntryEntity> = emptyList(),
    val bodyMetrics: List<BodyMetricEntity> = emptyList(),
    val reminderRules: List<ReminderRuleEntity> = emptyList(),
    val reminderEvents: List<ReminderEventEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
) {
    val totalRecords: Int
        get() = exercises.size + routines.size + routineDays.size + routineExercises.size +
            sessions.size + sets.size + bodyMetrics.size + reminderRules.size +
            reminderEvents.size + habitLogs.size
}

@Singleton
class BackupRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val sessionDao: SessionDao,
    private val habitDao: HabitDao,
    private val reminderDao: ReminderDao,
    private val bodyDao: BodyDao,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun defaultFileName(): String {
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "barbellbase-backup-$date.json"
    }

    suspend fun exportJson(): String {
        val payload = BackupPayload(
            exercises = exerciseDao.getAll(),
            routines = routineDao.getAllRoutines(),
            routineDays = routineDao.getAllDays(),
            routineExercises = routineDao.getAllDayExercises(),
            sessions = sessionDao.getAllSessions(),
            sets = sessionDao.getAllSets(),
            bodyMetrics = bodyDao.getAll(),
            reminderRules = reminderDao.getAll(),
            reminderEvents = emptyList(),
            habitLogs = habitDao.getAll(),
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    suspend fun importJson(text: String): Result<BackupPayload> = runCatching {
        val payload = json.decodeFromString(BackupPayload.serializer(), text)
        require(payload.schemaVersion <= 1) { "Backup was created by a newer version of the app." }
        db.withTransaction {
            sessionDao.clearSets()
            sessionDao.clearSessions()
            routineDao.clearDayExercises()
            routineDao.clearDays()
            routineDao.clearRoutines()
            exerciseDao.clearAll()
            bodyDao.clearAll()
            reminderDao.clearEvents()
            reminderDao.clearAll()
            habitDao.clearAll()

            exerciseDao.insertAll(payload.exercises)
            routineDao.upsertRoutines(payload.routines)
            routineDao.upsertDays(payload.routineDays)
            routineDao.upsertDayExercises(payload.routineExercises)
            sessionDao.upsertSessions(payload.sessions)
            sessionDao.upsertSets(payload.sets)
            bodyDao.insertAll(payload.bodyMetrics)
            reminderDao.upsertAll(payload.reminderRules)
            habitDao.insertAll(payload.habitLogs)
        }
        payload
    }

    suspend fun snapshotForCsv(): CsvSnapshot = CsvSnapshot(
        sessionNames = sessionDao.getAllSessions().associate { it.id to it.name },
        exerciseNames = exerciseDao.getAll().associate { it.id to it.name },
        sets = sessionDao.getAllSets()
            .filter { it.completedAt != null }
            .sortedBy { it.completedAt ?: 0L },
    )
}

data class CsvSnapshot(
    val sessionNames: Map<String, String>,
    val exerciseNames: Map<String, String>,
    val sets: List<SetEntryEntity>,
)

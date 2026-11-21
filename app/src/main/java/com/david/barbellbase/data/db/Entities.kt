package com.david.barbellbase.data.db

import androidx.room.Entity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class Measurement { WEIGHT_REPS, REPS_ONLY, TIME }

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, OTHER }

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES, CORE, FULL_BODY;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

enum class SetType { NORMAL, WARMUP, DROP, FAILURE }

enum class ReminderKind { WATER, WALK, CUSTOM }

enum class AlertSound { ALARM, CHIME, SILENT }

enum class HabitKind { WATER, WALK }

enum class LogSource { NOTIFICATION, APP }

enum class ReminderAction { FIRED, DISMISSED, SNOOZED, LOGGED, IGNORED }

@Serializable
@Entity(tableName = "exercises", indices = [Index("name")])
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val primaryMuscles: List<MuscleGroup> = emptyList(),
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.OTHER,
    val measurement: Measurement = Measurement.WEIGHT_REPS,
    val instructions: String = "",
    val notes: String = "",
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val defaultRestSec: Int = 120,
)

@Serializable
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "routine_days",
    indices = [Index("routineId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineDayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val name: String,
    val position: Int,
)

@Serializable
@Entity(
    tableName = "routine_exercises",
    indices = [Index("dayId"), Index("exerciseId")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val targetWeightKg: Double? = null,
    val targetRpe: Double? = null,
    val restSec: Int = 120,
    val autoRest: Boolean = true,
)

@Serializable
@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineDayId: String? = null,
    val name: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val notes: String = "",
    val bodyweightKg: Double? = null,
)

@Serializable
@Entity(
    tableName = "set_entries",
    indices = [
        Index(value = ["sessionId", "exerciseId", "position"]),
        Index("exerciseId"),
        Index("completedAt"),
    ],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        ),
    ],
)
data class SetEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exerciseId: String,
    val exerciseOrder: Int = 0,
    val position: Int,
    val setType: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Double? = null,
    val durationSec: Int? = null,
    val restSec: Int? = null,
    val completedAt: Long? = null,
    val note: String = "",
)

@Serializable
@Entity(tableName = "body_metrics", indices = [Index(value = ["epochDay"], unique = true)])
data class BodyMetricEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val epochDay: Long,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val waistCm: Double? = null,
    val notes: String = "",
)

@Serializable
@Entity(tableName = "reminder_rules")
data class ReminderRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val kind: ReminderKind,
    val label: String,
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val windowStartMin: Int = 8 * 60,
    val windowEndMin: Int = 22 * 60,
    val daysOfWeek: Int = 0,
    val dailyCap: Int? = null,
    val snoozeMinutes: Int = 10,
    val sound: AlertSound = AlertSound.CHIME,
    val vibration: Boolean = true,
    val ringSeconds: Int = 60,
    val waterAmountMl: Int? = 250,
    val waterTargetMl: Int? = 2500,
    val walkSuggestMin: Int? = 10,
    val walkTargetMin: Int? = 30,
    val nextFireAt: Long? = null,
    val lastFiredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "reminder_events", indices = [Index("ruleId"), Index("firedAt")])
data class ReminderEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Int,
    val scheduledAt: Long,
    val firedAt: Long,
    val action: ReminderAction = ReminderAction.FIRED,
)

@Serializable
@Entity(tableName = "habit_logs", indices = [Index("kind"), Index("loggedAt")])
data class HabitLogEntity(
    @Serializable(with = HabitLogIdSerializer::class)
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: HabitKind,
    val amountMl: Int? = null,
    val durationMin: Int? = null,
    val loggedAt: Long = System.currentTimeMillis(),
    val source: LogSource = LogSource.APP,
    val note: String = "",
)

/**
 * Records local changes for cloud sync. Rows are written by SQLite triggers, so
 * every insert/update/delete is captured without instrumenting each repository.
 */
@Serializable
@Entity(tableName = "sync_changes", primaryKeys = ["entityType", "entityId"])
data class SyncChangeEntity(
    val entityType: String,
    val entityId: String,
    val changedAt: Long,
    val deleted: Boolean = false,
)

/**
 * Older backups stored habit log ids as numbers. Accept both forms so v1.0
 * exports can still be imported after ids became UUID strings.
 */
object HabitLogIdSerializer : kotlinx.serialization.json.JsonTransformingSerializer<String>(
    String.serializer(),
) {
    override fun transformDeserialize(
        element: kotlinx.serialization.json.JsonElement,
    ): kotlinx.serialization.json.JsonElement =
        if (element is kotlinx.serialization.json.JsonPrimitive && !element.isString) {
            kotlinx.serialization.json.JsonPrimitive(element.content)
        } else {
            element
        }
}

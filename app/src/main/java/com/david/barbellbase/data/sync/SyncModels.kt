package com.david.barbellbase.data.sync

data class SyncRecord(
    val entityType: String,
    val entityId: String,
    val payload: String?,
    val updatedAt: Long,
    val deleted: Boolean,
    val deviceId: String,
)

data class RemoteSettings(
    val payload: String,
    val updatedAt: Long,
)

data class SyncSummary(
    val uploaded: Int,
    val downloaded: Int,
)

object SyncEntityTypes {
    const val EXERCISES = "exercises"
    const val ROUTINES = "routines"
    const val ROUTINE_DAYS = "routine_days"
    const val ROUTINE_EXERCISES = "routine_exercises"
    const val SESSIONS = "sessions"
    const val SETS = "set_entries"
    const val BODY_METRICS = "body_metrics"
    const val HABIT_LOGS = "habit_logs"
}

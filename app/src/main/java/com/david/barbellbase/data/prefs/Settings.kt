package com.david.barbellbase.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class UnitSystem { KG, LB }

enum class ThemeMode { DYNAMIC, MONO, DARK, LIGHT }

/** Durable preferences that are safe to sync across devices. */
@Serializable
data class DurableSettings(
    val units: String = "KG",
    val theme: String = "DARK",
    val weekStartsMonday: Boolean = true,
    val showRpe: Boolean = false,
    val weightStepKg: Double = 2.5,
    val haptics: Boolean = true,
    val autoStartRest: Boolean = true,
    val defaultRestSec: Int = 120,
    val waterTargetMl: Int = 2500,
    val walkTargetMin: Int = 30,
)

data class AppSettings(
    val units: UnitSystem = UnitSystem.KG,
    val theme: ThemeMode = ThemeMode.DARK,
    val weekStartsMonday: Boolean = true,
    val showRpe: Boolean = false,
    val weightStepKg: Double = 2.5,
    val haptics: Boolean = true,
    val activeSessionId: String? = null,
    val restEndsAt: Long? = null,
    val restTotalSec: Int = 120,
    val autoStartRest: Boolean = true,
    val defaultRestSec: Int = 120,
    val waterTargetMl: Int = 2500,
    val walkTargetMin: Int = 30,
    val exactAlarmPrompted: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val units = stringPreferencesKey("units")
        val theme = stringPreferencesKey("theme")
        val weekMonday = booleanPreferencesKey("week_monday")
        val showRpe = booleanPreferencesKey("show_rpe")
        val weightStep = doublePreferencesKey("weight_step")
        val haptics = booleanPreferencesKey("haptics")
        val activeSession = stringPreferencesKey("active_session")
        val restEndsAt = intPreferencesKey("rest_ends_at")
        val restTotal = intPreferencesKey("rest_total")
        val autoRest = booleanPreferencesKey("auto_rest")
        val defaultRest = intPreferencesKey("default_rest")
        val waterTarget = intPreferencesKey("water_target")
        val walkTarget = intPreferencesKey("walk_target")
        val alarmPrompted = booleanPreferencesKey("alarm_prompted")
        val remindersSeeded = booleanPreferencesKey("reminders_seeded")
        val deviceId = stringPreferencesKey("sync_device_id")
        val lastSyncedAt = longPreferencesKey("sync_last_synced_at")
        val lastSettingsAt = longPreferencesKey("sync_last_settings_at")
        val settingsChangedAt = longPreferencesKey("settings_changed_at")
    }

    // Stored as Long split across two ints would be overkill; DataStore supports Long via longPreferencesKey.
    private val restEndsAtKey = androidx.datastore.preferences.core.longPreferencesKey("rest_ends_at_ms")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            units = prefs[Keys.units]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.KG,
            theme = prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            weekStartsMonday = prefs[Keys.weekMonday] ?: true,
            showRpe = prefs[Keys.showRpe] ?: false,
            weightStepKg = prefs[Keys.weightStep] ?: 2.5,
            haptics = prefs[Keys.haptics] ?: true,
            activeSessionId = prefs[Keys.activeSession],
            restEndsAt = prefs[restEndsAtKey],
            restTotalSec = prefs[Keys.restTotal] ?: 120,
            autoStartRest = prefs[Keys.autoRest] ?: true,
            defaultRestSec = prefs[Keys.defaultRest] ?: 120,
            waterTargetMl = prefs[Keys.waterTarget] ?: 2500,
            walkTargetMin = prefs[Keys.walkTarget] ?: 30,
            exactAlarmPrompted = prefs[Keys.alarmPrompted] ?: false,
        )
    }

    suspend fun setUnits(value: UnitSystem) = durableEdit { it[Keys.units] = value.name }
    suspend fun setTheme(value: ThemeMode) = durableEdit { it[Keys.theme] = value.name }
    suspend fun setWeekStartsMonday(value: Boolean) = durableEdit { it[Keys.weekMonday] = value }
    suspend fun setShowRpe(value: Boolean) = durableEdit { it[Keys.showRpe] = value }
    suspend fun setWeightStep(kg: Double) = durableEdit { it[Keys.weightStep] = kg }
    suspend fun setHaptics(value: Boolean) = durableEdit { it[Keys.haptics] = value }
    suspend fun setAutoStartRest(value: Boolean) = durableEdit { it[Keys.autoRest] = value }
    suspend fun setDefaultRest(sec: Int) = durableEdit { it[Keys.defaultRest] = sec }
    suspend fun setWaterTarget(ml: Int) = durableEdit { it[Keys.waterTarget] = ml }
    suspend fun setWalkTarget(min: Int) = durableEdit { it[Keys.walkTarget] = min }
    suspend fun setAlarmPrompted(value: Boolean) = edit { it[Keys.alarmPrompted] = value }

    suspend fun isRemindersSeeded(): Boolean =
        context.settingsStore.data.map { it[Keys.remindersSeeded] ?: false }.first()

    suspend fun markRemindersSeeded() = edit { it[Keys.remindersSeeded] = true }

    suspend fun setActiveSession(id: String?) = edit {
        if (id == null) it.remove(Keys.activeSession) else it[Keys.activeSession] = id
    }

    suspend fun setRest(endsAt: Long?, totalSec: Int) = edit {
        if (endsAt == null) it.remove(restEndsAtKey) else it[restEndsAtKey] = endsAt
        it[Keys.restTotal] = totalSec
    }

    suspend fun deviceId(): String {
        val existing = context.settingsStore.data.map { it[Keys.deviceId] }.first()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        edit { it[Keys.deviceId] = generated }
        return generated
    }

    suspend fun lastSyncedAt(): Long =
        context.settingsStore.data.map { it[Keys.lastSyncedAt] ?: 0L }.first()

    suspend fun setLastSyncedAt(value: Long) = edit { it[Keys.lastSyncedAt] = value }

    suspend fun lastSyncedSettingsAt(): Long =
        context.settingsStore.data.map { it[Keys.lastSettingsAt] ?: 0L }.first()

    suspend fun setLastSyncedSettingsAt(value: Long) = edit { it[Keys.lastSettingsAt] = value }

    suspend fun settingsChangedAt(): Long =
        context.settingsStore.data.map { it[Keys.settingsChangedAt] ?: 0L }.first()

    suspend fun durableSettings(): DurableSettings {
        val current = settings.first()
        return DurableSettings(
            units = current.units.name,
            theme = current.theme.name,
            weekStartsMonday = current.weekStartsMonday,
            showRpe = current.showRpe,
            weightStepKg = current.weightStepKg,
            haptics = current.haptics,
            autoStartRest = current.autoStartRest,
            defaultRestSec = current.defaultRestSec,
            waterTargetMl = current.waterTargetMl,
            walkTargetMin = current.walkTargetMin,
        )
    }

    suspend fun applyDurableSettings(value: DurableSettings) {
        edit {
            it[Keys.units] = value.units
            it[Keys.theme] = value.theme
            it[Keys.weekMonday] = value.weekStartsMonday
            it[Keys.showRpe] = value.showRpe
            it[Keys.weightStep] = value.weightStepKg
            it[Keys.haptics] = value.haptics
            it[Keys.autoRest] = value.autoStartRest
            it[Keys.defaultRest] = value.defaultRestSec
            it[Keys.waterTarget] = value.waterTargetMl
            it[Keys.walkTarget] = value.walkTargetMin
        }
    }

    private suspend fun durableEdit(
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        edit {
            block(it)
            it[Keys.settingsChangedAt] = System.currentTimeMillis()
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }
}

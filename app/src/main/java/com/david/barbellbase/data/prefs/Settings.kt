package com.david.barbellbase.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class UnitSystem { KG, LB }

enum class ThemeMode { DYNAMIC, MONO, DARK, LIGHT }

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

    suspend fun setUnits(value: UnitSystem) = edit { it[Keys.units] = value.name }
    suspend fun setTheme(value: ThemeMode) = edit { it[Keys.theme] = value.name }
    suspend fun setWeekStartsMonday(value: Boolean) = edit { it[Keys.weekMonday] = value }
    suspend fun setShowRpe(value: Boolean) = edit { it[Keys.showRpe] = value }
    suspend fun setWeightStep(kg: Double) = edit { it[Keys.weightStep] = kg }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.haptics] = value }
    suspend fun setAutoStartRest(value: Boolean) = edit { it[Keys.autoRest] = value }
    suspend fun setDefaultRest(sec: Int) = edit { it[Keys.defaultRest] = sec }
    suspend fun setWaterTarget(ml: Int) = edit { it[Keys.waterTarget] = ml }
    suspend fun setWalkTarget(min: Int) = edit { it[Keys.walkTarget] = min }
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

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }
}

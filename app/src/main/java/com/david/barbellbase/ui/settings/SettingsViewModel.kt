package com.david.barbellbase.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.db.BodyMetricEntity
import com.david.barbellbase.data.prefs.AppSettings
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.ThemeMode
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.data.repo.BackupRepository
import com.david.barbellbase.data.repo.BodyRepository
import com.david.barbellbase.data.repo.ResetRepository
import com.david.barbellbase.domain.Units
import com.david.barbellbase.reminder.NotificationChannels
import com.david.barbellbase.reminder.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val latestBody: BodyMetricEntity? = null,
    val exactAlarm: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val backupName: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val bodyRepository: BodyRepository,
    private val backupRepository: BackupRepository,
    private val reminderRepository: ReminderRepository,
    private val resetRepository: ResetRepository,
) : ViewModel() {

    private val flags = MutableStateFlow(Triple(false, true, backupRepository.defaultFileName()))
    private val message = MutableStateFlow<String?>(null)

    val userMessage = message

    val state = combine(
        settingsRepository.settings,
        bodyRepository.observeLatest(),
        flags,
    ) { settings, body, current ->
        SettingsState(
            settings = settings,
            latestBody = body,
            exactAlarm = current.first,
            notificationsEnabled = current.second,
            backupName = current.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun refreshPermissions() {
        flags.value = Triple(
            reminderRepository.hasExactAlarmAccess(),
            NotificationChannels.canPost(context),
            backupRepository.defaultFileName(),
        )
    }

    fun consumeMessage() {
        message.value = null
    }

    fun setUnits(value: UnitSystem) = launch { settingsRepository.setUnits(value) }
    fun setTheme(value: ThemeMode) = launch { settingsRepository.setTheme(value) }
    fun setWeekStartsMonday(value: Boolean) = launch { settingsRepository.setWeekStartsMonday(value) }
    fun setShowRpe(value: Boolean) = launch { settingsRepository.setShowRpe(value) }
    fun setWeightStep(step: Double) = launch { settingsRepository.setWeightStep(step) }
    fun setHaptics(value: Boolean) = launch { settingsRepository.setHaptics(value) }
    fun setAutoStartRest(value: Boolean) = launch { settingsRepository.setAutoStartRest(value) }
    fun setDefaultRest(seconds: Int) = launch { settingsRepository.setDefaultRest(seconds) }
    fun setWaterTarget(ml: Int) = launch { settingsRepository.setWaterTarget(ml) }
    fun setWalkTarget(minutes: Int) = launch { settingsRepository.setWalkTarget(minutes) }

    fun logBodyWeight(input: String) {
        val units = state.value.settings.units
        val value = input.replace(',', '.').toDoubleOrNull() ?: return
        if (value <= 0) return
        launch {
            bodyRepository.save(weightKg = Units.toKg(value, units))
            message.value = "Bodyweight saved"
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read the selected file")
                backupRepository.importJson(text).getOrThrow()
            }.onSuccess { payload ->
                // Re-read the imported rules so their next-fire times and armed alarms are current,
                // instead of whatever the backup file happened to store.
                reminderRepository.rescheduleAll()
                message.value = "Imported ${payload.totalRecords} records"
            }.onFailure { error ->
                message.value = "Import failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    /** Clears logged data only: sessions, sets, bodyweight, habit logs and reminder history. */
    fun resetData() {
        viewModelScope.launch {
            runCatching {
                resetRepository.resetTrackedData()
            }.onSuccess {
                message.value = "All tracked data cleared"
            }.onFailure { error ->
                message.value = "Reset failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = backupRepository.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open the selected file")
                json.length
            }.onSuccess { size ->
                message.value = "Exported ${size / 1024} KB of JSON"
            }.onFailure { error ->
                message.value = "Export failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    /** CSV is a convenience export for spreadsheets: one row per completed set. */
    fun exportCsvTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val snapshot = backupRepository.snapshotForCsv()
                val rows = StringBuilder()
                rows.append("session,exercise,set,weight_kg,reps,rpe,completed_at\n")
                snapshot.sets.forEach { set ->
                    rows.append(csv(snapshot.sessionNames[set.sessionId] ?: ""))
                    rows.append(',')
                    rows.append(csv(snapshot.exerciseNames[set.exerciseId] ?: ""))
                    rows.append(',')
                    rows.append(set.position + 1)
                    rows.append(',')
                    rows.append(set.weightKg ?: "")
                    rows.append(',')
                    rows.append(set.reps ?: "")
                    rows.append(',')
                    rows.append(set.rpe ?: "")
                    rows.append(',')
                    rows.append(set.completedAt ?: "")
                    rows.append('\n')
                }
                val csv = rows.toString()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open the selected file")
                csv.length
            }.onSuccess { size ->
                message.value = "Exported $size bytes of CSV"
            }.onFailure { error ->
                message.value = "CSV export failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

package com.david.barbellbase.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.data.prefs.ThemeMode
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.domain.Units
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val message by viewModel.userMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var bodyInput by remember(state.settings.units) { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? -> uri?.let(viewModel::exportTo) }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? -> uri?.let(viewModel::exportCsvTo) }

    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(viewModel::importFrom) }

    val googleSignIn = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> viewModel.handleSignInResult(result.data) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    SectionTitle("Units")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("Kilograms", selected = state.settings.units == UnitSystem.KG, onClick = { viewModel.setUnits(UnitSystem.KG) })
                        Pill("Pounds", selected = state.settings.units == UnitSystem.LB, onClick = { viewModel.setUnits(UnitSystem.LB) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Weights are stored in kilograms internally, so switching units never converts your history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    SectionTitle("Training")
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show RPE", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Adds an optional effort field to the set editor.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.settings.showRpe, onCheckedChange = viewModel::setShowRpe)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto-start rest timer", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Completing a set starts the countdown automatically.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.settings.autoStartRest, onCheckedChange = viewModel::setAutoStartRest)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Haptics", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Vibrate on set completion.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.settings.haptics, onCheckedChange = viewModel::setHaptics)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Default rest", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(60, 90, 120, 150, 180).forEach { seconds ->
                            Pill(
                                "${seconds}s",
                                selected = state.settings.defaultRestSec == seconds,
                                onClick = { viewModel.setDefaultRest(seconds) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Weight step", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0, 2.5, 5.0).forEach { step ->
                            Pill(
                                "${Units.format(step, state.settings.units)} ${Units.label(state.settings.units)}",
                                selected = state.settings.weightStepKg == step,
                                onClick = { viewModel.setWeightStep(step) },
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("Appearance")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            Pill(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = state.settings.theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Week starts Monday", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Affects the weekly charts and streak boundaries.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.settings.weekStartsMonday, onCheckedChange = viewModel::setWeekStartsMonday)
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("Reminder defaults")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.settings.waterTargetMl.toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let(viewModel::setWaterTarget)
                        },
                        label = { Text("Daily water target (ml)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.settings.walkTargetMin.toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let(viewModel::setWalkTarget)
                        },
                        label = { Text("Daily walking target (min)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    StatusLine(
                        label = "Exact alarms",
                        value = if (state.exactAlarm) "Allowed" else "Not allowed",
                        active = state.exactAlarm,
                    )
                    if (!state.exactAlarm) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"),
                                )
                                runCatching { context.startActivity(intent) }
                            },
                        ) { Text("Allow exact alarms") }
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusLine(
                        label = "Notifications",
                        value = if (state.notificationsEnabled) "Allowed" else "Blocked",
                        active = state.notificationsEnabled,
                    )
                    if (!state.notificationsEnabled) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                runCatching { context.startActivity(intent) }
                            },
                        ) { Text("Open notification settings") }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = viewModel::refreshPermissions) { Text("Re-check permissions") }
                }
            }

            item {
                AppCard {
                    SectionTitle("Body")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Latest: " + (state.latestBody?.let {
                            "${Units.format(it.weightKg, state.settings.units)} ${Units.label(state.settings.units)}"
                        } ?: "not logged"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bodyInput,
                            onValueChange = { bodyInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            label = { Text("Today (${Units.label(state.settings.units)})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.logBodyWeight(bodyInput)
                                bodyInput = ""
                            },
                            enabled = bodyInput.isNotBlank(),
                        ) { Text("Save") }
                    }
                }
            }

            if (sync.configured) {
                item {
                    AppCard {
                        SectionTitle("Cloud sync")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Sync workouts, routines and settings across every device signed in with the same Google account. The app remains fully usable offline.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (sync.account == null) {
                            OutlinedButton(
                                onClick = { viewModel.signInIntent()?.let(googleSignIn::launch) },
                                enabled = !sync.busy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (sync.busy) "Signing in..." else "Sign in with Google")
                            }
                        } else {
                            Text(
                                "Signed in as ${sync.account}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Last sync: " + if (sync.lastSyncedAt > 0L) {
                                    android.text.format.DateUtils.getRelativeTimeSpanString(sync.lastSyncedAt)
                                } else {
                                    "never"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::syncNow,
                                    enabled = !sync.busy,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (sync.busy) "Syncing..." else "Sync now")
                                }
                                TextButton(onClick = viewModel::signOut) { Text("Sign out") }
                            }
                        }
                        sync.error?.let { error ->
                            Spacer(Modifier.height(6.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("Data")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Everything lives on this phone. Export a JSON backup before reinstalling, or a CSV to chart your sets elsewhere.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportJson.launch(state.backupName.ifBlank { "barbellbase-backup.json" }) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Export JSON") }
                        OutlinedButton(
                            onClick = { exportCsv.launch("barbellbase-sets.csv") },
                            modifier = Modifier.weight(1f),
                        ) { Text("Export CSV") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importJson.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Import JSON (replaces everything)") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Reset tracked data") }
                }
            }

            item {
                AppCard {
                    SectionTitle("About")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "BarbellBase 1.0 · offline strength tracker for Android",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Offline by default. Optional Google sync. No analytics. Uninstalling removes local data.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all tracked data?") },
            text = {
                Text(
                    "This permanently deletes every workout session and set, bodyweight entry, " +
                        "water/walk log and reminder history. Your exercise library, routines, " +
                        "reminder rules and settings stay. It cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.resetData()
                    },
                ) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

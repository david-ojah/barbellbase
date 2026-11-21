package com.david.barbellbase.ui.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.data.db.AlertSound
import com.david.barbellbase.data.db.ReminderKind
import com.david.barbellbase.data.db.ReminderRuleEntity
import com.david.barbellbase.domain.ReminderSchedule
import com.david.barbellbase.domain.TimeFormat
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.EmptyState
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.SectionTitle
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ReminderRuleEntity?>(null) }
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val exactAlarm = state.exactAlarm || runCatching {
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }.getOrDefault(false)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                actions = {
                    TextButton(onClick = { editing = viewModel.newRuleTemplate() }) {
                        androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!notificationsEnabled) {
                item {
                    AppCard {
                        Text("Notifications are blocked", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Reminders cannot appear until you allow notifications for BarbellBase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                runCatching { context.startActivity(intent) }
                                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                            },
                        ) { Text("Open notification settings") }
                    }
                }
            }

            if (!exactAlarm) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Exact alarms are off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Android will batch reminders to save battery, so they can arrive late. Allow exact alarms for on-time rings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"),
                                )
                                runCatching { context.startActivity(intent) }
                            },
                        ) { Text("Allow exact alarms") }
                    }
                }
            }

            item {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Water today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.waterTodayMl} ml", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Walking today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.walkTodayMin} min", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (state.rules.isEmpty()) {
                item { EmptyState("No reminders", "Add a water alarm or a walking chime to get started.") }
            }
            items(state.rules, key = { it.id }) { rule ->
                AppCard(onClick = { editing = rule }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(rule.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Pill(ReminderText.sound(rule.sound))
                            }
                            Text(
                                ReminderText.cadence(rule),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (!rule.enabled) "Off"
                                else rule.nextFireAt?.let { "Next ${TimeFormat.day(it)} ${TimeFormat.clock(it)}" } ?: "Not scheduled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggle(rule, it) })
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("How it works")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Water rules ring with the real alarm sound and keep ringing until you dismiss them or the ring time runs out. " +
                            "Walking rules play a short chime and disappear on their own. " +
                            "Every rule only fires inside its window, on the days you pick, and re-arms itself after each ring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    editing?.let { rule ->
        ReminderEditorSheet(
            initial = rule,
            onDismiss = { editing = null },
            onSave = { updated -> viewModel.save(updated); editing = null },
            onTest = { if (rule.id != 0) viewModel.test(rule.id) },
            onDelete = { if (rule.id != 0) { viewModel.delete(rule.id); editing = null } },
        )
    }
}

private object ReminderText {
    fun sound(sound: AlertSound): String = when (sound) {
        AlertSound.ALARM -> "Alarm"
        AlertSound.CHIME -> "Chime"
        AlertSound.SILENT -> "Silent"
    }

    fun cadence(rule: ReminderRuleEntity): String {
        val window = "${clockOf(rule.windowStartMin)}–${clockOf(rule.windowEndMin)}"
        val days = if (rule.daysOfWeek == 0) "every day" else daysLabel(rule.daysOfWeek)
        val cap = rule.dailyCap?.let { " · max $it/day" }.orEmpty()
        return "Every ${rule.intervalMinutes} min · $window · $days$cap"
    }

    fun clockOf(minutes: Int): String =
        String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    fun daysLabel(mask: Int): String {
        if (mask == 0) return "every day"
        val days = ReminderSchedule.daysFromMask(mask)
        if (days.size == 7) return "every day"
        val order = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        return order.filter { days.contains(it) }
            .joinToString(" ") { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorSheet(
    initial: ReminderRuleEntity,
    onDismiss: () -> Unit,
    onSave: (ReminderRuleEntity) -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
) {
    var rule by remember(initial.id, initial.label) { mutableStateOf(initial) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).imePadding()) {
            Text(if (initial.id == 0) "New reminder" else "Edit reminder", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = rule.label,
                onValueChange = { rule = rule.copy(label = it) },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))
            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderKind.entries.forEach { kind ->
                    Pill(
                        text = kind.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = rule.kind == kind,
                        onClick = { rule = rule.copy(kind = kind) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Sound", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertSound.entries.forEach { sound ->
                    Pill(
                        text = ReminderText.sound(sound),
                        selected = rule.sound == sound,
                        onClick = { rule = rule.copy(sound = sound) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rule.intervalMinutes.toString(),
                    onValueChange = { input ->
                        input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(intervalMinutes = it.coerceIn(5, 720)) }
                    },
                    label = { Text("Every (min)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = (rule.dailyCap ?: 0).toString(),
                    onValueChange = { input ->
                        val value = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                        rule = rule.copy(dailyCap = if (value <= 0) null else value.coerceIn(1, 48))
                    },
                    label = { Text("Max/day (0 = ∞)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                    Text("From ${ReminderText.clockOf(rule.windowStartMin)}")
                }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                    Text("To ${ReminderText.clockOf(rule.windowEndMin)}")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Days", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val order = listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
                )
                val mask = rule.daysOfWeek
                items(order.map { it }) { day ->
                    val selected = mask == 0 || (mask and (1 shl (day.value - 1))) != 0
                    Pill(
                        text = day.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                        selected = selected,
                        onClick = {
                            val bit = 1 shl (day.value - 1)
                            val current = if (mask == 0) 0x7F else mask
                            val toggled = current xor bit
                            rule = rule.copy(daysOfWeek = if (toggled == 0x7F || toggled == 0) 0 else toggled)
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rule.snoozeMinutes.toString(),
                    onValueChange = { input ->
                        input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(snoozeMinutes = it.coerceIn(1, 120)) }
                    },
                    label = { Text("Snooze (min)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = rule.ringSeconds.toString(),
                    onValueChange = { input ->
                        input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(ringSeconds = it.coerceIn(0, 300)) }
                    },
                    label = { Text("Ring (sec)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            if (rule.kind == ReminderKind.WATER) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = (rule.waterAmountMl ?: 250).toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(waterAmountMl = it.coerceIn(50, 1000)) }
                        },
                        label = { Text("Amount (ml)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = (rule.waterTargetMl ?: 2500).toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(waterTargetMl = it.coerceIn(250, 8000)) }
                        },
                        label = { Text("Daily target (ml)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (rule.kind == ReminderKind.WALK) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = (rule.walkSuggestMin ?: 10).toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(walkSuggestMin = it.coerceIn(1, 180)) }
                        },
                        label = { Text("Suggest (min)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = (rule.walkTargetMin ?: 30).toString(),
                        onValueChange = { input ->
                            input.filter { it.isDigit() }.toIntOrNull()?.let { rule = rule.copy(walkTargetMin = it.coerceIn(5, 300)) }
                        },
                        label = { Text("Daily target (min)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enabled", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Off removes the scheduled alarm without deleting the rule.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rule.enabled, onCheckedChange = { rule = rule.copy(enabled = it) })
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Vibrate", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Used when the phone is silent, and as a fallback when alarm audio cannot play.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rule.vibration, onCheckedChange = { rule = rule.copy(vibration = it) })
            }

            Spacer(Modifier.height(14.dp))
            Button(onClick = { onSave(rule) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (initial.id == 0) "Add reminder" else "Save")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTest, enabled = initial.id != 0, modifier = Modifier.weight(1f)) {
                    Text("Test in 10s")
                }
                OutlinedButton(onClick = onDelete, enabled = initial.id != 0, modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (pickingStart) {
        TimeWindowDialog(
            initialMinutes = rule.windowStartMin,
            title = "Window start",
            onDismiss = { pickingStart = false },
            onPick = { minutes -> rule = rule.copy(windowStartMin = minutes); pickingStart = false },
        )
    }
    if (pickingEnd) {
        TimeWindowDialog(
            initialMinutes = rule.windowEndMin,
            title = "Window end",
            onDismiss = { pickingEnd = false },
            onPick = { minutes -> rule = rule.copy(windowEndMin = minutes); pickingEnd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowDialog(
    initialMinutes: Int,
    title: String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = (initialMinutes % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

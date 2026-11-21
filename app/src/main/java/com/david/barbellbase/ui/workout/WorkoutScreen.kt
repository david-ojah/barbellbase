package com.david.barbellbase.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.data.db.ExerciseEntity
import com.david.barbellbase.data.db.Measurement
import com.david.barbellbase.data.db.SetEntryEntity
import com.david.barbellbase.data.db.SetType
import com.david.barbellbase.domain.TimeFormat
import com.david.barbellbase.domain.Units
import com.david.barbellbase.data.prefs.UnitSystem
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.ProgressRing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editing by viewModel.editingSet.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.workout?.session?.name ?: "Workout", style = MaterialTheme.typography.titleMedium)
                        Text(
                            TimeFormat.duration(state.elapsedSec),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showDiscard = true }) { Icon(Icons.Default.Close, contentDescription = "Discard") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exercise")
                    }
                    Button(
                        onClick = { viewModel.finish(onBack) },
                        modifier = Modifier.weight(1f),
                        enabled = state.doneSets > 0,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Finish")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "${state.doneSets} / ${state.totalSets} sets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Volume ${Units.format(state.volumeKg, state.units)} ${Units.label(state.units)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ProgressRing(
                        progress = if (state.totalSets > 0) state.doneSets.toFloat() / state.totalSets else 0f,
                        size = 52.dp,
                        strokeWidth = 6.dp,
                    ) {
                        Text("${if (state.totalSets > 0) state.doneSets * 100 / state.totalSets else 0}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            state.rest?.let { rest ->
                item {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Rest", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    TimeFormat.duration(rest.remainingSeconds.toLong()),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.extendRest(15) }) { Text("+15s") }
                                TextButton(onClick = { viewModel.skipRest() }) { Text("Skip") }
                            }
                        }
                    }
                }
            }

            if (state.workout?.blocks.isNullOrEmpty()) {
                item {
                    AppCard {
                        Text("No exercises yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add your first exercise to start logging sets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(state.workout?.blocks.orEmpty(), key = { it.exercise.id }) { block ->
                var menuOpen by remember { mutableStateOf(false) }
                val previous = state.previous[block.exercise.id].orEmpty()
                AppCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(block.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                block.exercise.primaryMuscles.joinToString { it.label },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Exercise menu") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Add set") },
                                    onClick = { menuOpen = false; viewModel.addSet(block.exercise.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove exercise") },
                                    onClick = { menuOpen = false; viewModel.removeExercise(block.exercise.id) },
                                )
                            }
                        }
                    }

                    if (previous.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Last: " + previous.joinToString(", ") { set ->
                                when {
                                    set.weightKg != null && set.reps != null ->
                                        "${Units.format(set.weightKg, state.units)}×${set.reps}"
                                    set.reps != null -> "${set.reps}"
                                    set.durationSec != null -> "${set.durationSec}s"
                                    else -> "—"
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    block.sets.forEachIndexed { index, set ->
                        SetRow(
                            index = index,
                            set = set,
                            units = state.units,
                            measurement = block.exercise.measurement,
                            onClick = { viewModel.edit(set) },
                            onToggle = {
                                if (set.completedAt == null) {
                                    viewModel.completeQuick(set, set.weightKg, set.reps)
                                } else {
                                    viewModel.edit(set)
                                }
                            },
                        )
                        if (index != block.sets.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = state.allExercises,
            onDismiss = { showPicker = false },
            onPick = { exercise ->
                viewModel.addExercise(exercise.id)
                showPicker = false
            },
        )
    }

    editing?.let { set ->
        val exercise = state.workout?.blocks?.firstOrNull { it.exercise.id == set.exerciseId }?.exercise
        SetEditorSheet(
            set = set,
            units = state.units,
            showRpe = state.showRpe,
            measurement = exercise?.measurement ?: Measurement.WEIGHT_REPS,
            onDismiss = viewModel::dismissEditor,
            onSave = { weight, reps, rpe, type, complete ->
                viewModel.saveSet(set, weight, reps, rpe, type, complete)
            },
            onDelete = { viewModel.deleteSet(set.id) },
        )
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("This session and its logged sets will be deleted.") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; viewModel.discard(onBack) }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun SetRow(
    index: Int,
    set: SetEntryEntity,
    units: UnitSystem,
    measurement: Measurement,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val completed = set.completedAt != null
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
        )
        if (set.setType != SetType.NORMAL) {
            Text(
                set.setType.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.width(64.dp),
            )
        }
        Text(
            when {
                measurement == Measurement.TIME -> set.durationSec?.let { "${it}s" } ?: "—"
                set.weightKg != null && set.reps != null ->
                    "${Units.format(set.weightKg, units)} ${Units.label(units)} × ${set.reps}"
                set.reps != null -> "${set.reps} reps"
                else -> "—"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        set.rpe?.let {
            Text("RPE ${it.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onToggle) {
            Icon(
                Icons.Default.Check,
                contentDescription = if (completed) "Completed" else "Complete set",
                tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetEditorSheet(
    set: SetEntryEntity,
    units: UnitSystem,
    showRpe: Boolean,
    measurement: Measurement,
    onDismiss: () -> Unit,
    onSave: (String, String, String, SetType, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var weight by remember(set.id) {
        mutableStateOf(set.weightKg?.let { trimNumber(Units.fromKg(it, units)) } ?: "")
    }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }
    var rpe by remember(set.id) { mutableStateOf(set.rpe?.let { trimNumber(it) } ?: "") }
    var type by remember(set.id) { mutableStateOf(set.setType) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp).imePadding()) {
            Text("Set ${set.position + 1}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (measurement != Measurement.TIME) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Weight (${Units.label(units)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = if (measurement == Measurement.TIME) reps else reps,
                    onValueChange = { reps = it.filter { c -> c.isDigit() } },
                    label = { Text(if (measurement == Measurement.TIME) "Seconds" else "Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            if (showRpe) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rpe,
                    onValueChange = { rpe = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("RPE (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SetType.entries.forEach { option ->
                    Pill(
                        text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = type == option,
                        onClick = { type = option },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(weight, reps, rpe, type, true) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (set.completedAt == null) "Complete set" else "Update set") }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { onSave(weight, reps, rpe, type, false) },
                    modifier = Modifier.weight(1f),
                ) { Text("Save for later") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises
        else exercises.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.primaryMuscles.any { m -> m.label.contains(query, ignoreCase = true) }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Add exercise", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search exercises") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(420.dp)) {
                items(filtered, key = { it.id }) { exercise ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                exercise.primaryMuscles.joinToString { it.label } + " · " +
                                    exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

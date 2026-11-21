package com.david.barbellbase.ui.routines

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.data.db.RoutineExerciseEntity
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.EmptyState
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onOpenRoutine: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New routine")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.summaries.isEmpty()) {
                item {
                    EmptyState(
                        title = "No routines yet",
                        message = "A routine is a list of days. Each day is a list of exercises with set and rep targets.",
                        action = { Button(onClick = { showCreate = true }) { Text("Create your first routine") } },
                    )
                }
            }
            items(state.summaries, key = { it.routine.id }) { summary ->
                AppCard(onClick = { onOpenRoutine(summary.routine.id) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${summary.days.size} day${if (summary.days.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    if (summary.days.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        summary.days.take(4).forEach { day ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(day.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${summary.setTargets[day.id] ?: 0} exercises",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New routine") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreate = false
                        viewModel.create(newName) { id -> newName = ""; onOpenRoutine(id) }
                    },
                    enabled = newName.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    onOpenDay: (String) -> Unit,
    onStartDay: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRename by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menu = false; name = state.routine?.name.orEmpty(); showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete routine") },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = { viewModel.addDay { id -> onOpenDay(id) } }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add day")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Days", "${state.days.size}") }
            items(state.days, key = { it.id }) { day ->
                var menu by remember { mutableStateOf(false) }
                AppCard(onClick = { onOpenDay(day.id) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(day.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${state.exerciseCounts[day.id] ?: 0} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.startDay(day.id, onStartDay) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start day")
                        }
                        Column {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Day menu") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Move up") },
                                    onClick = { menu = false; viewModel.moveDay(day.id, -1) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move down") },
                                    onClick = { menu = false; viewModel.moveDay(day.id, 1) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete day") },
                                    onClick = { menu = false; viewModel.deleteDay(day.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename routine") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = { showRename = false; viewModel.rename(name) }, enabled = name.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete routine?") },
            text = { Text("Days and exercise targets in this routine are removed. Logged sessions are kept.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteRoutine(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    onBack: () -> Unit,
    viewModel: DayEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineExerciseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.day?.name ?: "Day")
                        Text(
                            state.routineName ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = { showPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exercise")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        title = "No exercises",
                        message = "Add exercises with target sets and rep ranges. Starting this day pre-fills every set.",
                    )
                }
            }
            items(state.items, key = { it.entry.id }) { row ->
                AppCard(onClick = { editing = row.entry }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                row.exercise?.name ?: "Unknown exercise",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${row.entry.targetSets} × ${row.entry.repsMin}-${row.entry.repsMax} · rest ${row.entry.restSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.move(row.entry.id, -1) }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(onClick = { viewModel.move(row.entry.id, 1) }) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                        }
                        IconButton(onClick = { viewModel.remove(row.entry.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerForRoutine(
            exercises = state.exercises,
            onDismiss = { showPicker = false },
            onPick = { exercise ->
                viewModel.addExercise(exercise)
                showPicker = false
            },
        )
    }

    editing?.let { entry ->
        TargetEditorSheet(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { updated -> viewModel.update(updated); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerForRoutine(
    exercises: List<com.david.barbellbase.data.db.ExerciseEntity>,
    onDismiss: () -> Unit,
    onPick: (com.david.barbellbase.data.db.ExerciseEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, exercises) {
        if (query.isBlank()) exercises else exercises.filter { it.name.contains(query, ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Add exercise", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.height(400.dp)) {
                items(filtered, key = { it.id }) { exercise ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                exercise.primaryMuscles.joinToString { it.label },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetEditorSheet(
    entry: RoutineExerciseEntity,
    onDismiss: () -> Unit,
    onSave: (RoutineExerciseEntity) -> Unit,
) {
    var sets by remember { mutableStateOf(entry.targetSets.toString()) }
    var repsMin by remember { mutableStateOf(entry.repsMin.toString()) }
    var repsMax by remember { mutableStateOf(entry.repsMax.toString()) }
    var rest by remember { mutableStateOf(entry.restSec.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Targets", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Sets", sets, { sets = it }, Modifier.weight(1f))
                NumberField("Reps min", repsMin, { repsMin = it }, Modifier.weight(1f))
                NumberField("Reps max", repsMax, { repsMax = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("Rest (s)", rest, { rest = it }, Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Text("Quick rest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(60, 90, 120, 180).forEach { option ->
                            Pill("$option", selected = rest == option.toString(), onClick = { rest = option.toString() })
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        entry.copy(
                            targetSets = sets.toIntOrNull()?.coerceIn(1, 20) ?: entry.targetSets,
                            repsMin = repsMin.toIntOrNull()?.coerceIn(1, 100) ?: entry.repsMin,
                            repsMax = repsMax.toIntOrNull()?.coerceIn(1, 100) ?: entry.repsMax,
                            restSec = rest.toIntOrNull()?.coerceIn(0, 900) ?: entry.restSec,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save targets") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

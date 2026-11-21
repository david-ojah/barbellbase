package com.david.barbellbase.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.david.barbellbase.data.db.Equipment
import com.david.barbellbase.data.db.Measurement
import com.david.barbellbase.data.db.MuscleGroup
import com.david.barbellbase.domain.TimeFormat
import com.david.barbellbase.domain.Units
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.EmptyState
import com.david.barbellbase.ui.components.LineChart
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.SectionTitle
import com.david.barbellbase.ui.components.StatTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onOpenExercise: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, contentDescription = "New exercise") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search exercises") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.muscle == null,
                            onClick = { viewModel.setMuscle(null) },
                            label = { Text("All") },
                        )
                    }
                    items(MuscleGroup.entries.toList()) { group ->
                        FilterChip(
                            selected = state.muscle == group,
                            onClick = { viewModel.setMuscle(if (state.muscle == group) null else group) },
                            label = { Text(group.label) },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
            }
            if (state.exercises.isEmpty()) {
                item { EmptyState("No matches", "Try clearing the filters or create a custom exercise.") }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                AppCard(onClick = { onOpenExercise(exercise.id) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                buildString {
                                    append(exercise.primaryMuscles.joinToString { it.label })
                                    append(" · ")
                                    append(exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() })
                                    if (exercise.isCustom) append(" · custom")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Pill(
                            when (exercise.measurement) {
                                Measurement.WEIGHT_REPS -> "kg × reps"
                                Measurement.REPS_ONLY -> "reps"
                                Measurement.TIME -> "time"
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateExerciseDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, muscle, equipment, measurement, rest, instructions ->
                viewModel.create(name, listOf(muscle), equipment, measurement, rest, instructions) { id ->
                    showCreate = false
                    onOpenExercise(id)
                }
            },
        )
    }
}

@Composable
private fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (String, MuscleGroup, Equipment, Measurement, Int, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var equipment by remember { mutableStateOf(Equipment.BARBELL) }
    var measurement by remember { mutableStateOf(Measurement.WEIGHT_REPS) }
    var rest by remember { mutableStateOf("120") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Muscle group", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MuscleGroup.entries.toList()) { group ->
                        Pill(group.label, selected = muscle == group, onClick = { muscle = group })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Equipment", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Equipment.entries.toList()) { option ->
                        Pill(
                            option.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = equipment == option,
                            onClick = { equipment = option },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Tracking", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Weight × reps", selected = measurement == Measurement.WEIGHT_REPS, onClick = { measurement = Measurement.WEIGHT_REPS })
                    Pill("Reps", selected = measurement == Measurement.REPS_ONLY, onClick = { measurement = Measurement.REPS_ONLY })
                    Pill("Time", selected = measurement == Measurement.TIME, onClick = { measurement = Measurement.TIME })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it.filter { c -> c.isDigit() } },
                    label = { Text("Default rest (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        name.trim(),
                        muscle,
                        equipment,
                        measurement,
                        rest.toIntOrNull() ?: 120,
                        instructions,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise
    val history = remember(state.sets) { e1rmHistory(state.sets) }
    val best = remember(state.sets) { bestSet(state.sets) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Archive exercise") },
                            onClick = { menu = false; viewModel.archive() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile(
                            "Best set",
                            best?.weightKg?.let { Units.format(it, state.units) } ?: "—",
                            best?.weightKg?.let { Units.label(state.units) },
                        )
                        StatTile(
                            "Est. 1RM",
                            history.maxOfOrNull { it.valueKg }?.let { Units.format(it, state.units) } ?: "—",
                            history.maxOfOrNull { it.valueKg }?.let { Units.label(state.units) },
                        )
                        StatTile("Sets logged", state.sets.size.toString())
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("Estimated 1RM", "last ${minOf(history.size, 30)}")
                    Spacer(Modifier.height(8.dp))
                    LineChart(
                        values = history.takeLast(30).map { it.valueKg.toFloat() },
                        emptyLabel = "Log a few sets to see the trend",
                    )
                }
            }

            if (exercise != null) {
                item {
                    AppCard {
                        SectionTitle("Details")
                        Spacer(Modifier.height(8.dp))
                        DetailRow("Primary", exercise.primaryMuscles.joinToString { it.label }.ifBlank { "—" })
                        DetailRow("Secondary", exercise.secondaryMuscles.joinToString { it.label }.ifBlank { "—" })
                        DetailRow("Equipment", exercise.equipment.name.lowercase().replaceFirstChar { it.uppercase() })
                        DetailRow("Tracking", exercise.measurement.name.lowercase().replace('_', ' '))
                        DetailRow("Default rest", "${exercise.defaultRestSec}s")
                        if (exercise.instructions.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(exercise.instructions, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (state.sets.isNotEmpty()) {
                item { SectionTitle("Recent sets") }
                items(state.sets.take(25), key = { it.id }) { set ->
                    AppCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                when {
                                    set.weightKg != null && set.reps != null ->
                                        "${Units.format(set.weightKg, state.units)} ${Units.label(state.units)} × ${set.reps}"
                                    set.reps != null -> "${set.reps} reps"
                                    set.durationSec != null -> "${set.durationSec}s"
                                    else -> "—"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    set.completedAt?.let { TimeFormat.day(it) } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                set.rpe?.let {
                                    Text(
                                        "RPE ${it.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item { EmptyState("No history yet", "Sets you complete in a workout show up here.") }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

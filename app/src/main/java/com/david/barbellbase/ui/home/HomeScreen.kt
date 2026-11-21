package com.david.barbellbase.ui.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.domain.Metrics
import com.david.barbellbase.domain.TimeFormat
import com.david.barbellbase.domain.Units
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.ProgressRing
import com.david.barbellbase.ui.components.SectionTitle
import com.david.barbellbase.ui.components.StatTile
import com.david.barbellbase.ui.updates.UpdateBanner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenWorkout: (String) -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExercises: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 24.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("BarbellBase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill("Settings", onClick = onOpenSettings)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Routines", onClick = onOpenRoutines)
                Pill("History", onClick = onOpenHistory)
                Pill("Exercises", onClick = onOpenExercises)
            }
        }

        state.activeSession?.let { session ->
            item {
                AppCard {
                    Text("IN PROGRESS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(session.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Started ${TimeFormat.clock(session.startedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onOpenWorkout(session.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Resume workout")
                    }
                }
            }
        }

        if (state.activeSession == null) {
            item {
                AppCard {
                    Text("UP NEXT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    if (state.nextDay != null) {
                        Text(
                            state.nextRoutineName ?: "Routine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            state.nextDay!!.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.startDay(state.nextDay!!.id, onOpenWorkout) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Start day") }
                            FilledTonalButton(
                                onClick = { viewModel.startEmpty(onOpenWorkout) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Empty") }
                        }
                    } else {
                        Text("No routine yet", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Build a routine once and every session after that is two taps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onOpenRoutines, modifier = Modifier.weight(1f)) { Text("Create routine") }
                            FilledTonalButton(
                                onClick = { viewModel.startEmpty(onOpenWorkout) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Start empty") }
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val progress = if (state.waterTargetMl > 0) state.waterMl.toFloat() / state.waterTargetMl else 0f
                    ProgressRing(progress = progress, size = 78.dp, strokeWidth = 8.dp) {
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Water", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.waterMl} ml of ${state.waterTargetMl} ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("+250", onClick = { viewModel.logWater(250) })
                            Pill("+500", onClick = { viewModel.logWater(500) })
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Walking", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.walkMin} of ${state.walkTargetMin} min today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Pill("Log 10 min", onClick = { viewModel.logWalk(10) })
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        if (state.walkTargetMin > 0) (state.walkMin.toFloat() / state.walkTargetMin).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
        }

        item {
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatTile(
                        "Volume (7d)",
                        Units.format(state.weekVolumeKg, state.units),
                        Units.label(state.units),
                    )
                    StatTile("Sessions (7d)", state.weekSessions.toString())
                    StatTile(
                        "Body",
                        state.latestWeightKg?.let { Units.format(it, state.units) } ?: "—",
                        if (state.latestWeightKg != null) Units.label(state.units) else null,
                    )
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            item { SectionTitle("Recent sessions") }
            items(state.recent, key = { it.id }) { session ->
                AppCard(onClick = { onOpenSession(session.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(session.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${TimeFormat.day(session.startedAt)} · ${TimeFormat.clock(session.startedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (session.finishedAt == null) "Active" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        }

        UpdateBanner(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
    }
}

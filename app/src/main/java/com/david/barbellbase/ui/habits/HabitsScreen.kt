package com.david.barbellbase.ui.habits

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.BarSeries
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.ProgressRing
import com.david.barbellbase.ui.components.SectionTitle
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Habits") }) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = if (state.waterTargetMl > 0) state.waterMl.toFloat() / state.waterTargetMl else 0f,
                            size = 92.dp,
                            strokeWidth = 10.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(state.waterMl / 1000.0).let { String.format(java.util.Locale.US, "%.1f", it) }} L",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "of ${state.waterTargetMl / 1000.0} L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Water", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${state.waterTargetMl - state.waterMl} ml to go",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("+250", onClick = { viewModel.logWater(250) })
                                Pill("+500", onClick = { viewModel.logWater(500) })
                                Pill("+750", onClick = { viewModel.logWater(750) })
                            }
                            TextButton(onClick = viewModel::undoWater) { Text("Undo last") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("Last 14 days", "ml")
                    BarSeries(
                        values = state.waterSeries.map { it.amount.toFloat() },
                        labels = state.waterSeries.map { dayLabel(it.epochDay) },
                        height = 90.dp,
                        emptyLabel = "No water logged yet",
                    )
                }
            }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = if (state.walkTargetMin > 0) state.walkMin.toFloat() / state.walkTargetMin else 0f,
                            size = 92.dp,
                            strokeWidth = 10.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${state.walkMin}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Walking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${(state.walkTargetMin - state.walkMin).coerceAtLeast(0)} min to go",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill("10 min", onClick = { viewModel.logWalk(10) })
                                Pill("20 min", onClick = { viewModel.logWalk(20) })
                            }
                            TextButton(onClick = viewModel::undoWalk) { Text("Undo last") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("Last 14 days", "min")
                    BarSeries(
                        values = state.walkSeries.map { it.amount.toFloat() },
                        labels = state.walkSeries.map { dayLabel(it.epochDay) },
                        height = 90.dp,
                        emptyLabel = "No walks logged yet",
                    )
                }
            }

            item {
                AppCard {
                    SectionTitle("Why track these?")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Water and walking targets feed the reminder notifications: the alarm shows how much you have " +
                            "left, and the walking chime offers a one-tap log.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun dayLabel(epochDay: Long): String {
    val date = java.time.LocalDate.ofEpochDay(epochDay)
    return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))
}

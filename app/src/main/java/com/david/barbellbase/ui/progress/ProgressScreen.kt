package com.david.barbellbase.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.domain.Units
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.BarSeries
import com.david.barbellbase.ui.components.EmptyState
import com.david.barbellbase.ui.components.LineChart
import com.david.barbellbase.ui.components.SectionTitle
import com.david.barbellbase.ui.components.StatTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Progress") }) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile("Sessions", state.totalSessions.toString())
                        StatTile("Sets", state.totalSets.toString())
                        StatTile(
                            "Volume",
                            Units.format(state.totalVolumeKg, state.units),
                            Units.label(state.units),
                        )
                    }
                }
            }

            item {
                AppCard {
                    SectionTitle("Weekly volume", "8 weeks")
                    Spacer(Modifier.height(10.dp))
                    BarSeries(
                        values = state.weeks.map { it.volumeKg.toFloat() },
                        labels = state.weeks.map { it.label },
                        emptyLabel = "Log a workout to start the chart",
                    )
                }
            }

            item {
                AppCard {
                    SectionTitle("Sessions per week", "8 weeks")
                    Spacer(Modifier.height(10.dp))
                    BarSeries(
                        values = state.weeks.map { it.sessions.toFloat() },
                        labels = state.weeks.map { it.label },
                        height = 90.dp,
                        emptyLabel = "No sessions logged yet",
                    )
                }
            }

            item {
                AppCard {
                    SectionTitle("Bodyweight")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            state.latestBodyKg?.let { "${Units.format(it, state.units)} ${state.bodyUnit}" } ?: "Not logged",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                        state.bodyChangeKg?.let { change ->
                            Text(
                                (if (change >= 0) "+" else "") + Units.format(change, state.units) + " " + state.bodyUnit,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LineChart(
                        values = state.bodyPoints,
                        emptyLabel = "Log your weight in Settings → Body",
                    )
                }
            }

            if (state.lifts.isNotEmpty()) {
                item { SectionTitle("Lift trends", "estimated 1RM") }
                items(state.lifts, key = { it.exerciseId }) { lift ->
                    AppCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(lift.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Best ${Units.format(lift.bestKg, state.units)} ${Units.label(state.units)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LineChart(values = lift.points, height = 100.dp)
                    }
                }
            } else {
                item {
                    EmptyState(
                        "No lift data yet",
                        "Exercises you have logged at least three sets for will show a trend here.",
                    )
                }
            }
        }
    }
}

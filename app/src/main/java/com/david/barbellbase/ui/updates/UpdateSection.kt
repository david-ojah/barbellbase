package com.david.barbellbase.ui.updates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.david.barbellbase.ui.components.AppCard
import com.david.barbellbase.ui.components.Pill
import com.david.barbellbase.ui.components.SectionTitle

@Composable
fun UpdateSection(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onPermissionResult() }

    AppCard {
        SectionTitle("Updates", "v${state.currentVersion}")
        Spacer(Modifier.height(6.dp))

        val update = state.update
        if (update == null) {
            Text(
                when {
                    state.checking -> "Checking for updates..."
                    state.skippedForToday -> "Update skipped for today. It will appear again tomorrow."
                    state.upToDate -> "You're on the latest version."
                    else -> "Check for a newer build. Updates install over the current app."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = viewModel::check,
                enabled = !state.checking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.checking) "Checking..." else "Check for updates")
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BarbellBase ${update.version} is available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Pill("New")
            }

            if (update.releaseNotes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    update.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))

            when {
                state.downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Downloading ${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.downloadedFile != null -> {
                    Button(onClick = viewModel::install, modifier = Modifier.fillMaxWidth()) {
                        Text("Install update")
                    }
                }

                else -> {
                    Button(onClick = viewModel::download, modifier = Modifier.fillMaxWidth()) {
                        Text("Download update${sizeLabel(update.sizeBytes)}")
                    }
                }
            }

            if (state.needsInstallPermission) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow BarbellBase to install unknown apps, then tap Install update again.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { permissionLauncher.launch(viewModel.installPermissionIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open install settings")
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = viewModel::skip) { Text("Skip today") }
                TextButton(onClick = viewModel::openReleasePage) { Text("Release notes") }
                TextButton(onClick = viewModel::check, enabled = !state.checking) {
                    Text(if (state.checking) "Checking..." else "Re-check")
                }
            }
        }

        state.error?.let { error ->
            Spacer(Modifier.height(6.dp))
            Text(
                error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun sizeLabel(bytes: Long): String =
    if (bytes <= 0L) "" else " (%.1f MB)".format(bytes / 1_048_576.0)

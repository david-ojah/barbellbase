package com.david.barbellbase.ui.updates

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@Composable
fun UpdateBanner(
    modifier: Modifier = Modifier,
    viewModel: UpdateBannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    val update = state.update ?: return

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onPermissionResult() }

    AppCard(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "UPDATE AVAILABLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Pill("v${update.version}")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "BarbellBase ${update.version} is ready",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (update.releaseNotes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                update.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = viewModel::install, modifier = Modifier.weight(1f)) {
                        Text("Install update")
                    }
                    TextButton(onClick = viewModel::skip) { Text("Skip") }
                }
            }

            else -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = viewModel::download, modifier = Modifier.weight(1f)) {
                        Text("Download${bannerSizeLabel(update.sizeBytes)}")
                    }
                    TextButton(onClick = viewModel::skip) { Text("Skip") }
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

private fun bannerSizeLabel(bytes: Long): String =
    if (bytes <= 0L) "" else " (%.1f MB)".format(bytes / 1_048_576.0)

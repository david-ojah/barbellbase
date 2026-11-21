package com.david.barbellbase.data.update

import android.content.Context
import com.david.barbellbase.data.sync.SyncConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads update metadata from Firebase Remote Config. Every device reads the same
 * cached config, so publishing a release does not hit the GitHub API per user.
 */
@Singleton
class RemoteConfigUpdateSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val defaults = mapOf(
        "latest_version" to "",
        "latest_tag" to "",
        "apk_url" to "",
        "apk_sha256" to "",
        "apk_size_bytes" to "0",
        "release_notes" to "",
    )

    suspend fun fetch(manual: Boolean): Result<AvailableUpdate?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SyncConfig.isConfigured) return@runCatching null

            val config = FirebaseRemoteConfig.getInstance()
            Tasks.await(config.setDefaultsAsync(defaults))
            Tasks.await(
                config.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(if (manual) 0L else 12 * 60 * 60L)
                        .build(),
                ),
            )
            Tasks.await(config.fetchAndActivate())

            val version = config.getString("latest_version").trim().removePrefix("v").removePrefix("V")
            val url = config.getString("apk_url").trim()
            if (version.isBlank() || url.isBlank()) return@runCatching null

            AvailableUpdate(
                version = version,
                tag = config.getString("latest_tag").trim().ifBlank { "v$version" },
                apkUrl = url,
                sha256 = config.getString("apk_sha256").trim().removePrefix("sha256:"),
                sizeBytes = config.getString("apk_size_bytes").trim().toLongOrNull() ?: 0L,
                releaseNotes = config.getString("release_notes").trim(),
            )
        }
    }
}

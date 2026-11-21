package com.david.barbellbase.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateStore: DataStore<Preferences> by preferencesDataStore(name = "app_updates")

@Singleton
class UpdatePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val checkedAt = longPreferencesKey("checked_at")
        val version = stringPreferencesKey("version")
        val tag = stringPreferencesKey("tag")
        val apkUrl = stringPreferencesKey("apk_url")
        val sha256 = stringPreferencesKey("sha256")
        val sizeBytes = longPreferencesKey("size_bytes")
        val releaseNotes = stringPreferencesKey("release_notes")
        val dismissedTag = stringPreferencesKey("dismissed_tag")
        val notifiedTag = stringPreferencesKey("notified_tag")
    }

    val state: Flow<StoredUpdate> = context.updateStore.data.map { preferences ->
        StoredUpdate(
            checkedAt = preferences[Keys.checkedAt] ?: 0L,
            update = readUpdate(preferences),
            dismissedTag = preferences[Keys.dismissedTag],
            notifiedTag = preferences[Keys.notifiedTag],
        )
    }

    suspend fun read(): StoredUpdate {
        val preferences = context.updateStore.data.first()
        return StoredUpdate(
            checkedAt = preferences[Keys.checkedAt] ?: 0L,
            update = readUpdate(preferences),
            dismissedTag = preferences[Keys.dismissedTag],
            notifiedTag = preferences[Keys.notifiedTag],
        )
    }

    suspend fun saveCheck(checkedAt: Long, update: AvailableUpdate?) {
        context.updateStore.edit { preferences ->
            preferences[Keys.checkedAt] = checkedAt
            if (update == null) {
                preferences.remove(Keys.version)
                preferences.remove(Keys.tag)
                preferences.remove(Keys.apkUrl)
                preferences.remove(Keys.sha256)
                preferences.remove(Keys.sizeBytes)
                preferences.remove(Keys.releaseNotes)
            } else {
                preferences[Keys.version] = update.version
                preferences[Keys.tag] = update.tag
                preferences[Keys.apkUrl] = update.apkUrl
                preferences[Keys.sha256] = update.sha256
                preferences[Keys.sizeBytes] = update.sizeBytes
                preferences[Keys.releaseNotes] = update.releaseNotes
            }
        }
    }

    suspend fun dismiss(tag: String) = context.updateStore.edit { it[Keys.dismissedTag] = tag }

    suspend fun markNotified(tag: String) = context.updateStore.edit { it[Keys.notifiedTag] = tag }

    private fun readUpdate(preferences: Preferences): AvailableUpdate? {
        val version = preferences[Keys.version] ?: return null
        val url = preferences[Keys.apkUrl] ?: return null
        return AvailableUpdate(
            version = version,
            tag = preferences[Keys.tag] ?: "v$version",
            apkUrl = url,
            sha256 = preferences[Keys.sha256].orEmpty(),
            sizeBytes = preferences[Keys.sizeBytes] ?: 0L,
            releaseNotes = preferences[Keys.releaseNotes].orEmpty(),
        )
    }
}

package com.david.barbellbase.data.sync

import android.content.Intent
import androidx.room.withTransaction
import com.david.barbellbase.data.db.BarbellBaseDatabase
import com.david.barbellbase.data.db.SyncChangeDao
import com.david.barbellbase.data.prefs.DurableSettings
import com.david.barbellbase.data.prefs.SettingsRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-record sync against Firestore. Local changes are collected by SQLite
 * triggers; remote changes are applied with last-write-wins per record, so
 * edits on different devices merge instead of overwriting a whole snapshot.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val db: BarbellBaseDatabase,
    private val syncChangeDao: SyncChangeDao,
    private val mapper: SyncEntityMapper,
    private val remote: FirebaseSyncDataSource,
    private val settings: SettingsRepository,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun isConfigured(): Boolean = remote.isConfigured()
    fun accountEmail(): String? = remote.currentUserEmail()
    fun signInIntent(): Intent = remote.signInIntent()

    suspend fun completeSignIn(data: Intent?): Result<String> = remote.completeSignIn(data)

    fun signOut() = remote.signOut()

    suspend fun sync(): Result<SyncSummary> = runCatching {
        if (!remote.isConfigured()) error("Cloud sync is not configured")
        if (remote.currentUid() == null) error("Sign in with Google first")

        val deviceId = settings.deviceId()
        val lastSyncedAt = settings.lastSyncedAt()
        var downloaded = 0

        val remoteRecords = remote.download(SyncPolicy.overlapSince(lastSyncedAt)).getOrThrow()
        db.withTransaction {
            remoteRecords
                .filter { it.deviceId != deviceId }
                .forEach { record ->
                    val localChange = syncChangeDao.get(record.entityType, record.entityId)
                    val localWins = SyncPolicy.localWins(localChange?.changedAt, record.updatedAt)
                    if (!localWins) {
                        mapper.apply(record)
                        syncChangeDao.clear(record.entityType, record.entityId)
                        downloaded += 1
                    }
                }
        }

        val toUpload = mutableListOf<SyncRecord>()
        val exhausted = mutableListOf<Pair<String, String>>()
        syncChangeDao.getAll().forEach { change ->
            if (change.deleted) {
                toUpload += SyncRecord(
                    entityType = change.entityType,
                    entityId = change.entityId,
                    payload = null,
                    updatedAt = change.changedAt,
                    deleted = true,
                    deviceId = deviceId,
                )
            } else {
                val payload = mapper.load(change.entityType, change.entityId)
                if (payload == null) {
                    exhausted += change.entityType to change.entityId
                } else {
                    toUpload += SyncRecord(
                        entityType = change.entityType,
                        entityId = change.entityId,
                        payload = payload,
                        updatedAt = change.changedAt,
                        deleted = false,
                        deviceId = deviceId,
                    )
                }
            }
        }

        if (exhausted.isNotEmpty()) {
            db.withTransaction {
                exhausted.forEach { (type, id) -> syncChangeDao.clear(type, id) }
            }
        }

        if (toUpload.isNotEmpty()) {
            remote.upload(toUpload).getOrThrow()
            db.withTransaction {
                toUpload.forEach {
                    // Do not erase an edit that arrived while the upload was in flight.
                    syncChangeDao.clearIfUnchanged(it.entityType, it.entityId, it.updatedAt)
                }
            }
        }

        syncSettings(deviceId)
        settings.setLastSyncedAt(System.currentTimeMillis())

        SyncSummary(uploaded = toUpload.size, downloaded = downloaded)
    }

    private suspend fun syncSettings(deviceId: String) {
        val localChangedAt = settings.settingsChangedAt()
        val lastSettingsAt = settings.lastSyncedSettingsAt()
        val remoteSettings = remote.downloadSettings().getOrNull()

        val remoteWins = remoteSettings != null &&
            remoteSettings.updatedAt > localChangedAt &&
            remoteSettings.updatedAt > lastSettingsAt

        if (remoteWins) {
            val toApply = remoteSettings ?: return
            val durable = json.decodeFromString(DurableSettings.serializer(), toApply.payload)
            settings.applyDurableSettings(durable)
            settings.setLastSyncedSettingsAt(toApply.updatedAt)
        } else if (localChangedAt > lastSettingsAt) {
            val payload = json.encodeToString(
                DurableSettings.serializer(),
                settings.durableSettings(),
            )
            remote.uploadSettings(payload, localChangedAt, deviceId).getOrThrow()
            settings.setLastSyncedSettingsAt(localChangedAt)
        }
    }
}

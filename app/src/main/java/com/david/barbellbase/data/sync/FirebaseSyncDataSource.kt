package com.david.barbellbase.data.sync

import android.content.Context
import android.content.Intent
import com.david.barbellbase.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isConfigured(): Boolean = SyncConfig.isConfigured

    fun currentUid(): String? = runCatching {
        if (!isConfigured()) null else FirebaseAuth.getInstance().currentUser?.uid
    }.getOrNull()

    fun currentUserEmail(): String? = runCatching {
        if (!isConfigured()) return@runCatching null
        val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching null
        user.email ?: user.displayName
    }.getOrNull()

    fun signInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun completeSignIn(data: Intent?): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured()) error("Cloud sync is not configured")
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = account.idToken ?: error("Google did not return an ID token")
            Tasks.await(
                FirebaseAuth.getInstance().signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null),
                ),
            )
            FirebaseAuth.getInstance().currentUser?.email ?: account.email ?: "Google account"
        }
    }

    fun signOut() {
        if (!isConfigured()) return
        runCatching { FirebaseAuth.getInstance().signOut() }
        runCatching {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, options).signOut()
        }
    }

    suspend fun upload(records: List<SyncRecord>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUid() ?: error("Not signed in")
            val firestore = FirebaseFirestore.getInstance()
            val collection = firestore.collection("users").document(uid).collection("records")

            records.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { record ->
                    val data = mutableMapOf<String, Any>(
                        "entityType" to record.entityType,
                        "entityId" to record.entityId,
                        "deleted" to record.deleted,
                        "updatedAt" to record.updatedAt,
                        "deviceId" to record.deviceId,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                    )
                    record.payload?.let { data["payload"] = it }
                    batch.set(collection.document("${record.entityType}:${record.entityId}"), data)
                }
                Tasks.await(batch.commit())
            }
        }
    }

    suspend fun download(since: Long): Result<List<SyncRecord>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUid() ?: error("Not signed in")
            val collection = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("records")

            val query: Query = if (since <= 0L) {
                collection
            } else {
                val seconds = since / 1000
                val nanos = ((since % 1000) * 1_000_000).toInt()
                collection.whereGreaterThan("serverUpdatedAt", Timestamp(seconds, nanos))
            }

            val snapshot = Tasks.await(query.get())
            snapshot.documents.mapNotNull { doc ->
                val type = doc.getString("entityType") ?: return@mapNotNull null
                val id = doc.getString("entityId") ?: return@mapNotNull null
                SyncRecord(
                    entityType = type,
                    entityId = id,
                    payload = doc.getString("payload"),
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    deleted = doc.getBoolean("deleted") ?: false,
                    deviceId = doc.getString("deviceId") ?: "",
                )
            }
        }
    }

    suspend fun uploadSettings(
        payload: String,
        updatedAt: Long,
        deviceId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUid() ?: error("Not signed in")
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("meta").document("settings")
            Tasks.await(
                doc.set(
                    mapOf(
                        "payload" to payload,
                        "updatedAt" to updatedAt,
                        "deviceId" to deviceId,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                    ),
                ),
            )
            Unit
        }
    }

    suspend fun downloadSettings(): Result<RemoteSettings?> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = currentUid() ?: error("Not signed in")
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("meta").document("settings")
            val snapshot = Tasks.await(doc.get())
            if (!snapshot.exists()) return@runCatching null
            val payload = snapshot.getString("payload") ?: return@runCatching null
            RemoteSettings(payload, snapshot.getLong("updatedAt") ?: 0L)
        }
    }
}

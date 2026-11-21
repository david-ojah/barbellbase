package com.david.barbellbase.data.sync

import android.content.Context
import com.david.barbellbase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object SyncConfig {

    val isFirebaseConfigured: Boolean
        get() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank()

    val isConfigured: Boolean
        get() = isFirebaseConfigured && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    val isPushConfigured: Boolean
        get() = isFirebaseConfigured && BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    /** Initializes Firebase manually so no google-services.json is required in the public repo. */
    fun initialize(context: Context) {
        if (!isFirebaseConfigured) return
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        val builder = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
        if (BuildConfig.FIREBASE_SENDER_ID.isNotBlank()) {
            builder.setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
        }
        FirebaseApp.initializeApp(context, builder.build())
    }
}

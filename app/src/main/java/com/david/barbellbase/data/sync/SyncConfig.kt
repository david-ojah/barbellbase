package com.david.barbellbase.data.sync

import android.content.Context
import com.david.barbellbase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object SyncConfig {

    val isConfigured: Boolean
        get() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Initializes Firebase manually so no google-services.json is required in the public repo. */
    fun initialize(context: Context) {
        if (!isConfigured) return
        if (FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build(),
        )
    }
}

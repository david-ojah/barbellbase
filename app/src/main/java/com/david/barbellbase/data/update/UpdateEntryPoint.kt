package com.david.barbellbase.data.update

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpdateEntryPoint {
    fun updateRepository(): UpdateRepository
    fun updatePreferences(): UpdatePreferences
}

fun Context.updateEntryPoint(): UpdateEntryPoint =
    EntryPointAccessors.fromApplication(this, UpdateEntryPoint::class.java)

package com.david.barbellbase.service

import android.content.Context
import com.david.barbellbase.data.prefs.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RestState(
    val endsAt: Long,
    val totalSeconds: Int,
    val remainingSeconds: Int,
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

@Singleton
class RestTimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val ticker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(500)
        }
    }

    val state: StateFlow<RestState?> = settings.settings
        .map { it.restEndsAt to it.restTotalSec }
        .distinctUntilChanged()
        .flatMapLatest { (endsAt, total) ->
            if (endsAt == null) {
                flowOf(null)
            } else {
                ticker.map { now ->
                    val remaining = ((endsAt - now) / 1000L).coerceAtLeast(0).toInt()
                    if (remaining <= 0) null else RestState(endsAt, total, remaining)
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun start(seconds: Int) {
        val endAt = System.currentTimeMillis() + seconds * 1000L
        settings.setRest(endAt, seconds)
        RestNotifications.show(context, endAt)
        RestAlarmScheduler.schedule(context, endAt)
    }

    fun extend(deltaSeconds: Int) {
        val current = state.value ?: return
        val endAt = current.endsAt + deltaSeconds * 1000L
        scope.launch { settings.setRest(endAt, current.totalSeconds) }
        RestNotifications.show(context, endAt)
        RestAlarmScheduler.schedule(context, endAt)
    }

    fun stop() {
        scope.launch { settings.setRest(null, 0) }
        RestNotifications.cancel(context)
        RestAlarmScheduler.cancel(context)
    }
}

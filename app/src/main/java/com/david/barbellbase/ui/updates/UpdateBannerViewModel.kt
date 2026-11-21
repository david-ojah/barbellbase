package com.david.barbellbase.ui.updates

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.update.AvailableUpdate
import com.david.barbellbase.data.update.UpdateInstaller
import com.david.barbellbase.data.update.UpdatePreferences
import com.david.barbellbase.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

data class UpdateBannerState(
    val visible: Boolean = false,
    val update: AvailableUpdate? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedFile: File? = null,
    val needsInstallPermission: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the compact update banner on the home screen. Visibility comes from
 * the stored update and a calendar-day skip, so a skipped release reappears
 * after midnight without another network call.
 */
@HiltViewModel
class UpdateBannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val preferences: UpdatePreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateBannerState())
    val state = _state.asStateFlow()

    private val today = flow {
        while (true) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate().toEpochDay())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
        }
    }

    init {
        viewModelScope.launch {
            combine(preferences.state, today) { stored, day ->
                stored.visibleUpdate(repository.currentVersion, day)
            }.collect { update ->
                _state.update { current ->
                    if (current.downloading) {
                        current
                    } else {
                        val changed = current.update?.tag != update?.tag
                        current.copy(
                            visible = update != null,
                            update = update,
                            downloadedFile = if (changed) null else current.downloadedFile,
                            progress = if (changed) 0f else current.progress,
                        )
                    }
                }
            }
        }
    }

    fun download() {
        if (_state.value.downloading || _state.value.update == null) return
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0f, error = null) }
            repository.download { progress ->
                _state.update { it.copy(progress = progress) }
            }
                .onSuccess { file ->
                    _state.update { it.copy(downloading = false, downloadedFile = file) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            downloading = false,
                            error = error.message ?: "Download failed",
                        )
                    }
                }
        }
    }

    fun install() {
        val file = _state.value.downloadedFile ?: return
        if (!UpdateInstaller.canInstall(context)) {
            _state.update { it.copy(needsInstallPermission = true) }
            return
        }
        runCatching { UpdateInstaller.install(context, file) }
            .onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Could not open the installer") }
            }
    }

    fun installPermissionIntent(): Intent = UpdateInstaller.permissionIntent(context)

    fun onPermissionResult() {
        val allowed = UpdateInstaller.canInstall(context)
        _state.update { it.copy(needsInstallPermission = !allowed) }
        if (allowed) install()
    }

    fun skip() {
        val tag = _state.value.update?.tag ?: return
        viewModelScope.launch { preferences.skip(tag, LocalDate.now().toEpochDay()) }
        _state.update {
            it.copy(
                visible = false,
                update = null,
                downloadedFile = null,
                progress = 0f,
                error = null,
            )
        }
    }
}

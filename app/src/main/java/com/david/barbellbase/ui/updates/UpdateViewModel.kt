package com.david.barbellbase.ui.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.update.AvailableUpdate
import com.david.barbellbase.data.update.UpdateInstaller
import com.david.barbellbase.data.update.UpdatePreferences
import com.david.barbellbase.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

data class UpdateUiState(
    val currentVersion: String = "",
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val update: AvailableUpdate? = null,
    val lastCheckedAt: Long = 0L,
    val upToDate: Boolean = false,
    val skippedForToday: Boolean = false,
    val downloadedFile: File? = null,
    val needsInstallPermission: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val preferences: UpdatePreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState(currentVersion = repository.currentVersion))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = preferences.read()
            val suppressed = stored.isSuppressed(LocalDate.now().toEpochDay())
            _state.update {
                it.copy(
                    update = if (suppressed) null else stored.update,
                    lastCheckedAt = stored.checkedAt,
                    upToDate = stored.checkedAt > 0L && stored.update == null,
                    skippedForToday = suppressed,
                )
            }
        }
    }

    fun check() {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null, downloadedFile = null) }
            repository.check(manual = true)
                .onSuccess { update ->
                    _state.update {
                        it.copy(
                            checking = false,
                            update = update,
                            upToDate = update == null,
                            skippedForToday = false,
                            lastCheckedAt = System.currentTimeMillis(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            checking = false,
                            error = error.message ?: "Could not check for updates",
                        )
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
                update = null,
                downloadedFile = null,
                progress = 0f,
                skippedForToday = true,
            )
        }
    }

    fun openReleasePage() {
        val tag = _state.value.update?.tag ?: return
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/david-ojah/barbellbase/releases/tag/$tag"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Could not open the release page") }
            }
    }
}

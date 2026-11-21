package com.david.barbellbase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.prefs.SettingsRepository
import com.david.barbellbase.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val theme = settingsRepository.settings
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
}

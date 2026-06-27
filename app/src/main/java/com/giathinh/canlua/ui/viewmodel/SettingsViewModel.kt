package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.AppLanguage
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.WeighDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cardRepository: com.giathinh.canlua.repository.CardRepository
) : ViewModel() {
    
    val isTtsEnabled: StateFlow<Boolean> = settingsRepository.ttsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.isTtsEnabled()
        )

    val isAutoSyncEnabled: StateFlow<Boolean> = settingsRepository.autoSyncEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.isAutoSyncEnabled()
        )

    val fontScale: StateFlow<FontScale> = settingsRepository.fontScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getFontScale()
        )

    val language: StateFlow<AppLanguage> = settingsRepository.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getLanguage()
        )

    val appThemeMode: StateFlow<AppThemeMode> = settingsRepository.appThemeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getThemeMode()
        )

    val weighDefaults: StateFlow<WeighDefaults> = settingsRepository.weighDefaults
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getWeighDefaults()
        )

    fun setWeighDefaults(d: WeighDefaults) {
        viewModelScope.launch { settingsRepository.setWeighDefaults(d) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTtsEnabled(enabled)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncEnabled(enabled)
        }
    }

    fun setFontScale(scale: FontScale) {
        viewModelScope.launch {
            settingsRepository.setFontScale(scale)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    val isGuestMode: StateFlow<Boolean> = settingsRepository.guestMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.isGuestMode()
        )

    fun setGuestMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGuestMode(enabled)
        }
    }

}


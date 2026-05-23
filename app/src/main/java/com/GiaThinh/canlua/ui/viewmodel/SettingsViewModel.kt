package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.AppLanguage
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.repository.WeighDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _isTtsEnabled = MutableStateFlow(settingsRepository.isTtsEnabled())
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    val isAutoSyncEnabled: StateFlow<Boolean> = settingsRepository.autoSyncEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settingsRepository.isAutoSyncEnabled()
        )

    val fontScale: StateFlow<FontScale> = settingsRepository.fontScale
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settingsRepository.getFontScale()
        )

    val language: StateFlow<AppLanguage> = settingsRepository.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settingsRepository.getLanguage()
        )

    val weighDefaults: StateFlow<WeighDefaults> = settingsRepository.weighDefaults
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settingsRepository.getWeighDefaults()
        )

    fun setWeighDefaults(d: WeighDefaults) {
        viewModelScope.launch { settingsRepository.setWeighDefaults(d) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTtsEnabled(enabled)
            _isTtsEnabled.value = enabled
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
}


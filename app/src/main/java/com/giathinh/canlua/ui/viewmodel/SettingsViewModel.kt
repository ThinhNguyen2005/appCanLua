package com.giathinh.canlua.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.AppLanguage
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.WeighDefaults
import com.giathinh.canlua.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cardRepository: CardRepository,
    private val authManager: AuthManager
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

    val isSyncOnlyWifi: StateFlow<Boolean> = settingsRepository.syncOnlyWifi
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.isSyncOnlyWifi()
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

    fun setSyncOnlyWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSyncOnlyWifi(enabled)
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

    fun exportBackup(
        context: Context,
        uri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val uid = authManager.currentUser?.uid ?: "GUEST"
            val result = BackupManager.exportData(context, uri, uid, cardRepository)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Lỗi xuất dữ liệu")
            }
        }
    }

    fun importBackup(
        context: Context,
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val uid = authManager.currentUser?.uid ?: "GUEST"
            val result = BackupManager.importData(context, uri, uid, cardRepository)
            if (result.isSuccess) {
                onSuccess(result.getOrNull() ?: 0)
            } else {
                onError(result.exceptionOrNull()?.message ?: "Lỗi nhập dữ liệu")
            }
        }
    }
}
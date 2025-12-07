package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.repository.BackupStatus
import com.GiaThinh.canlua.repository.StorageBackupManager
import com.GiaThinh.canlua.repository.SyncManager
import com.GiaThinh.canlua.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val backupManager: StorageBackupManager
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
    val lastSyncTime: StateFlow<Long?> = syncManager.lastSyncTime
    val backupStatus: StateFlow<BackupStatus> = backupManager.backupStatus
    val lastBackupTime: StateFlow<Long?> = backupManager.lastBackupTime

    fun syncAll() {
        viewModelScope.launch {
            syncManager.syncAll()
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            backupManager.backupDatabase()
        }
    }
}


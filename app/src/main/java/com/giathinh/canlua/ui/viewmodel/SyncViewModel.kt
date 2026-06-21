package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.repository.BackupStatus
import com.giathinh.canlua.repository.StorageBackupManager
import com.giathinh.canlua.repository.SyncManager
import com.giathinh.canlua.repository.SyncStatus
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
    val hasPendingSyncData: StateFlow<Boolean> = syncManager.hasPendingSyncData
    val backupStatus: StateFlow<BackupStatus> = backupManager.backupStatus
    val lastBackupTime: StateFlow<Long?> = backupManager.lastBackupTime
    val isUserSignedIn: Boolean
        get() = syncManager.isUserSignedIn


    init {
        refreshPendingSyncState()
    }

    fun syncAll() {
        viewModelScope.launch {
            syncManager.syncAll()
        }
    }

    fun refreshPendingSyncState() {
        viewModelScope.launch {
            syncManager.refreshPendingSyncState()
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            backupManager.backupDatabase()
        }
    }
}


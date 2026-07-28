package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.giathinh.canlua.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor() : ViewModel() {
    val syncStatus: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus.Idle)
    val lastSyncTime: StateFlow<Long?> = MutableStateFlow(null)
    val hasPendingSyncData: StateFlow<Boolean> = MutableStateFlow(false)
    val isUserSignedIn: Boolean = false
    fun syncAll() = Unit
    fun refreshPendingSyncState() = Unit
}
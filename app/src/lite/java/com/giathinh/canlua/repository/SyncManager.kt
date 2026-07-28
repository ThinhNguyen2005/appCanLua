package com.giathinh.canlua.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor() {
    val syncStatus = kotlinx.coroutines.flow.flowOf<SyncStatus>(SyncStatus.Idle)
}
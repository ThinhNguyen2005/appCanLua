package com.giathinh.canlua.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = syncManager.syncAll()
        return if (result.isSuccess) {
            Result.success()
        } else {
            // Nếu chưa đăng nhập, không retry để tránh vòng lặp
            val msg = result.exceptionOrNull()?.message ?: ""
            return if (msg.contains("No user signed in", ignoreCase = true)) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }
}


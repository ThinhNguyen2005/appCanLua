package com.GiaThinh.canlua.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    suspend fun backupDatabase(): Result<String> {
        if (auth.currentUser == null) {
            val msg = "Yêu cầu đăng nhập trước khi backup"
            _backupStatus.value = BackupStatus.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        val dbFile = context.getDatabasePath("canlua_database")
        if (!dbFile.exists()) {
            val msg = "Không tìm thấy file cơ sở dữ liệu"
            _backupStatus.value = BackupStatus.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }

        return try {
            _backupStatus.value = BackupStatus.BackingUp
            val targetPath = "backups/${Build.MODEL}_${System.currentTimeMillis()}.db"
            val ref = storage.reference.child(targetPath)
            ref.putFile(Uri.fromFile(dbFile)).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            _backupStatus.value = BackupStatus.Success(downloadUrl)
            _lastBackupTime.value = System.currentTimeMillis()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error(e.message ?: "Backup thất bại")
            Result.failure(e)
        }
    }
}

sealed class BackupStatus {
    object Idle : BackupStatus()
    object BackingUp : BackupStatus()
    data class Success(val url: String?) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}


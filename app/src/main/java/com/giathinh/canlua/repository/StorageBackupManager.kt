package com.giathinh.canlua.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import com.giathinh.canlua.data.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val database: AppDatabase
) {
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    suspend fun backupDatabase(): Result<String> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            val msg = "Yêu cầu đăng nhập trước khi backup"
            _backupStatus.value = BackupStatus.Error(msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        return@withContext try {
            _backupStatus.value = BackupStatus.BackingUp

            // Force WAL checkpoint to flush memory/journal writes to raw database file
            try {
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                    cursor.moveToFirst()
                }
            } catch (e: Exception) {
                // Fallback: log error but proceed with best-effort backup
            }

            val dbFile = context.getDatabasePath("canlua_database")
            if (!dbFile.exists()) {
                val msg = "Không tìm thấy file cơ sở dữ liệu"
                _backupStatus.value = BackupStatus.Error(msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

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


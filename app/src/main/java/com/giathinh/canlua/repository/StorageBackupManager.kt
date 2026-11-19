package com.giathinh.canlua.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import com.giathinh.canlua.util.BackupManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T07: Backup cloud dùng JSON portable (qua [BackupManager]) thay vì upload nguyên
 * file database thô chứa CCCD/tọa độ. File JSON có thể import lại bằng luồng hiện
 * có. Backup raw cũ trên cloud được GIỮ nguyên (không tự xóa).
 */
@Singleton
class StorageBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val cardRepository: CardRepository
) {
    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    val lastBackupTime: StateFlow<Long?> = _lastBackupTime.asStateFlow()

    suspend fun backupDatabase(): Result<String> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val msg = "Yêu cầu đăng nhập trước khi backup"
            _backupStatus.value = BackupStatus.Error(msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        return@withContext try {
            _backupStatus.value = BackupStatus.BackingUp

            // Xuất JSON ra file tạm trong cache rồi upload — không upload raw DB.
            val tempFile = File(context.cacheDir, "backup_${System.currentTimeMillis()}.json")
            val exportResult = BackupManager.exportData(
                context = context,
                uri = Uri.fromFile(tempFile),
                ownerUid = uid,
                cardRepository = cardRepository
            )
            if (exportResult.isFailure) {
                tempFile.delete()
                val msg = "Không xuất được dữ liệu backup"
                _backupStatus.value = BackupStatus.Error(msg)
                return@withContext Result.failure(exportResult.exceptionOrNull() ?: IllegalStateException(msg))
            }

            val targetPath = "backups/${Build.MODEL}_${System.currentTimeMillis()}.json"
            val ref = storage.reference.child(targetPath)
            ref.putFile(Uri.fromFile(tempFile)).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            tempFile.delete()
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


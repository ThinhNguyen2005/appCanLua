package com.GiaThinh.canlua.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý yêu cầu nâng cấp role TRADER — collection `roleRequests/{uid}` trên Firestore.
 *
 * Document schema:
 * ```
 * {
 *   uid: String,
 *   email: String,
 *   displayName: String,
 *   businessName: String,
 *   taxId: String,           // optional
 *   phone: String,
 *   reason: String,          // lý do user muốn trở thành thương lái
 *   status: "PENDING" | "APPROVED" | "REJECTED",
 *   createdAt: Timestamp,
 *   reviewedAt: Timestamp?,  // null khi chưa duyệt
 *   reviewerNote: String?
 * }
 * ```
 *
 * Phase này: chỉ FE submit/observe; admin duyệt thủ công qua Firebase Console
 * bằng cách update `status = "APPROVED"` rồi update `profiles/{uid}.role = "TRADER"`.
 * Phase sau sẽ build Cloud Function tự động + custom claims.
 */
@Singleton
class RoleRequestRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    enum class Status { PENDING, APPROVED, REJECTED }

    data class RoleRequest(
        val uid: String = "",
        val email: String = "",
        val displayName: String = "",
        val businessName: String = "",
        val taxId: String = "",
        val phone: String = "",
        val reason: String = "",
        val status: String = "PENDING",
        val createdAt: Long = 0L,
        val reviewedAt: Long? = null,
        val reviewerNote: String? = null
    )

    suspend fun submit(
        businessName: String,
        taxId: String,
        phone: String,
        reason: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Chưa đăng nhập"))
        return try {
            val data = mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "businessName" to businessName,
                "taxId" to taxId,
                "phone" to phone,
                "reason" to reason,
                "status" to Status.PENDING.name,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION).document(user.uid).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lắng nghe trạng thái request hiện tại — UI dùng để hiển thị badge "Đang chờ duyệt"
     * hoặc tự ẩn form khi status = APPROVED.
     */
    fun observeMyRequest(): Flow<RoleRequest?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = firestore.collection(COLLECTION).document(uid)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snap, _ ->
                if (snap == null || !snap.exists()) {
                    trySend(null)
                } else {
                    trySend(snap.toObject(RoleRequest::class.java))
                }
            }
        awaitClose { reg.remove() }
    }

    companion object {
        private const val COLLECTION = "roleRequests"
    }
}

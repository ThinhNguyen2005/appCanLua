package com.giathinh.canlua.data.model

import androidx.room.Entity
import androidx.room.Index

/**
 * v20: Hàng đợi snapshot bất biến chờ đẩy lên cloud.
 *
 * Mỗi phiếu chỉ có một snapshot đang gửi cho một [localRevision]. Snapshot được
 * chụp trong Room transaction (card + entries + transactions cùng revision) nên
 * không bao giờ ghép parent/children từ các thời điểm khác nhau.
 *
 * [payload] là aggregate đã serialize (mã hóa tại chỗ theo T07). [sessionGeneration]
 * gắn snapshot với phiên đăng nhập đã chụp — đổi tài khoản thì snapshot cũ bị loại.
 * [baseRemoteRevision] là head remote mong đợi khi publish (CAS).
 */
@Entity(
    tableName = "sync_outbox",
    primaryKeys = ["ownerUid", "syncId", "localRevision"],
    indices = [Index(value = ["ownerUid"])]
)
data class SyncOutbox(
    val ownerUid: String,
    val syncId: String,
    val localRevision: Long,
    val sessionGeneration: Long,
    val baseRemoteRevision: String? = null,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)
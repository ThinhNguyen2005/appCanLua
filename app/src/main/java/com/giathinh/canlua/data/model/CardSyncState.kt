package com.giathinh.canlua.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * v20: Trạng thái đồng bộ của một phiếu, TÁCH khỏi [Card] để vẫn tồn tại sau khi
 * phiếu bị xóa (publish manifest deleted=true). Khóa chính là syncId logical.
 *
 * - [localRevision]: revision local hiện tại (khớp Card.revision khi còn sống).
 * - [baseRemoteRevision]: head remote mà local đã biết làm base cho lần push kế.
 * - [ackedRemoteRevision]: revision remote gần nhất đã được xác nhận đồng bộ.
 * - [protocolState]: LEGACY (chưa nâng cấp) / SYNCED / DIRTY / CONFLICT / DELETED.
 */
@Entity(
    tableName = "card_sync_state",
    indices = [Index(value = ["ownerUid"])]
)
data class CardSyncState(
    @PrimaryKey
    val syncId: String,
    val ownerUid: String,
    val localRevision: Long = 1L,
    val baseRemoteRevision: String? = null,
    val ackedRemoteRevision: String? = null,
    val protocolState: String = STATE_LEGACY
) {
    companion object {
        const val STATE_LEGACY = "LEGACY"
        const val STATE_SYNCED = "SYNCED"
        const val STATE_DIRTY = "DIRTY"
        const val STATE_CONFLICT = "CONFLICT"
        const val STATE_DELETED = "DELETED"
    }
}
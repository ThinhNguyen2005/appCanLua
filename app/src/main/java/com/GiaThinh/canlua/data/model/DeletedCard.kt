package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tombstone cho phiếu đã xoá — lưu snapshot tại thời điểm xoá.
 *
 * **Vì sao cần?** Offline-first sync có "tombstone problem":
 *  1. User xoá phiếu local → `DELETE FROM cards`.
 *  2. `SyncableCardRepository.deleteCard` cố xoá Firestore — fail nếu offline.
 *  3. Auto pull tiếp theo → Firestore vẫn còn doc → `PullDedupResolver` không
 *     match local (đã xoá) → INSERT lại → **phiếu phục sinh**.
 *
 * Fix: ghi tombstone với `firestoreId` (key dedup mạnh nhất) trước khi xoá.
 * Khi pull, check tombstone → skip insert. Background worker sẽ retry xoá cloud
 * cho đến khi thành công.
 *
 * **Bonus**: dữ liệu tombstone = "Lịch sử phiếu đã xoá" để user khôi phục.
 *
 * @param firestoreId source-of-truth dedup. Nullable cho phiếu chưa từng sync
 *                    (chỉ tồn tại local) — vẫn ghi tombstone để khôi phục được.
 * @param localId Room id cũ — null sau khi xoá, dùng để hiển thị "phiếu cũ".
 * @param cardJson serialized full Card data — restore lại nguyên trạng.
 */
@Entity(
    tableName = "deleted_cards",
    indices = [
        androidx.room.Index(value = ["ownerUid"]),
        androidx.room.Index(value = ["firestoreId"])
    ]
)
data class DeletedCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUid: String,
    val firestoreId: String?,
    val localId: Long?,
    val cardJson: String,
    val deletedAt: Long = System.currentTimeMillis(),

    // Denormalized fields hiển thị nhanh trong list không cần parse JSON
    val name: String,
    val traderName: String,
    val totalWeight: Double,
    val totalAmount: Double,
    val cardDate: Long,
    val seasonLabel: String,
    val riceVariety: String,

    /** Đã đẩy delete lên cloud chưa. False = retry trong background sync. */
    val cloudDeleted: Boolean = false
)

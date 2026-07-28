package com.giathinh.canlua.repository

import com.giathinh.canlua.data.firestore.FirestoreCard
import com.giathinh.canlua.data.model.Card

/**
 * Pure logic quyết định hành động khi pull 1 [FirestoreCard] về Room.
 *
 * Tách khỏi [SyncManager] để test JVM-only (không cần Robolectric/Room runtime).
 * `SyncManager` inject 2 lookup function (theo firestoreId, theo composite key)
 * rồi `resolve()` trả về [Action] để caller execute.
 *
 * **3 case xử lý:**
 *  1. Match firestoreId → bản đã sync v13+. So sánh `lastModifiedMs` quyết định
 *     overwrite hay giữ local.
 *  2. Không match firestoreId nhưng match composite (ownerUid + date + name +
 *     totalWeight) → card đã sync ở version cũ chưa có firestoreId field.
 *     Stamp firestoreId cho row đó. **Đây là fix bug duplicate khi logout/login.**
 *  3. Cả 2 đều miss → card mới hoàn toàn (máy khác tạo) → insert.
 */
object PullDedupResolver {

    sealed class Action {
        /** Card mới — caller insert vào Room. */
        data class Insert(val fsCard: FirestoreCard) : Action()

        /** Local đã có (match firestoreId hoặc composite) — caller update + stamp firestoreId. */
        data class Update(val localId: Long, val fsCard: FirestoreCard, val reason: Reason) : Action()

        /** Local newer hơn cloud — giữ local, không ghi đè. SyncWorker push sẽ đẩy lên sau. */
        data class Skip(val localId: Long, val reason: String) : Action()
    }

    enum class Reason {
        FIRESTORE_ID_MATCH_NEWER,    // match fsId, cloud newer → overwrite
        COMPOSITE_KEY_MATCH          // không match fsId nhưng match composite (legacy card chưa có fsId)
    }

    /**
     * @param fsCard card từ Firestore
     * @param byFirestoreId lookup local card theo fsCard.id (null nếu không có)
     * @param byCompositeKey lookup local orphan (firestoreId=null) theo
     *                       (date + name + totalWeight) — chỉ gọi khi byFirestoreId null
     */
    suspend fun resolve(
        fsCard: FirestoreCard,
        byFirestoreId: Card?,
        byCompositeKey: suspend () -> Card?
    ): Action {
        // Skip card không có Firestore id (sai data hoặc local-only card).
        if (fsCard.id.isBlank()) {
            return Action.Skip(localId = 0, reason = "blank_firestore_id")
        }

        // Case 1: match firestoreId.
        if (byFirestoreId != null) {
            val cloudNewer = fsCard.lastModifiedMs > byFirestoreId.lastModifiedMs
            return if (cloudNewer) {
                Action.Update(byFirestoreId.id, fsCard, Reason.FIRESTORE_ID_MATCH_NEWER)
            } else {
                Action.Skip(byFirestoreId.id, reason = "local_newer_or_equal")
            }
        }

        // Case 2: match composite key (legacy card từ pre-v13 sync chưa có firestoreId).
        val orphan = byCompositeKey()
        if (orphan != null) {
            return Action.Update(orphan.id, fsCard, Reason.COMPOSITE_KEY_MATCH)
        }

        // Case 3: card mới hoàn toàn.
        return Action.Insert(fsCard)
    }
}

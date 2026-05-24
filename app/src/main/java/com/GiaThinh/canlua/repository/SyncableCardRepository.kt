package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decorator repository — bọc [CardRepository] để mỗi mutation Room đều push
 * tương ứng lên Firestore nếu thiết bị online. Tách thành lớp riêng để
 * `CardRepository` không phụ thuộc `SyncManager` (tránh circular DI).
 *
 * **Delete sync (Sprint 1A)**: dùng `firestoreId` đã lưu sẵn từ Pha 2 để
 * xoá đúng doc trên cloud → User xoá phiếu trên máy A, máy B pull về sẽ
 * không còn thấy.
 *
 * **Entries/Transactions cần `cardFirestoreId`**: lookup từ Card cha; nếu
 * card chưa từng push (firestoreId = null) → push card trước rồi sync entry.
 * Lý do: WeightEntry trên Firestore tham chiếu `cardId: String` (doc id),
 * không phải Room Long id.
 */
@Singleton
class SyncableCardRepository @Inject constructor(
    private val cardRepository: CardRepository,
    private val syncManager: SyncManager,
    private val firestoreRepository: FirestoreRepository,
    private val authManager: AuthManager,
    private val locationProvider: LocationProvider
) {
    fun getAllCards(): Flow<List<Card>> = cardRepository.getAllCards()

    fun getDistinctRiceVarieties(): Flow<List<String>> = cardRepository.getDistinctRiceVarieties()

    fun getSuggestedRiceVarieties(): Flow<List<String>> = cardRepository.getSuggestedRiceVarieties()

    fun getDistinctSeasons(): Flow<List<String>> = cardRepository.getDistinctSeasons()

    suspend fun getCardById(id: Long) = cardRepository.getCardById(id)

    suspend fun insertCard(card: Card): Long {
        // Auto-capture GPS nếu card chưa có toạ độ và app có quyền.
        // forceFresh=true vì phiếu mới cần geo chính xác lúc cân.
        val cardWithGps = if (card.latitude == null || card.longitude == null) {
            val geo = locationProvider.getCurrentLocation(forceFresh = true)
            if (geo != null) card.copy(latitude = geo.lat, longitude = geo.lon) else card
        } else card

        val id = cardRepository.insertCard(cardWithGps)
        if (syncManager.isOnline()) {
            val cardWithId = cardWithGps.copy(id = id)
            syncManager.syncCard(cardWithId)
        } else {
            // Offline → enqueue OneTimeWorkRequest, WorkManager sẽ tự chạy
            // SyncWorker khi có mạng (không phải đợi periodic 12h).
            syncManager.scheduleImmediateSync()
        }
        return id
    }

    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
        if (syncManager.isOnline()) {
            syncManager.syncCard(card)
        } else {
            syncManager.scheduleImmediateSync()
        }
    }

    /**
     * Xoá card local + Firestore (nếu đã từng sync).
     * `CardRepository.deleteCard` đã ghi tombstone trước khi xoá Room → bug
     * "phiếu phục sinh" sau pull được fix triệt để (pull check tombstone trước).
     *
     * Trên Firestore không có CASCADE → xoá tay child docs trước, parent sau
     * để tránh orphan rác trên cloud. Nếu offline → tombstone giữ
     * `cloudDeleted = false`, background worker retry.
     */
    suspend fun deleteCard(card: Card) {
        val fsCardId = card.firestoreId
        // 1. Ghi tombstone + xoá Room TRƯỚC.
        //    Lý do: nếu mình xoá cloud trước, app crash giữa chừng → cloud đã
        //    xoá nhưng local còn → user thấy phiếu trên 1 máy, máy khác mất.
        //    Ghi tombstone trước đảm bảo có "ý định xoá" persisted.
        cardRepository.deleteCard(card)

        // 2. Đẩy delete lên cloud nếu online + có firestoreId.
        if (fsCardId != null && syncManager.isOnline()) {
            val ok = runCatching {
                firestoreRepository.getWeightEntriesByCardId(fsCardId).getOrNull().orEmpty()
                    .forEach { entry ->
                        if (entry.id.isNotBlank()) firestoreRepository.deleteWeightEntry(entry.id)
                    }
                firestoreRepository.getTransactionsByCardId(fsCardId).getOrNull().orEmpty()
                    .forEach { tx ->
                        if (tx.id.isNotBlank()) firestoreRepository.deleteTransaction(tx.id)
                    }
                firestoreRepository.deleteCard(fsCardId).getOrNull()
            }.isSuccess

            // Nếu xoá cloud thành công → đánh dấu tombstone đã clean cloud,
            // không retry trong background nữa.
            if (ok) {
                val uid = authManager.currentUser?.uid.orEmpty()
                cardRepository.getDeletedCards(uid).first()
                    .firstOrNull { it.firestoreId == fsCardId }
                    ?.let { cardRepository.markTombstoneCloudDeleted(it.id) }
            }
        }
    }

    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        cardRepository.getWeightEntriesByCardId(cardId)

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long {
        val id = cardRepository.insertWeightEntry(weightEntry)
        syncEntryIfPossible(weightEntry.copy(id = id))
        return id
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) {
        cardRepository.updateWeightEntry(weightEntry)
        syncEntryIfPossible(weightEntry)
    }

    /**
     * Xoá entry local + Firestore. Nếu entry chưa từng sync (firestoreId=null)
     * thì chỉ xoá local — không có gì trên cloud để xoá.
     */
    suspend fun deleteWeightEntry(weightEntry: WeightEntry) {
        val fsId = weightEntry.firestoreId
        if (fsId != null && syncManager.isOnline()) {
            firestoreRepository.deleteWeightEntry(fsId)
        }
        cardRepository.deleteWeightEntry(weightEntry)
    }

    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        cardRepository.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = cardRepository.insertTransaction(transaction)
        syncTransactionIfPossible(transaction.copy(id = id))
        return id
    }

    suspend fun calculateCardTotals(cardId: Long) =
        cardRepository.calculateCardTotals(cardId)

    suspend fun updateCardCalculations(cardId: Long) {
        cardRepository.updateCardCalculations(cardId)
        cardRepository.getCardById(cardId)?.let { updatedCard ->
            if (syncManager.isOnline()) {
                syncManager.syncCard(updatedCard)
            } else {
                syncManager.scheduleImmediateSync()
            }
        }
    }

    suspend fun findByQrToken(token: String): Card? = cardRepository.findByQrToken(token)

    suspend fun updateQrToken(cardId: Long, token: String) {
        cardRepository.updateQrToken(cardId, token)
        cardRepository.getCardById(cardId)?.let { updatedCard ->
            if (syncManager.isOnline()) {
                syncManager.syncCard(updatedCard)
            } else {
                syncManager.scheduleImmediateSync()
            }
        }
    }

    suspend fun lockCard(cardId: Long, traderId: String) {
        cardRepository.lockCard(cardId, traderId)
        cardRepository.getCardById(cardId)?.let { updatedCard ->
            if (syncManager.isOnline()) {
                syncManager.syncCard(updatedCard)
            } else {
                syncManager.scheduleImmediateSync()
            }
        }
    }

    /**
     * Đồng bộ 1 weight entry lên Firestore. Yêu cầu card cha đã có firestoreId
     * (entry tham chiếu cardId là Firestore doc id, không phải Room id).
     * Nếu card cha chưa sync — push card trước rồi push entry.
     * Offline → noop, để background sync xử lý.
     */
    private suspend fun syncEntryIfPossible(weightEntry: WeightEntry) {
        if (!syncManager.isOnline()) {
            // Offline → defer cho retry queue. SyncWorker sẽ pickup khi có mạng.
            syncManager.scheduleImmediateSync()
            return
        }
        val parent = cardRepository.getCardById(weightEntry.cardId) ?: return
        val cardFsId = parent.firestoreId ?: run {
            syncManager.syncCard(parent).getOrNull() ?: return
        }
        syncManager.syncWeightEntry(weightEntry, cardFsId)
    }

    private suspend fun syncTransactionIfPossible(transaction: Transaction) {
        if (!syncManager.isOnline()) {
            syncManager.scheduleImmediateSync()
            return
        }
        val parent = cardRepository.getCardById(transaction.cardId) ?: return
        val cardFsId = parent.firestoreId ?: run {
            syncManager.syncCard(parent).getOrNull() ?: return
        }
        syncManager.syncTransaction(transaction, cardFsId)
    }
}

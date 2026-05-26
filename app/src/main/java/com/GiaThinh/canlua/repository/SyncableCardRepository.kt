package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncJobs = ConcurrentHashMap<Long, Job>()

    private fun scheduleDebouncedCardSync(cardId: Long) {
        syncJobs[cardId]?.cancel()
        syncJobs[cardId] = scope.launch {
            delay(5000) // Trì hoãn 5 giây để gom cụm các sự kiện gõ liên tiếp
            syncJobs.remove(cardId)
            if (syncManager.isWifiConnected()) {
                syncManager.syncCardAndDetails(cardId)
            } else {
                syncManager.scheduleImmediateSync()
            }
        }
    }

    fun getAllCards(): Flow<List<Card>> = cardRepository.getAllCards()

    fun getDistinctRiceVarieties(): Flow<List<String>> = cardRepository.getDistinctRiceVarieties()

    fun getSuggestedRiceVarieties(): Flow<List<String>> = cardRepository.getSuggestedRiceVarieties()

    fun getDistinctSeasons(): Flow<List<String>> = cardRepository.getDistinctSeasons()

    suspend fun getCardById(id: Long) = cardRepository.getCardById(id)

    suspend fun insertCard(card: Card): Long {
        val id = cardRepository.insertCard(card)
        if (syncManager.isWifiConnected()) {
            val cardWithId = card.copy(id = id)
            syncManager.syncCard(cardWithId)
        } else {
            // Offline/No Wifi → enqueue OneTimeWorkRequest, WorkManager sẽ tự chạy
            // SyncWorker khi có mạng và kết nối Wi-Fi.
            syncManager.scheduleImmediateSync()
        }
        return id
    }

    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
        scheduleDebouncedCardSync(card.id)
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
        if (fsCardId != null && syncManager.isWifiConnected()) {
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
        scheduleDebouncedCardSync(weightEntry.cardId)
        return id
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) {
        cardRepository.updateWeightEntry(weightEntry)
        scheduleDebouncedCardSync(weightEntry.cardId)
    }

    /**
     * Xoá entry local + Firestore. Nếu entry chưa từng sync (firestoreId=null)
     * thì chỉ xoá local — không có gì trên cloud để xoá.
     */
    suspend fun deleteWeightEntry(weightEntry: WeightEntry) {
        val fsId = weightEntry.firestoreId
        if (fsId != null && syncManager.isWifiConnected()) {
            firestoreRepository.deleteWeightEntry(fsId)
        }
        cardRepository.deleteWeightEntry(weightEntry)
        scheduleDebouncedCardSync(weightEntry.cardId)
    }

    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        cardRepository.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = cardRepository.insertTransaction(transaction)
        scheduleDebouncedCardSync(transaction.cardId)
        return id
    }

    suspend fun calculateCardTotals(cardId: Long) =
        cardRepository.calculateCardTotals(cardId)

    suspend fun updateCardCalculations(cardId: Long) {
        cardRepository.updateCardCalculations(cardId)
        scheduleDebouncedCardSync(cardId)
    }

    suspend fun findByQrToken(token: String): Card? = cardRepository.findByQrToken(token)

    suspend fun updateQrToken(cardId: Long, token: String) {
        cardRepository.updateQrToken(cardId, token)
        scheduleDebouncedCardSync(cardId)
    }

    suspend fun lockCard(cardId: Long, traderId: String) {
        cardRepository.lockCard(cardId, traderId)
        // Lock card là hành động one-off quan trọng cuối cùng, đồng bộ tức thì
        cardRepository.getCardById(cardId)?.let { updatedCard ->
            if (syncManager.isWifiConnected()) {
                syncManager.syncCard(updatedCard)
            } else {
                syncManager.scheduleImmediateSync()
            }
        }
    }

}


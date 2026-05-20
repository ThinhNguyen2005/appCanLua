package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow
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
    private val locationProvider: LocationProvider
) {
    fun getAllCards(): Flow<List<Card>> = cardRepository.getAllCards()

    suspend fun getCardById(id: Long) = cardRepository.getCardById(id)

    suspend fun insertCard(card: Card): Long {
        // Auto-capture GPS nếu card chưa có toạ độ và app có quyền
        val cardWithGps = if (card.latitude == null || card.longitude == null) {
            val geo = locationProvider.getCurrentLocation()
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
     * FK CASCADE trong Room đã xoá weight_entries + transactions con local;
     * trên Firestore không có CASCADE → xoá tay child docs trước, parent sau
     * để tránh orphan rác trên cloud.
     */
    suspend fun deleteCard(card: Card) {
        val fsCardId = card.firestoreId
        // Xoá child trên Firestore trước khi parent local bị mất link.
        if (fsCardId != null && syncManager.isOnline()) {
            // Get child docs từ Firestore (có thể nhiều hơn local nếu device khác đã sync)
            firestoreRepository.getWeightEntriesByCardId(fsCardId).getOrNull().orEmpty()
                .forEach { entry ->
                    if (entry.id.isNotBlank()) firestoreRepository.deleteWeightEntry(entry.id)
                }
            firestoreRepository.getTransactionsByCardId(fsCardId).getOrNull().orEmpty()
                .forEach { tx ->
                    if (tx.id.isNotBlank()) firestoreRepository.deleteTransaction(tx.id)
                }
            firestoreRepository.deleteCard(fsCardId)
        }
        cardRepository.deleteCard(card)
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

    suspend fun updateCardCalculations(cardId: Long) =
        cardRepository.updateCardCalculations(cardId)

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

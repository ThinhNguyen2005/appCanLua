package com.giathinh.canlua.repository

import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.WeightEntry
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
    private val authManager: AuthManager,
    private val locationProvider: LocationProvider
) {
    // Sync to Firestore disabled for performance and cost optimization in 2026 local-first refactor

    fun getAllCards(): Flow<List<Card>> = cardRepository.getAllCards()

    fun getDistinctRiceVarieties(): Flow<List<String>> = cardRepository.getDistinctRiceVarieties()

    fun getSuggestedRiceVarieties(): Flow<List<String>> = cardRepository.getSuggestedRiceVarieties()

    fun getDistinctSeasons(): Flow<List<String>> = cardRepository.getDistinctSeasons()

    suspend fun getCardById(id: Long) = cardRepository.getCardById(id)

    suspend fun insertCard(card: Card): Long {
        return cardRepository.insertCard(card)
    }

    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
    }

    /**
     * Xoá card local.
     */
    suspend fun deleteCard(card: Card) {
        cardRepository.deleteCard(card)
    }

    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        cardRepository.getWeightEntriesByCardId(cardId)

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long {
        return cardRepository.insertWeightEntry(weightEntry)
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) {
        cardRepository.updateWeightEntry(weightEntry)
    }

    /**
     * Xoá entry local.
     */
    suspend fun deleteWeightEntry(weightEntry: WeightEntry) {
        cardRepository.deleteWeightEntry(weightEntry)
    }

    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        cardRepository.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long {
        return cardRepository.insertTransaction(transaction)
    }

    suspend fun calculateCardTotals(cardId: Long) =
        cardRepository.calculateCardTotals(cardId)

    suspend fun updateCardCalculations(cardId: Long) {
        cardRepository.updateCardCalculations(cardId)
    }

    suspend fun lockCard(cardId: Long, traderId: String) {
        cardRepository.lockCard(cardId, traderId)
    }


}


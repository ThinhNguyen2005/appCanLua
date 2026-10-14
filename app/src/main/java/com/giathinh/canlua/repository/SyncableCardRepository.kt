package com.giathinh.canlua.repository

import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decorator repository — bọc [CardRepository] trong kiến trúc Local-First.
 * Mọi thao tác ghi đều thực hiện tức thì trên Room, sau đó đánh dấu trạng thái
 * chờ đồng bộ (`refreshPendingSyncState`) và đặt lịch gom batch qua WorkManager
 * (`scheduleImmediateSync`) nếu người dùng bật Tự động đồng bộ.
 */
@Singleton
class SyncableCardRepository @Inject constructor(
    private val cardRepository: CardRepository,
    private val syncManager: SyncManager
) {
    private suspend fun onLocalMutation() {
        syncManager.refreshPendingSyncState()
        syncManager.scheduleImmediateSync()
    }

    fun getAllCards(): Flow<List<Card>> = cardRepository.getAllCards()

    fun getDistinctRiceVarieties(): Flow<List<String>> = cardRepository.getDistinctRiceVarieties()

    fun getSuggestedRiceVarieties(): Flow<List<String>> = cardRepository.getSuggestedRiceVarieties()

    fun getDistinctSeasons(): Flow<List<String>> = cardRepository.getDistinctSeasons()

    suspend fun getCardById(id: Long): Card? = cardRepository.getCardById(id)

    suspend fun insertCard(card: Card): Long {
        val id = cardRepository.insertCard(card)
        onLocalMutation()
        return id
    }

    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
        onLocalMutation()
    }

    suspend fun deleteCard(card: Card) {
        cardRepository.deleteCard(card)
        onLocalMutation()
    }

    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        cardRepository.getWeightEntriesByCardId(cardId)

    suspend fun addWeightEntryAndRecalc(cardId: Long, weight: Double): Int? {
        val count = cardRepository.addWeightEntryAndRecalc(cardId, weight)
        if (count != null) {
            onLocalMutation()
        }
        return count
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) {
        cardRepository.updateWeightEntry(weightEntry)
        onLocalMutation()
    }

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) {
        cardRepository.deleteWeightEntry(weightEntry)
        onLocalMutation()
    }

    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        cardRepository.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = cardRepository.insertTransaction(transaction)
        onLocalMutation()
        return id
    }

    suspend fun updateCardCalculations(cardId: Long) {
        cardRepository.updateCardCalculations(cardId)
        onLocalMutation()
    }
}
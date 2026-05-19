package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper repository that adds sync functionality to CardRepository
 * This separates concerns and avoids circular dependencies
 */
@Singleton
class SyncableCardRepository @Inject constructor(
    private val cardRepository: CardRepository,
    private val syncManager: SyncManager,
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
        // Sync to Firestore if online
        if (syncManager.isOnline()) {
            val cardWithId = cardWithGps.copy(id = id)
            syncManager.syncCard(cardWithId)
        }
        return id
    }

    suspend fun updateCard(card: Card) {
        cardRepository.updateCard(card)
        // Sync to Firestore if online
        if (syncManager.isOnline()) {
            syncManager.syncCard(card)
        }
    }

    suspend fun deleteCard(card: Card) {
        cardRepository.deleteCard(card)
        // Note: Firestore deletion would need firestoreId mapping
    }

    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        cardRepository.getWeightEntriesByCardId(cardId)

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long {
        return cardRepository.insertWeightEntry(weightEntry)
        // Sync will be handled when we add ID mapping
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) =
        cardRepository.updateWeightEntry(weightEntry)

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) =
        cardRepository.deleteWeightEntry(weightEntry)

    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        cardRepository.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long {
        return cardRepository.insertTransaction(transaction)
        // Sync will be handled when we add ID mapping
    }

    suspend fun calculateCardTotals(cardId: Long) =
        cardRepository.calculateCardTotals(cardId)

    suspend fun updateCardCalculations(cardId: Long) =
        cardRepository.updateCardCalculations(cardId)
}


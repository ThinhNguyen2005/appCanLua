package com.GiaThinh.canlua.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.GiaThinh.canlua.data.firestore.FirestoreCard
import com.GiaThinh.canlua.data.firestore.FirestoreTransaction
import com.GiaThinh.canlua.data.firestore.FirestoreWeightEntry
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.WeightEntry
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SyncManager @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val cardRepository: CardRepository,
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun syncAll(): Result<Unit> {
        if (auth.currentUser == null) {
            _syncStatus.value = SyncStatus.Error("Yêu cầu đăng nhập để đồng bộ")
            return Result.failure(Exception("No user signed in"))
        }
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.Error("Không có kết nối internet")
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            _syncStatus.value = SyncStatus.Syncing

            // Sync cards - get first value from Flow
            val localCards = cardRepository.getAllCards().first()
            var syncedCount = 0
            var errorCount = 0
            
            localCards.forEach { card ->
                val cardResult = syncCard(card)
                cardResult.onSuccess { firestoreId ->
                    // Sync weight entries for this card
                    val weightEntries = cardRepository.getWeightEntriesByCardId(card.id).first()
                    weightEntries.forEach { entry ->
                        syncWeightEntry(entry, firestoreId)
                    }
                    
                    // Sync transactions for this card
                    val transactions = cardRepository.getTransactionsByCardId(card.id).first()
                    transactions.forEach { transaction ->
                        syncTransaction(transaction, firestoreId)
                    }
                    
                    syncedCount++
                }.onFailure {
                    errorCount++
                }
            }

            if (errorCount > 0 && syncedCount == 0) {
                _syncStatus.value = SyncStatus.Error("Không thể đồng bộ dữ liệu. Vui lòng thử lại.")
                Result.failure(Exception("Sync failed for all cards"))
            } else {
                _syncStatus.value = SyncStatus.Success
                _lastSyncTime.value = System.currentTimeMillis()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Lỗi đồng bộ")
            Result.failure(e)
        }
    }

    suspend fun syncCard(card: Card): Result<String> {
        if (auth.currentUser == null) {
            return Result.failure(Exception("No user signed in"))
        }
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            val firestoreCard = card.toFirestoreCard()
            val result = firestoreRepository.syncCard(firestoreCard)
            result.onSuccess { firestoreId ->
                // Update local card with firestore ID if needed
                // You might want to store the mapping in a separate table
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWeightEntry(weightEntry: WeightEntry, cardFirestoreId: String): Result<String> {
        if (auth.currentUser == null) {
            return Result.failure(Exception("No user signed in"))
        }
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            val firestoreEntry = weightEntry.toFirestoreWeightEntry(cardFirestoreId)
            firestoreRepository.syncWeightEntry(firestoreEntry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncTransaction(transaction: Transaction, cardFirestoreId: String): Result<String> {
        if (auth.currentUser == null) {
            return Result.failure(Exception("No user signed in"))
        }
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            val firestoreTransaction = transaction.toFirestoreTransaction(cardFirestoreId)
            firestoreRepository.syncTransaction(firestoreTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extension functions to convert local models to Firestore models
    private fun Card.toFirestoreCard(): FirestoreCard {
        return FirestoreCard(
            id = "", // Will be generated by Firestore
            name = this.name,
            cccd = this.cccd,
            date = this.date,
            totalWeight = this.totalWeight,
            bagWeight = this.bagWeight,
            impurityWeight = this.impurityWeight,
            netWeight = this.netWeight,
            depositAmount = this.depositAmount,
            pricePerKg = this.pricePerKg,
            totalAmount = this.totalAmount,
            paidAmount = this.paidAmount,
            remainingAmount = this.remainingAmount,
            bagCount = this.bagCount,
            isLocked = this.isLocked,
            traderName = this.traderName,
            localId = this.id
        )
    }

    private fun WeightEntry.toFirestoreWeightEntry(cardFirestoreId: String): FirestoreWeightEntry {
        return FirestoreWeightEntry(
            id = "",
            cardId = cardFirestoreId,
            weight = this.weight,
            bagWeight = this.bagWeight,
            impurityWeight = this.impurityWeight,
            netWeight = this.netWeight,
            timestamp = this.timestamp,
            localId = this.id
        )
    }

    private fun Transaction.toFirestoreTransaction(cardFirestoreId: String): FirestoreTransaction {
        return FirestoreTransaction(
            id = "",
            cardId = cardFirestoreId,
            amount = this.amount,
            type = this.type.name,
            description = this.description,
            date = this.date,
            localId = this.id
        )
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}


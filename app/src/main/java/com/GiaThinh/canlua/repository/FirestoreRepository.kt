package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.firestore.FirestoreCard
import com.GiaThinh.canlua.data.firestore.FirestoreTransaction
import com.GiaThinh.canlua.data.firestore.FirestoreWeightEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    // Cards collection
    private val cardsCollection
        get() = firestore.collection("cards")

    // Weight entries collection
    private val weightEntriesCollection
        get() = firestore.collection("weightEntries")

    // Transactions collection
    private val transactionsCollection
        get() = firestore.collection("transactions")

    // ========== Card Operations ==========

    suspend fun syncCard(card: FirestoreCard): Result<String> {
        return try {
            val cardWithUser = card.copy(
                userId = userId,
                syncTimestamp = System.currentTimeMillis()
            )
            val docRef = if (card.id.isNotEmpty()) {
                cardsCollection.document(card.id)
            } else {
                cardsCollection.document()
            }
            docRef.set(cardWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCardById(cardId: String): Result<FirestoreCard?> {
        return try {
            val document = cardsCollection.document(cardId).get().await()
            if (document.exists()) {
                val card = document.toObject(FirestoreCard::class.java)
                Result.success(card)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCards(): Result<List<FirestoreCard>> {
        return try {
            val query = userId?.let {
                cardsCollection.whereEqualTo("userId", it)
            } ?: cardsCollection

            val snapshot = query.get().await()
            val cards = snapshot.documents.mapNotNull { it.toObject(FirestoreCard::class.java) }
            Result.success(cards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCard(cardId: String): Result<Unit> {
        return try {
            cardsCollection.document(cardId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Weight Entry Operations ==========

    suspend fun syncWeightEntry(weightEntry: FirestoreWeightEntry): Result<String> {
        return try {
            val entryWithUser = weightEntry.copy(
                userId = userId,
                syncTimestamp = System.currentTimeMillis()
            )
            val docRef = if (weightEntry.id.isNotEmpty()) {
                weightEntriesCollection.document(weightEntry.id)
            } else {
                weightEntriesCollection.document()
            }
            docRef.set(entryWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeightEntriesByCardId(cardId: String): Result<List<FirestoreWeightEntry>> {
        return try {
            // NOTE: Sort client-side để tránh composite index (whereEqualTo + orderBy).
            val snapshot = weightEntriesCollection
                .whereEqualTo("cardId", cardId)
                .get()
                .await()
            val entries = snapshot.documents
                .mapNotNull { it.toObject(FirestoreWeightEntry::class.java) }
                .sortedByDescending { it.timestamp }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWeightEntry(entryId: String): Result<Unit> {
        return try {
            weightEntriesCollection.document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Transaction Operations ==========

    suspend fun syncTransaction(transaction: FirestoreTransaction): Result<String> {
        return try {
            val transactionWithUser = transaction.copy(
                userId = userId,
                syncTimestamp = System.currentTimeMillis()
            )
            val docRef = if (transaction.id.isNotEmpty()) {
                transactionsCollection.document(transaction.id)
            } else {
                transactionsCollection.document()
            }
            docRef.set(transactionWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByCardId(cardId: String): Result<List<FirestoreTransaction>> {
        return try {
            // NOTE: Bỏ .orderBy("date") server-side để tránh composite index requirement.
            // Sort client-side — 1 card thường có vài chục transactions, perf không vấn đề.
            val snapshot = transactionsCollection
                .whereEqualTo("cardId", cardId)
                .get()
                .await()
            val transactions = snapshot.documents
                .mapNotNull { it.toObject(FirestoreTransaction::class.java) }
                .sortedByDescending { it.date }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            transactionsCollection.document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Trader Operations (Phase 2.4) ==========

    /**
     * Realtime stream các card mà trader hiện tại đã verify bằng QR
     * (lockedByTraderId == uid).
     */
    fun observeMyTraderCards(): Flow<List<FirestoreCard>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        // NOTE: Bỏ .orderBy("date") server-side để tránh yêu cầu composite index
        // (whereEqualTo + orderBy ở field khác → Firestore bắt buộc composite index).
        // Sort client-side vì 1 trader chỉ có vài chục/trăm card đã verify.
        val registration: ListenerRegistration = cardsCollection
            .whereEqualTo("lockedByTraderId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Không crash app — trả emptyList, để UI tự hiển thị empty state.
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val cards = snapshot?.documents
                    ?.mapNotNull { it.toObject(FirestoreCard::class.java) }
                    .orEmpty()
                    .sortedByDescending { it.date }
                trySend(cards)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Realtime stream transactions thuộc về một list cardId.
     * Firestore `whereIn` tối đa 30 ID per query — đủ cho use case TRADER.
     */
    fun observeTransactionsForCards(cardIds: List<String>): Flow<List<FirestoreTransaction>> = callbackFlow {
        if (cardIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        // Firestore whereIn limit = 30 — chunk nếu cần
        val chunk = cardIds.take(30)
        // NOTE: whereIn + orderBy(field khác) cũng yêu cầu composite index.
        // Sort client-side để tránh phụ thuộc cấu hình Console.
        val registration: ListenerRegistration = transactionsCollection
            .whereIn("cardId", chunk)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(FirestoreTransaction::class.java) }
                    .orEmpty()
                    .sortedByDescending { it.date }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
}


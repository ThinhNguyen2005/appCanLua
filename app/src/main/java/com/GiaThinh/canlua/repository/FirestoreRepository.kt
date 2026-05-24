package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.firestore.FirestoreCard
import com.GiaThinh.canlua.data.firestore.FirestoreTransaction
import com.GiaThinh.canlua.data.firestore.FirestoreWeightEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class QrLockResult {
    data class Success(val firestoreId: String) : QrLockResult()
    data class AlreadyLockedByCurrentTrader(val firestoreId: String) : QrLockResult()
    data object NotFound : QrLockResult()
    data object AlreadyLockedByOtherTrader : QrLockResult()
    data class Error(val message: String?) : QrLockResult()
}

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

    /**
     * Trader xác thực QR thành công → cập nhật trên Firestore
     * để TraderTransactionsScreen (observe lockedByTraderId) thấy được giao dịch.
     *
     * Tìm card theo qrToken (do farmer generate), không theo localId
     * vì 2 thiết bị (farmer/trader) dùng Room ID khác nhau.
     */
    suspend fun lockCardByQrToken(qrToken: String, traderId: String): QrLockResult {
        return try {
            val snapshot = cardsCollection
                .whereEqualTo("qrToken", qrToken)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
                ?: return QrLockResult.NotFound

            firestore.runTransaction { transaction ->
                val fresh = transaction.get(doc.reference)
                val lockedBy = fresh.getString("lockedByTraderId").orEmpty()
                when {
                    lockedBy == traderId -> QrLockResult.AlreadyLockedByCurrentTrader(doc.id)
                    lockedBy.isNotBlank() -> QrLockResult.AlreadyLockedByOtherTrader
                    else -> {
                        transaction.update(
                            doc.reference,
                            mapOf(
                                "lockedByTraderId" to traderId,
                                "isLocked" to true,
                                "syncTimestamp" to System.currentTimeMillis()
                            )
                        )
                        QrLockResult.Success(doc.id)
                    }
                }
            }.await()
        } catch (e: Exception) {
            QrLockResult.Error(e.message)
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
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, error ->
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
     * Realtime stream transactions thuộc về list cardId của trader.
     * Firestore `whereIn` tối đa 30 ID/query, nên phải listen theo nhiều chunk.
     */
    fun observeTransactionsForCards(cardIds: List<String>): Flow<List<FirestoreTransaction>> = callbackFlow {
        if (cardIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chunks = cardIds.distinct().chunked(30)
        val latestByChunk = MutableList(chunks.size) { emptyList<FirestoreTransaction>() }
        val registrations = mutableListOf<ListenerRegistration>()

        chunks.forEachIndexed { index, chunk ->
            val registration = transactionsCollection
                .whereIn("cardId", chunk)
                .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, error ->
                    latestByChunk[index] = if (error != null) {
                        emptyList()
                    } else {
                        snapshot?.documents
                            ?.mapNotNull { it.toObject(FirestoreTransaction::class.java) }
                            .orEmpty()
                    }
                    trySend(latestByChunk.flatten().sortedByDescending { it.date })
                }
            registrations += registration
        }

        awaitClose { registrations.forEach { it.remove() } }
    }

    // ========== Analytics Counter Operations ==========

    suspend fun incrementAiQueryCount(): Result<Unit> {
        return try {
            val uid = userId ?: return Result.failure(IllegalStateException("User not logged in"))
            firestore.collection("profiles").document(uid)
                .update("aiQueryCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementCardCount(): Result<Unit> {
        return try {
            val uid = userId ?: return Result.failure(IllegalStateException("User not logged in"))
            firestore.collection("profiles").document(uid)
                .update("cardCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}



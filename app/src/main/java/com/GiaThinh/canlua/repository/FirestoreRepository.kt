package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.firestore.FirestoreCard
import com.GiaThinh.canlua.data.firestore.FirestoreTransaction
import com.GiaThinh.canlua.data.firestore.FirestoreWeightEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            val snapshot = weightEntriesCollection
                .whereEqualTo("cardId", cardId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val entries = snapshot.documents.mapNotNull { it.toObject(FirestoreWeightEntry::class.java) }
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
            val snapshot = transactionsCollection
                .whereEqualTo("cardId", cardId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            val transactions = snapshot.documents.mapNotNull { it.toObject(FirestoreTransaction::class.java) }
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
}


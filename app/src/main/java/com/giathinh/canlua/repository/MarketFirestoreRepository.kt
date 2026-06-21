package com.giathinh.canlua.repository

import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore CRUD cho collection `market_prices`.
 * Tách riêng để FirestoreRepository không phình ra.
 */
@Singleton
class MarketFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val pricesCollection
        get() = firestore.collection("ricePrices")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /** TRADER nhập / cập nhật bid. */
    suspend fun upsertBid(bid: FirestoreRicePrice): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (bid.id.isNotEmpty()) {
                pricesCollection.document(bid.id)
            } else {
                pricesCollection.document()
            }
            val finalBid = bid.copy(
                id = docRef.id,
                traderId = currentUserId ?: bid.traderId,
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(finalBid).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * One-shot fetch tất cả bids active — dùng khi FARMER mở tab Market.
     * Tránh giữ snapshot listener thường trực; client tự refresh khi cần.
     */
    suspend fun fetchActiveBids(): Result<List<FirestoreRicePrice>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = pricesCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            val bids = snapshot.documents
                .mapNotNull { doc ->
                    doc.toObject(FirestoreRicePrice::class.java)?.copy(id = doc.id)
                }
                .sortedByDescending { it.updatedAt }
            Result.success(bids)
        } catch (e: Exception) {
            android.util.Log.w("MarketRepo", "fetchActiveBids error", e)
            Result.failure(e)
        }
    }

    /** TRADER xoá / ẩn bid. */
    suspend fun deactivateBid(bidId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pricesCollection.document(bidId)
                .update(mapOf("active" to false, "updatedAt" to System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Realtime stream tất cả bids active — cho FARMER market screen.
     *
     * NOTE: Sort client-side để không cần composite index `(active, updatedAt)` trên Firestore.
     * Listener KHÔNG bao giờ crash app — lỗi mạng/permission chỉ trả empty list.
     */


    /**
     * Realtime stream bids của TRADER hiện tại.
     *
     * NOTE: Sort client-side để không cần composite index `(traderId, updatedAt)` trên Firestore.
     */
    fun observeMyBids(): Flow<List<FirestoreRicePrice>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration: ListenerRegistration = pricesCollection
            .whereEqualTo("traderId", uid)
            .addSnapshotListener(Dispatchers.IO.asExecutor(), MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("MarketRepo", "observeMyBids error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val isFromCache = snapshot?.metadata?.isFromCache ?: false
                val bids = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(FirestoreRicePrice::class.java)?.copy(id = doc.id)?.apply {
                            this.isFromCache = isFromCache
                        }
                    }
                    ?.sortedByDescending { it.updatedAt }
                    .orEmpty()
                trySend(bids)
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()
}

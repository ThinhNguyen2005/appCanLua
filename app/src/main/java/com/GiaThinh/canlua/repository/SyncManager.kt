package com.GiaThinh.canlua.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
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
    private val cardDao: CardDao,
    private val weightEntryDao: WeightEntryDao,
    private val transactionDao: TransactionDao,
    @param:ApplicationContext private val context: Context,
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

    /**
     * Enqueue 1 lần SyncWorker chạy ngay khi có mạng (không đợi periodic 12h).
     *
     * Trigger sau mỗi mutation Room offline (insert/update card hoặc weight entry
     * khi `!isOnline()`). Tận dụng WorkManager để:
     *  - Constraint `NETWORK_CONNECTED` tự defer cho đến khi có mạng.
     *  - `BackoffPolicy.EXPONENTIAL` retry với delay tăng dần khi Firestore fail
     *    (timeout, 5xx, network blip) — tránh DDOS server.
     *  - `ExistingWorkPolicy.KEEP` — không enqueue trùng nếu đã có work pending,
     *    SyncWorker sẽ pickup hết changes khi nó chạy.
     *
     * Reuse `SyncWorker` đã có (gọi `syncManager.syncAll()`).
     */
    fun scheduleImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                java.util.concurrent.TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync-immediate",
            ExistingWorkPolicy.KEEP,
            request
        )
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

            // Sau khi push xong, pull các thay đổi từ máy khác về.
            // Idempotent — pull dedup theo firestoreId nên không tạo duplicate.
            pullAllForCurrentUser()

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
            // Reuse firestoreId nếu đã từng push để Firestore update đúng doc cũ.
            val firestoreCard = card.toFirestoreCard().copy(id = card.firestoreId.orEmpty())
            val result = firestoreRepository.syncCard(firestoreCard)
            result.onSuccess { firestoreId ->
                // Stamp lại firestoreId vào Room để lần push sau update đúng doc,
                // và để pull dedup được khi user login máy khác.
                if (card.firestoreId != firestoreId) {
                    cardDao.updateFirestoreId(card.id, firestoreId)
                }
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
                .copy(id = weightEntry.firestoreId.orEmpty())
            val result = firestoreRepository.syncWeightEntry(firestoreEntry)
            result.onSuccess { fsId ->
                if (weightEntry.firestoreId != fsId) {
                    weightEntryDao.updateFirestoreId(weightEntry.id, fsId)
                }
            }
            result
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
                .copy(id = transaction.firestoreId.orEmpty())
            val result = firestoreRepository.syncTransaction(firestoreTransaction)
            result.onSuccess { fsId ->
                if (transaction.firestoreId != fsId) {
                    transactionDao.updateFirestoreId(transaction.id, fsId)
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull cards + weight entries + transactions từ Firestore về Room cho user
     * hiện tại. Idempotent — dedup theo `firestoreId`.
     *
     * Conflict resolution:
     *  - **Cards**: cloud wins (overwrite local) khi `firestoreId` khớp. User
     *    không sửa card nhiều cross-device nên trade-off chấp nhận được.
     *  - **Weight entries / Transactions**: insert-only. Đã có local
     *    (firestoreId match) → skip. Chúng là append-mostly nên đơn giản hơn.
     *
     * Trigger:
     *  - Tự động sau sign-in (CanLuaApplication observe authStateFlow).
     *  - Manual qua nút "Đồng bộ" (gọi `syncAll` → push → pull).
     */
    suspend fun pullAllForCurrentUser(): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(
            Exception("No user signed in")
        )
        if (!isOnline()) return Result.failure(Exception("No internet connection"))

        return try {
            val cardsResult = firestoreRepository.getAllCards()
            val firestoreCards = cardsResult.getOrElse {
                return Result.failure(it)
            }

            // Map firestoreId → local Room id để pull entries/transactions sau.
            val firestoreToLocalId = mutableMapOf<String, Long>()

            firestoreCards.forEach { fsCard ->
                if (fsCard.id.isBlank()) return@forEach
                val existing = cardDao.getByFirestoreId(fsCard.id)
                val localId = if (existing != null) {
                    // Conflict resolution: chỉ overwrite local khi cloud mới hơn.
                    // `lastModifiedMs` là source of truth — `syncTimestamp` chỉ là
                    // "lúc nào push" (có thể bị skew clock máy khác).
                    val cloudNewer = fsCard.lastModifiedMs > existing.lastModifiedMs
                    if (cloudNewer) {
                        cardDao.updateCard(fsCard.toLocalCard(currentUid, existingId = existing.id))
                    }
                    // Local mới hơn → giữ. SyncWorker push sẽ đẩy bản local lên cloud.
                    existing.id
                } else {
                    cardDao.insertCard(fsCard.toLocalCard(currentUid, existingId = 0L))
                }
                firestoreToLocalId[fsCard.id] = localId
            }

            // Pull weight entries + transactions cho từng card đã pull.
            // Lỗi 1 card không phá toàn bộ — log silent qua getOrNull.
            firestoreToLocalId.forEach { (fsCardId, localCardId) ->
                val entries = firestoreRepository.getWeightEntriesByCardId(fsCardId)
                    .getOrNull().orEmpty()
                entries.forEach { fsEntry ->
                    if (fsEntry.id.isBlank()) return@forEach
                    val existing = weightEntryDao.getByFirestoreId(fsEntry.id)
                    if (existing == null) {
                        weightEntryDao.insertWeightEntry(fsEntry.toLocalEntry(localCardId))
                    }
                }

                val transactions = firestoreRepository.getTransactionsByCardId(fsCardId)
                    .getOrNull().orEmpty()
                transactions.forEach { fsTx ->
                    if (fsTx.id.isBlank()) return@forEach
                    val existing = transactionDao.getByFirestoreId(fsTx.id)
                    if (existing == null) {
                        transactionDao.insertTransaction(fsTx.toLocalTransaction(localCardId))
                    }
                }
            }

            _lastSyncTime.value = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            // Pull lỗi không set SyncStatus.Error vì có thể đang chạy trong background;
            // user vẫn dùng app bình thường với data offline.
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers — Local ⇄ Firestore
    // ─────────────────────────────────────────────────────────────────────────

    private fun Card.toFirestoreCard(): FirestoreCard {
        return FirestoreCard(
            id = "", // Will be generated by Firestore (or override với firestoreId trong syncCard)
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
            riceVariety = this.riceVariety,
            moisturePercent = this.moisturePercent,
            seasonLabel = this.seasonLabel,
            qrToken = this.qrToken,
            lockedByTraderId = this.lockedByTraderId,
            latitude = this.latitude,
            longitude = this.longitude,
            traderPhone = this.traderPhone,
            fieldAddress = this.fieldAddress,
            lastModifiedMs = this.lastModifiedMs,
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

    private fun FirestoreCard.toLocalCard(ownerUid: String, existingId: Long): Card {
        return Card(
            id = existingId,
            ownerUid = ownerUid,
            firestoreId = this.id,
            lastModifiedMs = this.lastModifiedMs,
            name = this.name,
            cccd = this.cccd,
            traderName = this.traderName,
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
            riceVariety = this.riceVariety,
            moisturePercent = this.moisturePercent,
            seasonLabel = this.seasonLabel,
            qrToken = this.qrToken,
            lockedByTraderId = this.lockedByTraderId,
            latitude = this.latitude,
            longitude = this.longitude,
            traderPhone = this.traderPhone,
            fieldAddress = this.fieldAddress
        )
    }

    private fun FirestoreWeightEntry.toLocalEntry(localCardId: Long): WeightEntry {
        return WeightEntry(
            id = 0,
            cardId = localCardId,
            firestoreId = this.id,
            weight = this.weight,
            bagWeight = this.bagWeight,
            impurityWeight = this.impurityWeight,
            netWeight = this.netWeight,
            timestamp = this.timestamp
        )
    }

    private fun FirestoreTransaction.toLocalTransaction(localCardId: Long): Transaction {
        val txType = runCatching { TransactionType.valueOf(this.type) }
            .getOrDefault(TransactionType.PAYMENT)
        return Transaction(
            id = 0,
            cardId = localCardId,
            firestoreId = this.id,
            amount = this.amount,
            type = txType,
            description = this.description,
            date = this.date
        )
    }
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

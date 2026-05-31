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
import com.GiaThinh.canlua.util.AnalyticsHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository
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

    fun isWifiConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun canSync(): Boolean {
        return if (settingsRepository.isSyncOnlyWifi()) {
            isWifiConnected()
        } else {
            isOnline()
        }
    }

    /**
     * Enqueue 1 lần SyncWorker chạy ngay khi có mạng và Wi-Fi (không đợi periodic 24h).
     *
     * Trigger sau mỗi mutation Room offline (insert/update card hoặc weight entry
     * khi `!isWifiConnected()`). Tận dụng WorkManager để:
     *  - Constraint `NETWORK_UNMETERED` tự defer cho đến khi có mạng Wi-Fi.
     *  - `BackoffPolicy.EXPONENTIAL` retry với delay tăng dần khi Firestore fail
     *    (timeout, 5xx, network blip) — tránh DDOS server.
     *  - `ExistingWorkPolicy.KEEP` — không enqueue trùng nếu đã có work pending,
     *    SyncWorker sẽ pickup hết changes khi nó chạy.
     *
     * Reuse `SyncWorker` đã có (gọi `syncManager.syncAll()`).
     */
    fun scheduleImmediateSync() {
        if (!settingsRepository.isAutoSyncEnabled()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
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

    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            _syncStatus.value = SyncStatus.Error("Yêu cầu đăng nhập để đồng bộ")
            return@withContext Result.failure(Exception("No user signed in"))
        }
        if (!canSync()) {
            val errorMsg = if (settingsRepository.isSyncOnlyWifi()) "Yêu cầu kết nối Wi-Fi để đồng bộ" else "Yêu cầu kết nối mạng để đồng bộ"
            _syncStatus.value = SyncStatus.Error(errorMsg)
            return@withContext Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "Not connected to Wi-Fi" else "Not connected to network"))
        }

        return@withContext try {
            _syncStatus.value = SyncStatus.Syncing
            val startedAt = System.currentTimeMillis()

            // Lấy danh sách card trên cloud để so sánh delta lastModifiedMs
            val cloudCardsResult = firestoreRepository.getAllCards()
            val cloudCards = cloudCardsResult.getOrDefault(emptyList())
            val cloudCardMap = cloudCards.associateBy({ it.id }, { it.lastModifiedMs })

            // Lấy danh sách card local
            val localCards = cardRepository.getAllCards().first()
            
            // Lọc các card cần push (chưa sync hoặc local mới hơn cloud)
            val dirtyCards = localCards.filter { card ->
                card.firestoreId.isNullOrEmpty() ||
                !cloudCardMap.containsKey(card.firestoreId) ||
                card.lastModifiedMs > (cloudCardMap[card.firestoreId] ?: 0L)
            }

            var syncedCount = 0
            var errorCount = 0

            if (dirtyCards.isNotEmpty()) {
                var currentOpCount = 0
                val batchCards = mutableListOf<com.GiaThinh.canlua.data.firestore.FirestoreCard>()
                val batchEntries = mutableListOf<com.GiaThinh.canlua.data.firestore.FirestoreWeightEntry>()
                val batchTxs = mutableListOf<com.GiaThinh.canlua.data.firestore.FirestoreTransaction>()
                var batchNewCardsCount = 0

                val batchCardIdMappings = mutableListOf<Pair<Long, String>>()
                val batchEntryIdMappings = mutableListOf<Pair<Long, String>>()
                val batchTxIdMappings = mutableListOf<Pair<Long, String>>()

                for (card in dirtyCards) {
                    val fsCardId = if (card.firestoreId.isNullOrEmpty()) {
                        firestoreRepository.generateCardId()
                    } else {
                        card.firestoreId
                    }
                    val isNewCard = card.firestoreId.isNullOrEmpty()

                    val weightEntries = cardRepository.getWeightEntriesByCardId(card.id).first()
                    val transactions = cardRepository.getTransactionsByCardId(card.id).first()

                    val cardOps = 1 + weightEntries.size + transactions.size

                    // Commit batch trước đó nếu thêm card này sẽ vượt quá 400 ops
                    if (currentOpCount > 0 && currentOpCount + cardOps > 400) {
                        val batchResult = firestoreRepository.executeBatchSync(
                            cards = batchCards,
                            weightEntries = batchEntries,
                            transactions = batchTxs,
                            newCardsCount = batchNewCardsCount
                        )
                        if (batchResult.isSuccess) {
                            batchCardIdMappings.forEach { (localId, fsId) ->
                                cardDao.updateFirestoreId(localId, fsId)
                            }
                            batchEntryIdMappings.forEach { (localId, fsId) ->
                                weightEntryDao.updateFirestoreId(localId, fsId)
                            }
                            batchTxIdMappings.forEach { (localId, fsId) ->
                                transactionDao.updateFirestoreId(localId, fsId)
                            }
                            syncedCount += batchCards.size
                        } else {
                            errorCount += batchCards.size
                            AnalyticsHelper.logNonFatal(batchResult.exceptionOrNull() ?: Exception("Batch failed"), tag = "sync_batch")
                        }
                        // Reset batch
                        currentOpCount = 0
                        batchCards.clear()
                        batchEntries.clear()
                        batchTxs.clear()
                        batchNewCardsCount = 0
                        batchCardIdMappings.clear()
                        batchEntryIdMappings.clear()
                        batchTxIdMappings.clear()
                    }

                    // Build models
                    batchCards.add(card.toFirestoreCard().copy(id = fsCardId))
                    batchCardIdMappings.add(card.id to fsCardId)
                    if (isNewCard) batchNewCardsCount++

                    weightEntries.forEach { entry ->
                        val fsEntryId = if (entry.firestoreId.isNullOrEmpty()) {
                            firestoreRepository.generateWeightEntryId()
                        } else {
                            entry.firestoreId
                        }
                        batchEntries.add(entry.toFirestoreWeightEntry(fsCardId).copy(id = fsEntryId))
                        batchEntryIdMappings.add(entry.id to fsEntryId)
                    }

                    transactions.forEach { tx ->
                        val fsTxId = if (tx.firestoreId.isNullOrEmpty()) {
                            firestoreRepository.generateTransactionId()
                        } else {
                            tx.firestoreId
                        }
                        batchTxs.add(tx.toFirestoreTransaction(fsCardId).copy(id = fsTxId))
                        batchTxIdMappings.add(tx.id to fsTxId)
                    }

                    currentOpCount += cardOps
                }

                // Commit remaining batch
                if (currentOpCount > 0) {
                    val batchResult = firestoreRepository.executeBatchSync(
                        cards = batchCards,
                        weightEntries = batchEntries,
                        transactions = batchTxs,
                        newCardsCount = batchNewCardsCount
                    )
                    if (batchResult.isSuccess) {
                        batchCardIdMappings.forEach { (localId, fsId) ->
                            cardDao.updateFirestoreId(localId, fsId)
                        }
                        batchEntryIdMappings.forEach { (localId, fsId) ->
                            weightEntryDao.updateFirestoreId(localId, fsId)
                        }
                        batchTxIdMappings.forEach { (localId, fsId) ->
                            transactionDao.updateFirestoreId(localId, fsId)
                        }
                        syncedCount += batchCards.size
                    } else {
                        errorCount += batchCards.size
                        AnalyticsHelper.logNonFatal(batchResult.exceptionOrNull() ?: Exception("Batch failed"), tag = "sync_batch")
                    }
                }
            }

            // Sau khi push xong, pull các thay đổi từ máy khác về.
            pullAllForCurrentUser()

            if (errorCount > 0 && syncedCount == 0) {
                _syncStatus.value = SyncStatus.Error("Không thể đồng bộ dữ liệu. Vui lòng thử lại.")
                AnalyticsHelper.syncFailed(stage = "push_all", errorClass = "AllCardsFailed")
                Result.failure(Exception("Sync failed for all cards"))
            } else {
                _syncStatus.value = SyncStatus.Success
                _lastSyncTime.value = System.currentTimeMillis()
                AnalyticsHelper.syncSuccess(
                    durationMs = System.currentTimeMillis() - startedAt,
                    cardCount = syncedCount
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Lỗi đồng bộ")
            AnalyticsHelper.syncFailed(stage = "exception", errorClass = e.javaClass.simpleName)
            AnalyticsHelper.logNonFatal(e, tag = "sync_all")
            Result.failure(e)
        }
    }

    suspend fun syncCardAndDetails(cardId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            return@withContext Result.failure(Exception("No user signed in"))
        }
        if (!canSync()) {
            return@withContext Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection"))
        }

        return@withContext try {
            val uid = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("No user signed in"))
            val card = cardDao.getCardById(cardId, uid) ?: return@withContext Result.failure(Exception("Card not found"))
            
            val fsCardId = if (card.firestoreId.isNullOrEmpty()) {
                firestoreRepository.generateCardId()
            } else {
                card.firestoreId
            }
            val isNewCard = card.firestoreId.isNullOrEmpty()

            val weightEntries = weightEntryDao.getWeightEntriesByCardIdSync(card.id)
            val transactions = transactionDao.getTransactionsByCardId(card.id).first()

            val batchCards = listOf(card.toFirestoreCard().copy(id = fsCardId))
            
            val batchEntries = weightEntries.map { entry ->
                val fsEntryId = if (entry.firestoreId.isNullOrEmpty()) {
                    firestoreRepository.generateWeightEntryId()
                } else {
                    entry.firestoreId
                }
                entry.toFirestoreWeightEntry(fsCardId).copy(id = fsEntryId)
            }

            val batchTxs = transactions.map { tx ->
                val fsTxId = if (tx.firestoreId.isNullOrEmpty()) {
                    firestoreRepository.generateTransactionId()
                } else {
                    tx.firestoreId
                }
                tx.toFirestoreTransaction(fsCardId).copy(id = fsTxId)
            }

            val result = firestoreRepository.executeBatchSync(
                cards = batchCards,
                weightEntries = batchEntries,
                transactions = batchTxs,
                newCardsCount = if (isNewCard) 1 else 0
            )

            result.onSuccess {
                cardDao.updateFirestoreId(card.id, fsCardId)
                weightEntries.forEachIndexed { idx, entry ->
                    val fsEntryId = batchEntries[idx].id
                    weightEntryDao.updateFirestoreId(entry.id, fsEntryId)
                }
                transactions.forEachIndexed { idx, tx ->
                    val fsTxId = batchTxs[idx].id
                    transactionDao.updateFirestoreId(tx.id, fsTxId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AnalyticsHelper.logNonFatal(e, tag = "sync_card_details")
            Result.failure(e)
        }
    }

    suspend fun syncCard(card: Card): Result<String> {
        if (auth.currentUser == null) {
            return Result.failure(Exception("No user signed in"))
        }
        if (!canSync()) {
            return Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection"))
        }

        return try {
            // Reuse firestoreId nếu đã từng push để Firestore update đúng doc cũ.
            val firestoreCard = card.toFirestoreCard().copy(id = card.firestoreId.orEmpty())
            val result = firestoreRepository.syncCard(firestoreCard)
            result.onSuccess { firestoreId ->
                // Stamp lại firestoreId vào Room để lần push sau update đúng doc,
                // và để pull dedup được khi user login máy khác.
                if (card.firestoreId.isNullOrEmpty()) {
                    firestoreRepository.incrementCardCount()
                }
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
        if (!canSync()) {
            return Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection"))
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
        if (!canSync()) {
            return Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection"))
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
    suspend fun pullAllForCurrentUser(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext Result.failure(
            Exception("No user signed in")
        )
        if (!canSync()) return@withContext Result.failure(Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection"))

        return@withContext try {
            val cardsResult = firestoreRepository.getAllCards()
            val firestoreCards = cardsResult.getOrElse {
                return@withContext Result.failure(it)
            }

            // Map firestoreId → local Room id để pull entries/transactions sau.
            val firestoreToLocalId = mutableMapOf<String, Long>()

            firestoreCards.forEach { fsCard ->
                // Skip nếu user đã xoá phiếu này (có tombstone). Tránh bug
                // phiếu xoá rồi quay về sau pull. Background sẽ retry delete cloud.
                if (fsCard.id.isNotBlank() &&
                    cardRepository.isCardTombstoned(currentUid, fsCard.id)) {
                    return@forEach
                }
                val byFsId = if (fsCard.id.isNotBlank()) cardDao.getByFirestoreId(fsCard.id) else null
                val action = PullDedupResolver.resolve(fsCard, byFsId) {
                    cardDao.findOrphanFirestoreMatch(
                        uid = currentUid,
                        date = fsCard.date.time,
                        name = fsCard.name,
                        totalWeight = fsCard.totalWeight
                    )
                }
                val localId = when (action) {
                    is PullDedupResolver.Action.Insert ->
                        cardDao.insertCard(action.fsCard.toLocalCard(currentUid, existingId = 0L))

                    is PullDedupResolver.Action.Update -> {
                        cardDao.updateCard(
                            action.fsCard.toLocalCard(currentUid, existingId = action.localId)
                        )
                        if (action.reason == PullDedupResolver.Reason.COMPOSITE_KEY_MATCH) {
                            AnalyticsHelper.breadcrumb("pull_dedup_composite cardId=${action.localId}")
                        }
                        action.localId
                    }

                    is PullDedupResolver.Action.Skip -> action.localId
                }
                if (fsCard.id.isNotBlank() && localId > 0) {
                    firestoreToLocalId[fsCard.id] = localId
                }
            }

            // Fetch all weight entries and transactions in bulk to avoid N+1 queries
            val allWeightEntries = firestoreRepository.getAllWeightEntries().getOrDefault(emptyList())
            val allTransactions = firestoreRepository.getAllTransactions().getOrDefault(emptyList())

            val weightEntriesByCard = allWeightEntries.groupBy { it.cardId }
            val transactionsByCard = allTransactions.groupBy { it.cardId }

            // Retry pending cloud deletes — tombstone từ delete khi offline.
            cardRepository.getPendingCloudDeletes(currentUid).forEach { tomb ->
                val fsId = tomb.firestoreId ?: return@forEach
                val ok = runCatching {
                    weightEntriesByCard[fsId].orEmpty()
                        .forEach { e -> if (e.id.isNotBlank()) firestoreRepository.deleteWeightEntry(e.id) }
                    transactionsByCard[fsId].orEmpty()
                        .forEach { t -> if (t.id.isNotBlank()) firestoreRepository.deleteTransaction(t.id) }
                    firestoreRepository.deleteCard(fsId).getOrNull()
                }.isSuccess
                if (ok) cardRepository.markTombstoneCloudDeleted(tomb.id)
            }

            // Pull weight entries + transactions cho từng card đã pull.
            firestoreToLocalId.forEach { (fsCardId, localCardId) ->
                val entries = weightEntriesByCard[fsCardId].orEmpty()
                    .sortedByDescending { it.timestamp }
                entries.forEach { fsEntry ->
                    if (fsEntry.id.isBlank()) return@forEach
                    val existing = weightEntryDao.getByFirestoreId(fsEntry.id)
                    if (existing == null) {
                        weightEntryDao.insertWeightEntry(fsEntry.toLocalEntry(localCardId))
                    }
                }

                val transactions = transactionsByCard[fsCardId].orEmpty()
                    .sortedByDescending { it.date }
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
            isPaid = this.isPaid,
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
            localId = this.id,
            impurityIsPercent = this.impurityIsPercent,
            bagMethodIsSampling = this.bagMethodIsSampling,
            bagSampleCount = this.bagSampleCount,
            bagSampleTotalWeight = this.bagSampleTotalWeight,
            weightInputMode = this.weightInputMode
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
            isPaid = this.isPaid,
            riceVariety = this.riceVariety,
            moisturePercent = this.moisturePercent,
            seasonLabel = this.seasonLabel,
            qrToken = this.qrToken,
            lockedByTraderId = this.lockedByTraderId,
            latitude = this.latitude,
            longitude = this.longitude,
            traderPhone = this.traderPhone,
            fieldAddress = this.fieldAddress,
            impurityIsPercent = this.impurityIsPercent,
            bagMethodIsSampling = this.bagMethodIsSampling,
            bagSampleCount = this.bagSampleCount,
            bagSampleTotalWeight = this.bagSampleTotalWeight,
            weightInputMode = this.weightInputMode
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

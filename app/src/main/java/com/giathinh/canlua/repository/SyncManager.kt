package com.giathinh.canlua.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.firestore.FirestoreCard
import com.giathinh.canlua.data.firestore.FirestoreTransaction
import com.giathinh.canlua.data.firestore.FirestoreWeightEntry
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.util.AnalyticsHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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

    private val _hasPendingSyncData = MutableStateFlow(false)
    val hasPendingSyncData: StateFlow<Boolean> = _hasPendingSyncData.asStateFlow()

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null && auth.currentUser?.isAnonymous == false

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
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
    }

    fun canSync(): Boolean {
        return if (settingsRepository.isSyncOnlyWifi()) {
            isWifiConnected()
        } else {
            isOnline()
        }
    }

    suspend fun refreshPendingSyncState(): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
        val hasPending = uid != null && (
                cardDao.countUnsyncedCards(uid) > 0 ||
                        weightEntryDao.countUnsyncedWeightEntries() > 0 ||
                        transactionDao.countUnsyncedTransactions() > 0 ||
                        cardRepository.getPendingCloudDeletes(uid).isNotEmpty()
                )
        _hasPendingSyncData.value = hasPending
        hasPending
    }

    /**
     * Enqueue 1 lần SyncWorker chạy khi thoả điều kiện mạng (Wi-Fi hoặc Di động tuỳ cài đặt).
     * Dùng `ExistingWorkPolicy.REPLACE` khi đổi cấu hình mạng hoặc `KEEP` khi ghi dữ liệu liên tục.
     */
    fun scheduleImmediateSync(replaceExisting: Boolean = false) {
        if (!settingsRepository.isAutoSyncEnabled() || !isUserSignedIn) return

        val requiredNetwork = if (settingsRepository.isSyncOnlyWifi()) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(requiredNetwork)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            policy,
            request
        )
    }

    fun cancelScheduledSync() {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("sync-worker")
        }
    }

    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            _syncStatus.value = SyncStatus.Error("Yêu cầu đăng nhập để đồng bộ")
            return@withContext Result.failure(Exception("No user signed in"))
        }
        if (!canSync()) {
            val errorMsg = if (settingsRepository.isSyncOnlyWifi()) {
                "Yêu cầu kết nối Wi-Fi để đồng bộ"
            } else {
                "Yêu cầu kết nối mạng để đồng bộ"
            }
            _syncStatus.value = SyncStatus.Error(errorMsg)
            return@withContext Result.failure(
                Exception(if (settingsRepository.isSyncOnlyWifi()) "Not connected to Wi-Fi" else "Not connected to network")
            )
        }

        return@withContext try {
            _syncStatus.value = SyncStatus.Syncing
            val startedAt = System.currentTimeMillis()

            val cloudCardsResult = firestoreRepository.getAllCards()
            val cloudCards = cloudCardsResult.getOrDefault(emptyList())
            val cloudCardMap = cloudCards.associateBy({ it.id }, { it.lastModifiedMs })

            val localCards = cardRepository.getAllCards().first()

            val dirtyCards = localCards.filter { card ->
                card.firestoreId.isNullOrEmpty() ||
                        !cloudCardMap.containsKey(card.firestoreId) ||
                        card.lastModifiedMs > (cloudCardMap[card.firestoreId] ?: 0L)
            }

            var syncedCount = 0
            var errorCount = 0

            if (dirtyCards.isNotEmpty()) {
                var currentOpCount = 0
                val batchCards = mutableListOf<FirestoreCard>()
                val batchEntries = mutableListOf<FirestoreWeightEntry>()
                val batchTxs = mutableListOf<FirestoreTransaction>()
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
                            AnalyticsHelper.logNonFatal(
                                batchResult.exceptionOrNull() ?: Exception("Batch failed"),
                                tag = "sync_batch"
                            )
                        }
                        currentOpCount = 0
                        batchCards.clear()
                        batchEntries.clear()
                        batchTxs.clear()
                        batchNewCardsCount = 0
                        batchCardIdMappings.clear()
                        batchEntryIdMappings.clear()
                        batchTxIdMappings.clear()
                    }

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
                        AnalyticsHelper.logNonFatal(
                            batchResult.exceptionOrNull() ?: Exception("Batch failed"),
                            tag = "sync_batch"
                        )
                    }
                }
            }

            pullAllForCurrentUser()

            if (errorCount > 0 && syncedCount == 0) {
                _syncStatus.value = SyncStatus.Error("Không thể đồng bộ dữ liệu. Vui lòng thử lại.")
                AnalyticsHelper.syncFailed(stage = "push_all", errorClass = "AllCardsFailed")
                Result.failure(Exception("Sync failed for all cards"))
            } else {
                _syncStatus.value = SyncStatus.Success
                _lastSyncTime.value = System.currentTimeMillis()
                refreshPendingSyncState()
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

    suspend fun pullAllForCurrentUser(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext Result.failure(
            Exception("No user signed in")
        )
        if (!canSync()) {
            return@withContext Result.failure(
                Exception(if (settingsRepository.isSyncOnlyWifi()) "No Wi-Fi connection" else "No network connection")
            )
        }

        return@withContext try {
            val cardsResult = firestoreRepository.getAllCards()
            val firestoreCards = cardsResult.getOrElse {
                return@withContext Result.failure(it)
            }

            val firestoreToLocalId = mutableMapOf<String, Long>()

            firestoreCards.forEach { fsCard ->
                if (fsCard.id.isNotBlank() &&
                    cardRepository.isCardTombstoned(currentUid, fsCard.id)
                ) {
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

            val allWeightEntries = firestoreRepository.getAllWeightEntries().getOrDefault(emptyList())
            val allTransactions = firestoreRepository.getAllTransactions().getOrDefault(emptyList())

            val weightEntriesByCard = allWeightEntries.groupBy { it.cardId }
            val transactionsByCard = allTransactions.groupBy { it.cardId }

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
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers — Local ⇄ Firestore
    // ─────────────────────────────────────────────────────────────────────────

    private fun Card.toFirestoreCard(): FirestoreCard {
        return FirestoreCard(
            id = "",
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

    companion object {
        private const val IMMEDIATE_SYNC_WORK_NAME = "sync-immediate"
    }
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
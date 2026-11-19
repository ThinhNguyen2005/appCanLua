package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.CardDao
import com.giathinh.canlua.data.dao.TransactionDao
import com.giathinh.canlua.data.dao.WeightEntryDao
import com.giathinh.canlua.data.database.AppDatabase
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.SeasonStats
import com.giathinh.canlua.data.model.SeasonStatsRaw
import com.giathinh.canlua.data.model.TraderHistoryItem
import com.giathinh.canlua.data.model.TraderStat
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.VarietyStat
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.data.model.serialize
import com.giathinh.canlua.data.model.deserializeCard
import com.giathinh.canlua.domain.CardError
import com.giathinh.canlua.domain.CardMutationResult
import com.giathinh.canlua.util.RiceSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import com.giathinh.canlua.util.CccdCrypto
import kotlin.math.roundToLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository cho phiếu cân.
 *
 * **Per-user isolation (v12+)**: mọi truy vấn DAO đều forward Firebase UID
 * của user đang sign-in qua [AuthManager.currentUser]. Khi chưa sign-in,
 * trả Flow rỗng / null thay vì throw — tránh crash trong khoảng thời gian
 * AuthState đang chuyển trạng thái (cold start, sign-out → sign-in).
 *
 * Mọi `insert*` tự gán `ownerUid` cho card mới. UI không cần biết về uid.
 */
@Singleton
class CardRepository @Inject constructor(
    private val db: AppDatabase,
    private val cardDao: CardDao,
    private val weightEntryDao: WeightEntryDao,
    private val transactionDao: TransactionDao,
    private val deletedCardDao: com.giathinh.canlua.data.dao.DeletedCardDao,
    private val authManager: AuthManager,
    private val syncStateDao: com.giathinh.canlua.data.dao.CardSyncStateDao = db.cardSyncStateDao(),
    private val receiptDao: com.giathinh.canlua.data.dao.MutationReceiptDao = db.mutationReceiptDao(),
    private val outboxDao: com.giathinh.canlua.data.dao.SyncOutboxDao = db.syncOutboxDao()
) {
    /** Empty string khi chưa sign-in → DAO query trả empty (không match row nào). */
    private fun uid(): String = authManager.currentUser?.uid.orEmpty()

    private fun newSyncId(): String = java.util.UUID.randomUUID().toString()

    /** Đảm bảo tồn tại hàng trạng thái sync cho phiếu (idempotent). */
    private suspend fun ensureSyncState(syncId: String, ownerUid: String, revision: Long, state: String) {
        val existing = syncStateDao.getBySyncId(syncId)
        if (existing == null) {
            syncStateDao.insert(
                com.giathinh.canlua.data.model.CardSyncState(
                    syncId = syncId, ownerUid = ownerUid, localRevision = revision, protocolState = state
                )
            )
        } else {
            syncStateDao.update(existing.copy(localRevision = revision, protocolState = state))
        }
    }

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards(uid()).map { list ->
        list.map { it.copy(cccd = CccdCrypto.decrypt(it.cccd, it.ownerUid)) }
    }

    suspend fun getCardsWithDetails(uid: String = uid()): List<com.giathinh.canlua.data.model.CardWithDetails> {
        val details = cardDao.getCardsWithDetails(uid)
        return details.map { d ->
            d.copy(card = d.card.copy(cccd = CccdCrypto.decrypt(d.card.cccd, d.card.ownerUid)))
        }
    }

    suspend fun importBackupBatch(
        ownerUid: String,
        wrappers: List<com.giathinh.canlua.util.BackupManager.CardBackupWrapper>
    ): Int = db.withTransaction {
        var count = 0
        for (wrapper in wrappers) {
            // T06/T07: bản sao import mang định danh cloud MỚI (firestoreId=null,
            // syncId mới) để không ghi đè phiếu gốc trên cloud; mã hóa CCCD tại chỗ.
            val cardSyncId = newSyncId()
            val cardCopy = wrapper.card.copy(
                id = 0,
                ownerUid = ownerUid,
                firestoreId = null,
                syncId = cardSyncId,
                revision = 1L,
                cccd = CccdCrypto.encrypt(wrapper.card.cccd, ownerUid)
            )
            val newCardId = cardDao.insertCard(cardCopy)
            ensureSyncState(cardSyncId, ownerUid, 1L, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
            for ((idx, entry) in wrapper.entries.withIndex()) {
                val entryCopy = entry.copy(id = 0, cardId = newCardId, firestoreId = null, syncId = newSyncId(), ordinal = idx)
                weightEntryDao.insertWeightEntry(entryCopy)
            }
            for (tx in wrapper.transactions) {
                val txCopy = tx.copy(id = 0, cardId = newCardId, firestoreId = null, syncId = newSyncId())
                transactionDao.insertTransaction(txCopy)
            }
            updateCardCalculations(newCardId)
            count++
        }
        count
    }

    fun getCardByIdFlow(id: Long): Flow<Card?> = cardDao.getCardByIdFlow(id, uid()).map { card ->
        card?.copy(cccd = CccdCrypto.decrypt(card.cccd, card.ownerUid))
    }

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id, uid())?.let {
        it.copy(cccd = CccdCrypto.decrypt(it.cccd, it.ownerUid))
    }

    /**
     * Insert card mới. Tự stamp `ownerUid` từ session hiện tại + `lastModifiedMs`
     * từ system clock trước khi ghi DB. UI/ViewModel chỉ chuyển business data.
     */
    suspend fun insertCard(card: Card): Long {
        val now = System.currentTimeMillis()
        val owner = card.ownerUid.ifBlank { uid() }
        val syncId = card.syncId.ifBlank { newSyncId() }
        val owned = card.copy(
            ownerUid = owner,
            syncId = syncId,
            revision = if (card.revision <= 0L) 1L else card.revision,
            lastModifiedMs = if (card.lastModifiedMs == 0L) now else card.lastModifiedMs,
            cccd = CccdCrypto.encrypt(card.cccd, owner)
        )
        return db.withTransaction {
            val id = cardDao.insertCard(owned)
            ensureSyncState(syncId, owner, owned.revision, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
            id
        }
    }

    /**
     * Update card FULL snapshot — chỉ còn dùng nội bộ cho sync/legacy. Fail-closed:
     * không tìm thấy phiếu thuộc owner hiện tại → không ghi (trước đây ghi theo PK
     * kể cả khi lookup null). Chặn phiếu đã khóa; KHÔNG cho mở khóa qua đường này
     * (mở khóa phải dùng [setCardLocked]). Đọc–kiểm–ghi trong một transaction và
     * tăng revision để chống mất cập nhật.
     */
    suspend fun updateCard(card: Card) {
        db.withTransaction {
            val u = uid()
            val existing = cardDao.getCardById(card.id, u) ?: return@withTransaction
            if (existing.isLocked) return@withTransaction
            val now = System.currentTimeMillis()
            val syncId = existing.syncId.ifBlank { newSyncId() }
            cardDao.updateCard(
                card.copy(
                    ownerUid = existing.ownerUid,
                    syncId = syncId,
                    revision = existing.revision + 1,
                    isLocked = existing.isLocked,
                    lastModifiedMs = now,
                    cccd = CccdCrypto.encrypt(card.cccd, existing.ownerUid)
                )
            )
            ensureSyncState(syncId, existing.ownerUid, existing.revision + 1, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
        }
    }

    // =========================================================================
    // T04 — Atomic command API (CAS revision + idempotency + typed errors)
    // =========================================================================

    /**
     * Helper mutation trung tâm: đọc–kiểm tra–ghi trong MỘT transaction.
     * - Fail-closed khi không tìm thấy phiếu thuộc owner ([CardError.NotFound]).
     * - Chặn phiếu khóa trừ khi [allowLockChange] ( lệnh mở/khóa riêng).
     * - Từ chối snapshot cũ khi [expectedRevision] lệch ([CardError.StaleRevision]).
     * - Tăng revision, đóng dấu sync DIRTY, recalc tùy chọn.
     * Ordinary mutation không được đổi trạng thái khóa (giữ [Card.isLocked] cũ).
     */
    private suspend fun mutateCard(
        cardId: Long,
        expectedRevision: Long? = null,
        allowLockChange: Boolean = false,
        recalculate: Boolean = true,
        transform: (Card) -> Card
    ): CardMutationResult = db.withTransaction {
        val u = uid()
        if (u.isBlank()) return@withTransaction CardMutationResult.Failure(CardError.SessionExpired)
        val existing = cardDao.getCardById(cardId, u)
            ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        if (expectedRevision != null && existing.revision != expectedRevision) {
            return@withTransaction CardMutationResult.Failure(CardError.StaleRevision)
        }
        if (existing.isLocked && !allowLockChange) {
            return@withTransaction CardMutationResult.Failure(CardError.Locked)
        }
        val transformed = transform(existing)
        val now = System.currentTimeMillis()
        val syncId = existing.syncId.ifBlank { newSyncId() }
        val newRevision = existing.revision + 1
        cardDao.updateCard(
            transformed.copy(
                id = existing.id,
                ownerUid = existing.ownerUid,
                syncId = syncId,
                revision = newRevision,
                isLocked = if (allowLockChange) transformed.isLocked else existing.isLocked,
                lastModifiedMs = now,
                cccd = CccdCrypto.encrypt(transformed.cccd, existing.ownerUid)
            )
        )
        ensureSyncState(syncId, existing.ownerUid, newRevision, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
        if (recalculate) updateCardCalculations(cardId)
        val final = cardDao.getCardById(cardId, u) ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        CardMutationResult.Success(final.copy(cccd = CccdCrypto.decrypt(final.cccd, final.ownerUid)))
    }

    /** Mở/khóa phiếu tường minh — không ghi đè field nghiệp vụ từ snapshot UI. */
    suspend fun setCardLocked(cardId: Long, expectedRevision: Long?, locked: Boolean): CardMutationResult =
        mutateCard(cardId, expectedRevision, allowLockChange = true, recalculate = false) {
            it.copy(isLocked = locked)
        }

    /** Sửa field nghiệp vụ (không đổi khóa). Recalc mặc định để đồng bộ tổng. */
    suspend fun patchCard(
        cardId: Long,
        expectedRevision: Long? = null,
        recalculate: Boolean = true,
        transform: (Card) -> Card
    ): CardMutationResult = mutateCard(cardId, expectedRevision, recalculate = recalculate, transform = transform)

    /**
     * Thêm thanh toán ATOMIC + idempotent. Cùng [operationId] gửi lại trả kết quả
     * cũ (không nhân đôi); cùng id khác payload → [CardError.Conflict]. Ghi ledger
     * + recalc + bump revision + receipt trong một transaction.
     */
    suspend fun addPayment(
        cardId: Long,
        amount: Double,
        description: String?,
        operationId: String
    ): CardMutationResult = db.withTransaction {
        val u = uid()
        if (u.isBlank()) return@withTransaction CardMutationResult.Failure(CardError.SessionExpired)
        val hash = "pay|$cardId|$amount|${description.orEmpty()}"
        val receipt = receiptDao.get(u, operationId)
        if (receipt != null) {
            if (receipt.commandHash != hash) return@withTransaction CardMutationResult.Failure(CardError.Conflict)
            val card = cardDao.getCardById(cardId, u)
                ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
            return@withTransaction CardMutationResult.Success(card.copy(cccd = CccdCrypto.decrypt(card.cccd, card.ownerUid)))
        }
        val existing = cardDao.getCardById(cardId, u)
            ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        if (existing.isLocked) return@withTransaction CardMutationResult.Failure(CardError.Locked)
        if (!com.giathinh.canlua.domain.WeighValidation.isValidPositivePayment(amount)) {
            return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Số tiền thanh toán phải dương"))
        }
        transactionDao.insertTransaction(
            com.giathinh.canlua.data.model.Transaction(
                cardId = cardId,
                amount = amount,
                type = com.giathinh.canlua.data.model.TransactionType.PAYMENT,
                description = description ?: "Thanh toán thêm",
                syncId = newSyncId(),
                operationId = operationId
            )
        )
        updateCardCalculations(cardId)
        val newRevision = existing.revision + 1
        cardDao.bumpRevision(cardId, u, newRevision, System.currentTimeMillis())
        ensureSyncState(existing.syncId, u, newRevision, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
        receiptDao.insert(
            com.giathinh.canlua.data.model.MutationReceipt(
                ownerUid = u, operationId = operationId, commandHash = hash, createdCardId = cardId
            )
        )
        val final = cardDao.getCardById(cardId, u) ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        CardMutationResult.Success(final.copy(cccd = CccdCrypto.decrypt(final.cccd, final.ownerUid)))
    }

    /**
     * Tạo phiếu + cọc ATOMIC + idempotent. Location lookup làm ở NGOÀI (ViewModel)
     * trước khi gọi — không giữ DB lock trong lúc chờ GPS/network.
     */
    suspend fun createCardWithDeposit(
        card: Card,
        depositAmount: Double,
        operationId: String
    ): CardMutationResult = db.withTransaction {
        val u = uid()
        if (u.isBlank()) return@withTransaction CardMutationResult.Failure(CardError.SessionExpired)
        val hash = "create|${card.name}|$depositAmount|${card.pricePerKg}"
        val receipt = receiptDao.get(u, operationId)
        if (receipt != null) {
            if (receipt.commandHash != hash) return@withTransaction CardMutationResult.Failure(CardError.Conflict)
            val id = receipt.createdCardId
            val cardExisting = if (id != null) cardDao.getCardById(id, u) else null
            return@withTransaction if (cardExisting != null) {
                CardMutationResult.Success(cardExisting.copy(cccd = CccdCrypto.decrypt(cardExisting.cccd, cardExisting.ownerUid)))
            } else {
                CardMutationResult.Failure(CardError.NotFound)
            }
        }
        if (card.name.isBlank()) return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Tên phiếu trống"))
        if (!com.giathinh.canlua.domain.WeighValidation.isValidNonNegativeMoney(depositAmount)) {
            return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Tiền cọc không hợp lệ"))
        }
        val now = System.currentTimeMillis()
        val syncId = newSyncId()
        val newId = cardDao.insertCard(
            card.copy(
                id = 0L,
                ownerUid = u,
                syncId = syncId,
                revision = 1L,
                date = java.util.Date(),
                lastModifiedMs = now,
                cccd = CccdCrypto.encrypt(card.cccd, u)
            )
        )
        ensureSyncState(syncId, u, 1L, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
        if (depositAmount > 0.0) {
            transactionDao.insertTransaction(
                com.giathinh.canlua.data.model.Transaction(
                    cardId = newId,
                    amount = depositAmount,
                    type = com.giathinh.canlua.data.model.TransactionType.DEPOSIT,
                    description = "Tiền cọc",
                    syncId = newSyncId(),
                    operationId = operationId
                )
            )
        }
        updateCardCalculations(newId)
        receiptDao.insert(
            com.giathinh.canlua.data.model.MutationReceipt(
                ownerUid = u, operationId = operationId, commandHash = hash, createdCardId = newId
            )
        )
        val final = cardDao.getCardById(newId, u) ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        CardMutationResult.Success(final.copy(cccd = CccdCrypto.decrypt(final.cccd, final.ownerUid)))
    }

    /**
     * Xoá card local + ghi tombstone trước khi xoá để chống bug "phiếu phục sinh"
     * sau pull. Tombstone lưu snapshot full card data (JSON) để user khôi phục.
     *
     * Caller (SyncableCardRepository) sẽ chịu trách nhiệm xoá Firestore — nếu
     * fail (offline), tombstone giữ flag `cloudDeleted = false` để background
     * worker retry.
     */
    /**
     * Xoá card local + ghi tombstone — bọc trong transaction để đảm bảo:
     * nếu app crash sau khi ghi tombstone nhưng trước khi xoá card, DB vẫn nhất quán.
     *
     * ⚠️ Tombstone chỉ chứa Card header (serialize không bao gồm weight_entries /
     * transactions). Phiếu khôi phục từ tombstone sẽ không có dòng cân.
     * Ghi rõ điều này để user biết.
     */
    suspend fun deleteCard(card: Card) {
        val ownerUid = card.ownerUid.ifBlank { uid() }
        db.withTransaction {
            // 1. Ghi tombstone trước — pull sync sẽ skip phiếu này dù cloud vẫn có doc.
            if (ownerUid.isNotBlank()) {
                deletedCardDao.insert(
                    com.giathinh.canlua.data.model.DeletedCard(
                        ownerUid    = ownerUid,
                        firestoreId = card.firestoreId,
                        localId     = card.id,
                        cardJson    = card.serialize(),
                        name        = card.name,
                        traderName  = card.traderName,
                        totalWeight = card.totalWeight,
                        totalAmount = card.totalAmount,
                        cardDate    = card.date.time,
                        seasonLabel = card.seasonLabel,
                        riceVariety = card.riceVariety,
                        // Nếu card chưa từng sync (firestoreId null), không cần xoá cloud.
                        cloudDeleted = card.firestoreId == null
                    )
                )
            }
            // 2. Xoá Room — FK CASCADE tự xoá weight_entries + transactions con.
            cardDao.deleteCard(card)
        }
    }

    /**
     * Khôi phục phiếu từ tombstone — recreate card với lastModifiedMs = now
     * để force push lên cloud (cloud có thể đã xoá do delete sync trước đó).
     * Sau khôi phục thành công, purge tombstone.
     */
    suspend fun restoreFromTombstone(tombstoneId: Long): Long? {
        val tombstone = deletedCardDao.getById(tombstoneId) ?: return null
        val card = deserializeCard(tombstone.cardJson) ?: return null
        // Insert card mới — Room auto generate id mới (id cũ có thể đã collide).
        // Reset firestoreId + syncId mới để push tạo doc mới, không ghi đè bản gốc.
        val now = System.currentTimeMillis()
        val syncId = newSyncId()
        val newId = db.withTransaction {
            val id = cardDao.insertCard(
                card.copy(
                    id = 0L,
                    firestoreId = null,
                    syncId = syncId,
                    revision = 1L,
                    lastModifiedMs = now,
                    ownerUid = tombstone.ownerUid,
                    cccd = CccdCrypto.encrypt(card.cccd, tombstone.ownerUid)
                )
            )
            ensureSyncState(syncId, tombstone.ownerUid, 1L, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
            id
        }
        deletedCardDao.purge(tombstoneId)
        return newId
    }

    fun getDeletedCards(uid: String) = deletedCardDao.observe(uid)

    suspend fun isCardTombstoned(uid: String, firestoreId: String): Boolean =
        deletedCardDao.isTombstoned(uid, firestoreId)

    suspend fun getPendingCloudDeletes(uid: String) =
        deletedCardDao.getPendingCloudDeletes(uid)

    suspend fun markTombstoneCloudDeleted(id: Long) = deletedCardDao.markCloudDeleted(id)

    suspend fun purgeTombstone(id: Long) = deletedCardDao.purge(id)

    /**
     * Sửa phiếu từ edit dialog ATOMIC: cập nhật field nghiệp vụ + ghi ledger delta
     * (cọc/đã trả) để đạt target + recalc, tất cả trong MỘT transaction. Không tin
     * aggregate UI gửi lên — tổng luôn tính lại từ ledger. Chặn phiếu khóa.
     */
    suspend fun editCardFinancials(card: Card, operationId: String): CardMutationResult = db.withTransaction {
        val u = uid()
        if (u.isBlank()) return@withTransaction CardMutationResult.Failure(CardError.SessionExpired)
        val old = cardDao.getCardById(card.id, u)
            ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        if (old.isLocked) return@withTransaction CardMutationResult.Failure(CardError.Locked)
        if (!com.giathinh.canlua.domain.WeighValidation.isValidMoisture(card.moisturePercent)) {
            return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Độ ẩm không hợp lệ"))
        }
        if (!com.giathinh.canlua.domain.WeighValidation.isValidImpurity(card.impurityWeight, card.impurityIsPercent)) {
            return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Tạp chất không hợp lệ"))
        }
        if (!com.giathinh.canlua.domain.WeighValidation.isValidNonNegativeMoney(card.pricePerKg)) {
            return@withTransaction CardMutationResult.Failure(CardError.InvalidInput("Đơn giá không hợp lệ"))
        }
        val now = System.currentTimeMillis()
        val newRevision = old.revision + 1
        val merged = old.copy(
            name = card.name,
            traderName = card.traderName,
            riceVariety = card.riceVariety,
            seasonLabel = card.seasonLabel,
            moisturePercent = card.moisturePercent,
            impurityWeight = card.impurityWeight,
            impurityIsPercent = card.impurityIsPercent,
            pricePerKg = card.pricePerKg,
            traderPhone = card.traderPhone,
            fieldAddress = card.fieldAddress,
            cccd = CccdCrypto.encrypt(card.cccd, u),
            isPaid = card.isPaid,
            revision = newRevision,
            lastModifiedMs = now
        )
        cardDao.updateCard(merged)
        val depositDiff = card.depositAmount - old.depositAmount
        if (depositDiff != 0.0) {
            transactionDao.insertTransaction(
                Transaction(cardId = card.id, amount = depositDiff, type = com.giathinh.canlua.data.model.TransactionType.DEPOSIT, description = "Cập nhật tiền cọc", syncId = newSyncId())
            )
        }
        val paidDiff = card.paidAmount - old.paidAmount
        if (paidDiff != 0.0) {
            transactionDao.insertTransaction(
                Transaction(cardId = card.id, amount = paidDiff, type = com.giathinh.canlua.data.model.TransactionType.PAYMENT, description = "Cập nhật đã trả", syncId = newSyncId())
            )
        }
        updateCardCalculations(card.id)
        ensureSyncState(old.syncId, u, newRevision, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
        val final = cardDao.getCardById(card.id, u) ?: return@withTransaction CardMutationResult.Failure(CardError.NotFound)
        CardMutationResult.Success(final.copy(cccd = CccdCrypto.decrypt(final.cccd, final.ownerUid)))
    }

    // Weight Entry operations — không cần filter uid vì FK CASCADE qua cardId,
    // và caller chỉ truy cập sau khi đã có cardId từ getAllCards (đã filter).
    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        weightEntryDao.getWeightEntriesByCardId(cardId)

    /**
     * Thêm dòng cân thô và tự động tính toán lại toàn bộ phiếu trong 1 transaction atomic.
     * Trả về số lượng dòng cân hợp lệ hiện tại (weight > 0) để UI/TTS xử lý,
     * hoặc null nếu card không tồn tại hoặc đã bị khoá.
     */
    suspend fun addWeightEntryAndRecalc(cardId: Long, weight: Double): Int? = db.withTransaction {
        val u = uid()
        val card = cardDao.getCardById(cardId, u) ?: return@withTransaction null
        if (card.isLocked) return@withTransaction null
        if (!com.giathinh.canlua.domain.WeighValidation.isValidWeight(weight)) return@withTransaction null
        val nextOrdinal = (weightEntryDao.getMaxOrdinal(cardId) ?: -1) + 1
        weightEntryDao.insertWeightEntry(
            WeightEntry(cardId = cardId, weight = weight, syncId = newSyncId(), ordinal = nextOrdinal)
        )
        updateCardCalculations(cardId)
        bumpCardRevision(card, u)
        weightEntryDao.getWeightEntriesByCardIdSync(cardId).count { it.weight > 0.0 }
    }

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long = db.withTransaction {
        val u = uid()
        val card = cardDao.getCardById(weightEntry.cardId, u) ?: return@withTransaction -1L
        if (card.isLocked) return@withTransaction -1L
        if (!com.giathinh.canlua.domain.WeighValidation.isValidWeight(weightEntry.weight)) return@withTransaction -1L
        val nextOrdinal = (weightEntryDao.getMaxOrdinal(weightEntry.cardId) ?: -1) + 1
        val id = weightEntryDao.insertWeightEntry(
            weightEntry.copy(
                syncId = weightEntry.syncId.ifBlank { newSyncId() },
                ordinal = if (weightEntry.ordinal > 0) weightEntry.ordinal else nextOrdinal
            )
        )
        updateCardCalculations(weightEntry.cardId)
        bumpCardRevision(card, u)
        id
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) = db.withTransaction {
        val u = uid()
        val card = cardDao.getCardById(weightEntry.cardId, u) ?: return@withTransaction
        if (card.isLocked) return@withTransaction
        if (!com.giathinh.canlua.domain.WeighValidation.isValidWeight(weightEntry.weight)) return@withTransaction
        // Giữ syncId/ordinal ổn định của bản ghi đang sửa (không tin giá trị UI gửi lên).
        val current = weightEntryDao.getByCardAndSyncId(weightEntry.cardId, weightEntry.syncId)
            ?: weightEntryDao.getWeightEntriesByCardIdSync(weightEntry.cardId).firstOrNull { it.id == weightEntry.id }
        val stable = weightEntry.copy(
            syncId = current?.syncId ?: weightEntry.syncId.ifBlank { newSyncId() },
            ordinal = current?.ordinal ?: weightEntry.ordinal
        )
        weightEntryDao.updateWeightEntry(stable)
        updateCardCalculations(weightEntry.cardId)
        bumpCardRevision(card, u)
    }

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) = db.withTransaction {
        val u = uid()
        val card = cardDao.getCardById(weightEntry.cardId, u) ?: return@withTransaction
        if (card.isLocked) return@withTransaction
        weightEntryDao.deleteWeightEntry(weightEntry)
        updateCardCalculations(weightEntry.cardId)
        bumpCardRevision(card, u)
    }

    /** Tăng revision + đóng dấu sync DIRTY sau mutation dòng cân/giao dịch. */
    private suspend fun bumpCardRevision(card: Card, u: String) {
        val newRevision = card.revision + 1
        cardDao.bumpRevision(card.id, u, newRevision, System.currentTimeMillis())
        ensureSyncState(card.syncId, u, newRevision, com.giathinh.canlua.data.model.CardSyncState.STATE_DIRTY)
    }

    /**
     * T06: Thay toàn bộ children của phiếu bằng snapshot cloud trong MỘT transaction.
     * Chỉ gọi khi cloud THẮNG (Insert/Update) — không ghép parent mới với children cũ.
     * Xóa children local vắng mặt trong snapshot → chống bao đã xóa hồi sinh / sửa không đồng bộ.
     */
    suspend fun replaceChildrenAtomic(
        cardId: Long,
        entries: List<WeightEntry>,
        transactions: List<Transaction>
    ) = db.withTransaction {
        weightEntryDao.deleteWeightEntriesByCardId(cardId)
        transactionDao.deleteTransactionsByCardId(cardId)
        entries.forEach { weightEntryDao.insertWeightEntry(it.copy(id = 0L, cardId = cardId)) }
        transactions.forEach { transactionDao.insertTransaction(it.copy(id = 0L, cardId = cardId)) }
    }

    // Transaction operations
    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCardId(cardId)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    // Calculation methods — uses RiceCalculator with moisture adjustment
    suspend fun calculateCardTotals(cardId: Long): CardCalculationResult {
        val totalRawWeight = weightEntryDao.getTotalRawWeightByCardId(cardId) ?: 0.0
        val totalNetWeight = weightEntryDao.getTotalNetWeightByCardId(cardId) ?: 0.0
        val bagCount = weightEntryDao.getBagCountByCardId(cardId)
        val totalPaid = transactionDao.getTotalPaidAmountByCardId(cardId) ?: 0.0
        val totalDeposit = transactionDao.getTotalDepositAmountByCardId(cardId) ?: 0.0

        return CardCalculationResult(
            totalRawWeight = totalRawWeight,
            totalNetWeight = totalNetWeight,
            bagCount = bagCount,
            totalPaid = totalPaid,
            totalDeposit = totalDeposit
        )
    }

    /**
     * Recalculate aggregate fields trên Card từ các WeightEntry + Transaction con.
     *
     * **Thay đổi v19:**
     * - Bọc toàn bộ trong `withTransaction` → loại bỏ lost-update race condition.
     * - Guard `isLocked` → phiếu đã chốt không bị recalc.
     * - Dùng [RiceSettlement.compute] làm nguồn tính duy nhất:
     *   - Σ dòng == tổng phiếu tuyệt đối (Largest Remainder Method).
     *   - Tạp chất chia theo tỷ lệ KL sau bì (không chia đều nữa).
     *   - Tiền làm tròn 1 lần duy nhất (Long VND → Double để ghi DB).
     *   - Tiền trả dư ghi ra `overpaidAmount` thay vì bị clamp về 0.
     */
    suspend fun updateCardCalculations(cardId: Long) = db.withTransaction {
        val card = cardDao.getCardById(cardId, uid()) ?: return@withTransaction

        // Phiếu đã chốt: không recalc để tránh đổi giá/tổng sau khi chốt.
        if (card.isLocked) return@withTransaction

        val entries = weightEntryDao.getWeightEntriesByCardIdSync(cardId)
        val paidVnd    = (transactionDao.getTotalPaidAmountByCardId(cardId)    ?: 0.0).roundToLong()
        val depositVnd = (transactionDao.getTotalDepositAmountByCardId(cardId) ?: 0.0).roundToLong()

        // ── Tính toán bằng RiceSettlement qua mapper chung (nguồn duy nhất) ──
        // Dùng CardSettlement để preview (ViewModel) và quyết toán (DB) không lệch.
        val s = RiceSettlement.compute(
            com.giathinh.canlua.domain.CardSettlement.toInput(card, entries, paidVnd, depositVnd)
        )

        // Kiểm tra bất biến — vi phạm = lỗi code, không phải lỗi user.
        val violations = s.verify()
        check(violations.isEmpty()) { "RiceSettlement invariant broken: $violations" }

        // ── Cập nhật từng dòng (1 batch write) ──────────────────────────────
        // rawUnits / bagUnits / impUnits / netUnits chỉ chứa dòng weight > 0,
        // theo đúng thứ tự lọc. Map lại vào entries gốc để giữ id / firestoreId.
        var validIdx = 0
        val updatedEntries = entries.map { entry ->
            if (entry.weight > 0.0) {
                val i = validIdx++
                entry.copy(
                    bagWeight      = s.kg(s.bagUnits[i]),
                    impurityWeight = s.kg(s.impurityUnits[i]),
                    netWeight      = s.kg(s.netUnits[i])
                )
            } else {
                entry.copy(bagWeight = 0.0, impurityWeight = 0.0, netWeight = 0.0)
            }
        }
        // Chỉ ghi những entry thực sự thay đổi
        val dirty = updatedEntries.filterIndexed { i, e ->
            val old = entries[i]
            e.bagWeight != old.bagWeight || e.impurityWeight != old.impurityWeight || e.netWeight != old.netWeight
        }
        if (dirty.isNotEmpty()) weightEntryDao.updateWeightEntries(dirty)

        // ── Cập nhật Card ────────────────────────────────────────────────────
        cardDao.updateCard(
            card.copy(
                totalWeight     = s.kg(s.totalRawUnits),
                netWeight       = s.totalNetKg,
                bagCount        = s.rawUnits.size,   // khớp số bao đã trừ bì
                paidAmount      = paidVnd.toDouble(),
                depositAmount   = depositVnd.toDouble(),
                totalAmount     = s.totalAmountVnd.toDouble(),
                remainingAmount = s.remainingVnd.toDouble(),
                overpaidAmount  = s.overpaidVnd.toDouble(),
                lastModifiedMs  = System.currentTimeMillis()
            )
        )
    }


    suspend fun lockCard(cardId: Long, traderId: String) = cardDao.lockCard(cardId, traderId)

    // === Phase 1: Filter ===

    fun getCardsByRiceVariety(variety: String): Flow<List<Card>> =
        cardDao.getCardsByRiceVariety(variety, uid()).map { list ->
            list.map { it.copy(cccd = CccdCrypto.decrypt(it.cccd)) }
        }

    fun getDistinctRiceVarieties(): Flow<List<String>> =
        cardDao.getDistinctRiceVarieties(uid())

    fun getSuggestedRiceVarieties(): Flow<List<String>> =
        cardDao.getSuggestedRiceVarieties(uid())

    // === Phase 3: Season Statistics Dashboard ===

    fun getDistinctSeasons(): Flow<List<String>> = cardDao.getDistinctSeasons(uid())

    /** Stats cho 1 vụ → map raw → domain. */
    fun getSeasonStats(season: String): Flow<SeasonStats> =
        cardDao.getSeasonStats(season, uid()).map { it.toDomain(season) }

    /** Stats tổng toàn bộ data — dùng khi chưa có vụ nào hoặc fallback. */
    fun getOverallStats(): Flow<SeasonStats> =
        cardDao.getOverallStats(uid()).map { it.toDomain(season = "Tất cả") }

    fun getVarietyBreakdown(season: String): Flow<List<VarietyStat>> =
        cardDao.getVarietyBreakdown(season, uid())

    fun getTopTraders(season: String): Flow<List<TraderStat>> =
        cardDao.getTopTraders(season, uid())

    fun getAllSeasonsComparison(): Flow<List<SeasonStats>> =
        cardDao.getAllSeasonsComparison(uid()).map { rows ->
            rows.map { row ->
                SeasonStatsRaw(
                    cardCount = row.cardCount,
                    totalNetWeight = row.totalNetWeight,
                    totalRevenue = row.totalRevenue,
                    totalPaid = row.totalPaid,
                    totalRemaining = row.totalRemaining,
                    avgPrice = row.avgPrice,
                    avgMoisture = row.avgMoisture,
                    totalBags = row.totalBags,
                    totalImpurity = row.totalImpurity,
                    wetCardCount = row.wetCardCount,
                    dryCardCount = row.dryCardCount
                ).toDomain(row.season).copy(lastDate = row.lastDate)
            }
        }

    /** Lịch sử thương lái đã mua — reactive Flow từ cards table. */
    fun getTraderHistory(): Flow<List<TraderHistoryItem>> = cardDao.getTraderHistory(uid())

    /**
     * One-shot migration: gán toàn bộ cards orphan (ownerUid='') cho user đang
     * đăng nhập. Gọi từ `CanLuaApplication` sau lần đầu sign-in sau update v12.
     * Idempotent — gọi nhiều lần cũng an toàn (không có row orphan thì noop).
     *
     * @return số rows đã claim. 0 = không cần làm gì.
     */
    suspend fun claimOrphanCardsForCurrentUser(): Int {
        val u = uid()
        if (u.isBlank()) return 0
        if (cardDao.countOrphanCards() == 0) return 0
        return cardDao.claimOrphanCards(u)
    }
    /**
     * Đếm số phiếu user hiện tại đã tạo trong ngày hôm nay (00:00 local timezone
     * → bây giờ). Phục vụ Premium gate "free user tối đa N phiếu/ngày".
     * Trả 0 nếu chưa sign-in (uid blank).
     */
    suspend fun countCardsCreatedToday(): Int {
        val u = uid()
        if (u.isBlank()) return 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cardDao.countCardsSince(u, cal.timeInMillis)
    }
}

data class CardCalculationResult(
    val totalRawWeight: Double,
    val totalNetWeight: Double,
    val bagCount: Int,
    val totalPaid: Double,
    val totalDeposit: Double
)

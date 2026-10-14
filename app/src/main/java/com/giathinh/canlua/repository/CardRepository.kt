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
    private val authManager: AuthManager
) {
    /** Empty string khi chưa sign-in → DAO query trả empty (không match row nào). */
    private fun uid(): String = authManager.currentUser?.uid.orEmpty()

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards(uid()).map { list ->
        list.map { it.copy(cccd = CccdCrypto.decrypt(it.cccd)) }
    }

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id, uid())?.let {
        it.copy(cccd = CccdCrypto.decrypt(it.cccd))
    }

    /**
     * Insert card mới. Tự stamp `ownerUid` từ session hiện tại + `lastModifiedMs`
     * từ system clock trước khi ghi DB. UI/ViewModel chỉ chuyển business data.
     */
    suspend fun insertCard(card: Card): Long {
        val now = System.currentTimeMillis()
        val owned = card.copy(
            ownerUid = card.ownerUid.ifBlank { uid() },
            lastModifiedMs = if (card.lastModifiedMs == 0L) now else card.lastModifiedMs,
            cccd = CccdCrypto.encrypt(card.cccd)
        )
        return cardDao.insertCard(owned)
    }

    /**
     * Update card. Tự stamp lại `lastModifiedMs` để conflict resolver biết
     * bản local mới hơn cloud (cloud sẽ chỉ overwrite local nếu cloud mới hơn).
     */
    suspend fun updateCard(card: Card) {
        val existing = cardDao.getCardById(card.id, uid())
        if (existing?.isLocked == true && card.isLocked) {
            // Phiếu đã chốt và không phải là thao tác mở khoá -> chặn chỉnh sửa
            return
        }
        val now = System.currentTimeMillis()
        cardDao.updateCard(card.copy(
            lastModifiedMs = now,
            cccd = CccdCrypto.encrypt(card.cccd)
        ))
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
        // Reset firestoreId để push tạo doc mới (doc cũ đã bị delete sync).
        val now = System.currentTimeMillis()
        val newId = cardDao.insertCard(
            card.copy(
                id = 0L,
                firestoreId = null,
                lastModifiedMs = now,
                ownerUid = tombstone.ownerUid
            )
        )
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
        val card = cardDao.getCardById(cardId, uid()) ?: return@withTransaction null
        if (card.isLocked) return@withTransaction null
        weightEntryDao.insertWeightEntry(WeightEntry(cardId = cardId, weight = weight))
        updateCardCalculations(cardId)
        weightEntryDao.getWeightEntriesByCardIdSync(cardId).count { it.weight > 0.0 }
    }

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long = db.withTransaction {
        val card = cardDao.getCardById(weightEntry.cardId, uid()) ?: return@withTransaction -1L
        if (card.isLocked) return@withTransaction -1L
        val id = weightEntryDao.insertWeightEntry(weightEntry)
        updateCardCalculations(weightEntry.cardId)
        id
    }

    suspend fun updateWeightEntry(weightEntry: WeightEntry) = db.withTransaction {
        val card = cardDao.getCardById(weightEntry.cardId, uid()) ?: return@withTransaction
        if (card.isLocked) return@withTransaction
        weightEntryDao.updateWeightEntry(weightEntry)
        updateCardCalculations(weightEntry.cardId)
    }

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) = db.withTransaction {
        val card = cardDao.getCardById(weightEntry.cardId, uid()) ?: return@withTransaction
        if (card.isLocked) return@withTransaction
        weightEntryDao.deleteWeightEntry(weightEntry)
        updateCardCalculations(weightEntry.cardId)
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

        // ── Tính toán bằng RiceSettlement (nguồn duy nhất) ──────────────────
        val s = RiceSettlement.compute(
            RiceSettlement.Input(
                entryWeightsKg         = entries.map { it.weight },
                bagWeightKg            = card.bagWeight,
                bagMethodIsSampling    = card.bagMethodIsSampling,
                bagSampleCount         = card.bagSampleCount,
                bagSampleTotalWeightKg = card.bagSampleTotalWeight,
                impurityValue          = card.impurityWeight,
                impurityIsPercent      = card.impurityIsPercent,
                moisturePercent        = card.moisturePercent,
                pricePerKg             = card.pricePerKg,
                paidVnd                = paidVnd,
                depositVnd             = depositVnd
            )
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

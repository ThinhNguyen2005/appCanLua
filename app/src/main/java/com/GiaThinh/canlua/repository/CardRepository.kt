package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.data.model.SeasonStatsRaw
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.VarietyStat
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.util.RiceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val cardDao: CardDao,
    private val weightEntryDao: WeightEntryDao,
    private val transactionDao: TransactionDao,
    private val authManager: AuthManager
) {
    /** Empty string khi chưa sign-in → DAO query trả empty (không match row nào). */
    private fun uid(): String = authManager.currentUser?.uid.orEmpty()

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards(uid())

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id, uid())

    /**
     * Insert card mới. Tự stamp `ownerUid` từ session hiện tại + `lastModifiedMs`
     * từ system clock trước khi ghi DB. UI/ViewModel chỉ chuyển business data.
     */
    suspend fun insertCard(card: Card): Long {
        val now = System.currentTimeMillis()
        val owned = card.copy(
            ownerUid = if (card.ownerUid.isBlank()) uid() else card.ownerUid,
            lastModifiedMs = if (card.lastModifiedMs == 0L) now else card.lastModifiedMs
        )
        return cardDao.insertCard(owned)
    }

    /**
     * Update card. Tự stamp lại `lastModifiedMs` để conflict resolver biết
     * bản local mới hơn cloud (cloud sẽ chỉ overwrite local nếu cloud mới hơn).
     */
    suspend fun updateCard(card: Card) {
        cardDao.updateCard(card.copy(lastModifiedMs = System.currentTimeMillis()))
    }

    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)

    // Weight Entry operations — không cần filter uid vì FK CASCADE qua cardId,
    // và caller chỉ truy cập sau khi đã có cardId từ getAllCards (đã filter).
    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>> =
        weightEntryDao.getWeightEntriesByCardId(cardId)

    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long =
        weightEntryDao.insertWeightEntry(weightEntry)

    suspend fun updateWeightEntry(weightEntry: WeightEntry) =
        weightEntryDao.updateWeightEntry(weightEntry)

    suspend fun deleteWeightEntry(weightEntry: WeightEntry) =
        weightEntryDao.deleteWeightEntry(weightEntry)

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
     * **Quan trọng:** trước đây hàm này tự tính netWeight bằng công thức tự chế
     * `totalRaw - totalRaw × moisture/100` (trừ thẳng % moisture, sai chuẩn ngành lúa).
     * Đã unify về `RiceCalculator.calcNetWeight` để khớp với công thức tính ở từng
     * `WeightEntry` và đúng chuẩn ngành (quy đổi về độ ẩm chuẩn 14%).
     */
    suspend fun updateCardCalculations(cardId: Long) {
        val card = cardDao.getCardById(cardId, uid()) ?: return
        val calculation = calculateCardTotals(cardId)

        val totalRaw = calculation.totalRawWeight
        // Tổng khối lượng bao bì = số bao × trọng lượng bao đơn vị
        val totalBagWeight = calculation.bagCount * card.bagWeight

        // Net weight chuẩn: (raw - bao - tạp) × (100 - moisture) / (100 - 14)
        val finalNetWeight = RiceCalculator.calcNetWeight(
            rawWeight = totalRaw,
            bagWeight = totalBagWeight,
            impurityWeight = card.impurityWeight,
            moisturePercent = card.moisturePercent
        ).coerceAtLeast(0.0)

        val totalAmount = RiceCalculator.calcTotalAmount(
            netWeight = finalNetWeight,
            pricePerKg = card.pricePerKg
        ).coerceAtLeast(0.0)

        // Còn lại không cho phép âm để tránh hiển thị "-100,000đ" vô nghĩa.
        // Nếu paid + deposit > total, UI nên flag "Đã thanh toán dư" thay vì show số âm.
        val remainingAmount = RiceCalculator.calcRemainingAmount(
            totalAmount = totalAmount,
            paidAmount = calculation.totalPaid,
            depositAmount = calculation.totalDeposit
        ).coerceAtLeast(0.0)

        val updatedCard = card.copy(
            totalWeight = totalRaw,
            netWeight = finalNetWeight,
            bagCount = calculation.bagCount,
            paidAmount = calculation.totalPaid,
            depositAmount = calculation.totalDeposit,
            totalAmount = totalAmount,
            remainingAmount = remainingAmount
        )

        cardDao.updateCard(updatedCard)
    }

    // === Phase 1: QR Handshake ===
    // QR query KHÔNG filter ownerUid: trader (uid khác farmer) phải tìm được
    // card qua qrToken để xác thực giao dịch chéo. Token đã đủ entropy + được
    // bảo vệ bởi Firestore rule.

    suspend fun findByQrToken(token: String): Card? = cardDao.findByQrToken(token)

    suspend fun updateQrToken(cardId: Long, token: String) = cardDao.updateQrToken(cardId, token)

    suspend fun lockCard(cardId: Long, traderId: String) = cardDao.lockCard(cardId, traderId)

    // === Phase 1: Filter ===

    fun getCardsByRiceVariety(variety: String): Flow<List<Card>> =
        cardDao.getCardsByRiceVariety(variety, uid())

    fun getDistinctRiceVarieties(): Flow<List<String>> =
        cardDao.getDistinctRiceVarieties(uid())

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

    /** So sánh giữa nhiều vụ — map từng row Raw → SeasonStats. */
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
                ).toDomain(row.season)
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

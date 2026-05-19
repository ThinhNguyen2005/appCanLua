package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.data.model.SeasonStatsRaw
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.VarietyStat
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.util.RiceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val weightEntryDao: WeightEntryDao,
    private val transactionDao: TransactionDao
) {
    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards()

    suspend fun getCardById(id: Long): Card? = cardDao.getCardById(id)

    suspend fun insertCard(card: Card): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: Card) = cardDao.updateCard(card)

    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)

    // Weight Entry operations
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
        val card = cardDao.getCardById(cardId) ?: return
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

    suspend fun findByQrToken(token: String): Card? = cardDao.findByQrToken(token)

    suspend fun updateQrToken(cardId: Long, token: String) = cardDao.updateQrToken(cardId, token)

    suspend fun lockCard(cardId: Long, traderId: String) = cardDao.lockCard(cardId, traderId)

    // === Phase 1: Filter ===

    fun getCardsByRiceVariety(variety: String): Flow<List<Card>> =
        cardDao.getCardsByRiceVariety(variety)

    fun getDistinctRiceVarieties(): Flow<List<String>> =
        cardDao.getDistinctRiceVarieties()

    // === Phase 3: Season Statistics Dashboard ===

    fun getDistinctSeasons(): Flow<List<String>> = cardDao.getDistinctSeasons()

    /** Stats cho 1 vụ → map raw → domain. */
    fun getSeasonStats(season: String): Flow<SeasonStats> =
        cardDao.getSeasonStats(season).map { it.toDomain(season) }

    /** Stats tổng toàn bộ data — dùng khi chưa có vụ nào hoặc fallback. */
    fun getOverallStats(): Flow<SeasonStats> =
        cardDao.getOverallStats().map { it.toDomain(season = "Tất cả") }

    fun getVarietyBreakdown(season: String): Flow<List<VarietyStat>> =
        cardDao.getVarietyBreakdown(season)

    fun getTopTraders(season: String): Flow<List<TraderStat>> =
        cardDao.getTopTraders(season)

    /** So sánh giữa nhiều vụ — map từng row Raw → SeasonStats. */
    fun getAllSeasonsComparison(): Flow<List<SeasonStats>> =
        cardDao.getAllSeasonsComparison().map { rows ->
            rows.map { row ->
                SeasonStatsRaw(
                    cardCount = row.cardCount,
                    totalNetWeight = row.totalNetWeight,
                    totalRevenue = row.totalRevenue,
                    totalPaid = row.totalPaid,
                    totalRemaining = row.totalRemaining,
                    avgPrice = row.avgPrice,
                    avgMoisture = row.avgMoisture,
                    totalBags = row.totalBags
                ).toDomain(row.season)
            }
        }
}

data class CardCalculationResult(
    val totalRawWeight: Double,
    val totalNetWeight: Double,
    val bagCount: Int,
    val totalPaid: Double,
    val totalDeposit: Double
)

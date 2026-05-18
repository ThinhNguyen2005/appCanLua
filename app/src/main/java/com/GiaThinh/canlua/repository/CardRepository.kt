package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.CardDao
import com.GiaThinh.canlua.data.dao.TransactionDao
import com.GiaThinh.canlua.data.dao.WeightEntryDao
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.Transaction
import com.GiaThinh.canlua.data.model.TransactionType
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.util.RiceCalculator
import kotlinx.coroutines.flow.Flow
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

    suspend fun updateCardCalculations(cardId: Long) {
        val card = cardDao.getCardById(cardId) ?: return
        val calculation = calculateCardTotals(cardId)
        
        val totalRaw = calculation.totalRawWeight
        val bagCount = calculation.bagCount
        val bagWeightVal = card.bagWeight
        val impurityVal = card.impurityWeight
        val moistureVal = card.moisturePercent

        // Khối lượng thực = Tổng khối lượng - (Tổng số bao × Trừ bì) - Tạp chất - (Tổng khối lượng × (Độ ẩm / 100))
        val netWeight = totalRaw - (bagCount * bagWeightVal) - impurityVal - (totalRaw * (moistureVal / 100.0))
        val finalNetWeight = netWeight.coerceAtLeast(0.0)

        val totalAmount = RiceCalculator.calcTotalAmount(
            finalNetWeight,
            card.pricePerKg
        )
        val remainingAmount = RiceCalculator.calcRemainingAmount(
            totalAmount,
            calculation.totalPaid,
            calculation.totalDeposit
        )

        val updatedCard = card.copy(
            totalWeight = totalRaw,
            netWeight = finalNetWeight,
            bagCount = bagCount,
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
}

data class CardCalculationResult(
    val totalRawWeight: Double,
    val totalNetWeight: Double,
    val bagCount: Int,
    val totalPaid: Double,
    val totalDeposit: Double
)

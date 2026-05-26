package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.RicePriceDao
import com.GiaThinh.canlua.data.firestore.FirestoreRicePrice
import com.GiaThinh.canlua.data.model.PricePoint
import com.GiaThinh.canlua.data.model.RicePrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

import kotlinx.coroutines.withContext

/**
 * Phase 2.1 — MVP với mock data realistic dựa trên giá thị trường ĐBSCL 2026.
 * Phase 2.2 — Firestore sync: TRADER write, FARMER read on-demand.
 *
 * v3 (2026-05-26): Bỏ snapshot listener thường trực. FARMER chỉ refresh khi mở tab
 * Market (one-shot fetch + cache vào Room). Lý do: listener treo singleton scope
 * không bao giờ hủy → bombard main thread cả khi user không xem giá.
 */
@Singleton
class MarketRepository @Inject constructor(
    private val ricePriceDao: RicePriceDao,
    private val firestore: MarketFirestoreRepository
) {
    fun getAllPrices(): Flow<List<RicePrice>> = ricePriceDao.getAllPrices()

    fun getPriceByVariety(variety: String): Flow<RicePrice?> =
        ricePriceDao.getPriceByVariety(variety)

    fun getHistory(variety: String, days: Int): Flow<List<PricePoint>> {
        val sinceMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return ricePriceDao.getHistory(variety, sinceMs)
    }

    /**
     * One-shot fetch từ Firestore → mirror vào Room. UI gọi khi vào tab Market.
     * Empty list không overwrite Room (giữ mock + dữ liệu cũ làm fallback).
     */
    suspend fun refreshFromFirestore(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = firestore.fetchActiveBids()
        result.fold(
            onSuccess = { bids ->
                if (bids.isNotEmpty()) {
                    ricePriceDao.upsertAll(bids.map { it.toRicePrice() })
                }
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }

    /** TRADER submit / update bid. */
    suspend fun submitBid(
        variety: String,
        priceMin: Double,
        priceMax: Double,
        region: String,
        trend: String,
        traderName: String,
        traderPhone: String,
        note: String,
        existingId: String? = null
    ): Result<String> {
        val avg = (priceMin + priceMax) / 2
        val bid = FirestoreRicePrice(
            id = existingId.orEmpty(),
            variety = variety,
            priceMin = priceMin,
            priceMax = priceMax,
            priceAvg7d = avg,
            region = region,
            updatedAt = System.currentTimeMillis(),
            traderId = firestore.currentUserId.orEmpty(),
            traderName = traderName,
            traderPhone = traderPhone,
            trend = trend,
            active = true,
            note = note
        )
        return firestore.upsertBid(bid)
    }

    suspend fun deleteBid(bidId: String): Result<Unit> = firestore.deactivateBid(bidId)

    /** Stream bids của TRADER hiện tại — đổ thẳng từ Firestore. */
    fun observeMyBids(): Flow<List<FirestoreRicePrice>> = firestore.observeMyBids()

    /**
     * Seed mock data nếu DB trống. Chạy 1 lần ở app startup cho demo.
     * Khi Firestore có data thật, mock sẽ bị overwrite (mock dùng id "mock_*").
     */
    suspend fun seedMockDataIfEmpty() = withContext(Dispatchers.IO) {
        if (ricePriceDao.countPrices() > 0) return@withContext
        val now = System.currentTimeMillis()
        val varieties = listOf(
            VarietyDef("ST25", 8000.0, 8500.0, "UP"),
            VarietyDef("ST24", 7800.0, 8200.0, "UP"),
            VarietyDef("OM18", 7500.0, 7900.0, "STABLE"),
            VarietyDef("Jasmine 85", 7200.0, 7600.0, "STABLE"),
            VarietyDef("IR50404", 6500.0, 6900.0, "DOWN"),
            VarietyDef("Đài Thơm 8", 7400.0, 7800.0, "STABLE"),
            VarietyDef("Nàng Hoa 9", 7600.0, 8000.0, "UP"),
            VarietyDef("OM5451", 7000.0, 7400.0, "STABLE")
        )

        val prices = varieties.map { v ->
            val priceMin = randomNear(v.minBase, 100.0)
            val priceMax = randomNear(v.maxBase, 100.0)
            val priceAvg = (priceMin + priceMax) / 2
            RicePrice(
                id = "mock_${v.variety}",
                variety = v.variety,
                priceMin = priceMin,
                priceMax = priceMax,
                priceAvg7d = priceAvg,
                region = "ĐBSCL",
                updatedAt = now,
                traderId = null,
                traderName = "Hệ thống tổng hợp",
                trend = v.trend
            )
        }
        ricePriceDao.upsertAll(prices)

        // Generate 30 days history per variety
        val history = mutableListOf<PricePoint>()
        for (v in varieties) {
            val basePrice = (v.minBase + v.maxBase) / 2
            for (dayBack in 30 downTo 0) {
                val date = now - TimeUnit.DAYS.toMillis(dayBack.toLong())
                val trendBias = when (v.trend) {
                    "UP" -> (30 - dayBack) * 8.0
                    "DOWN" -> -(30 - dayBack) * 6.0
                    else -> 0.0
                }
                val noise = Random.nextDouble(-150.0, 150.0)
                val avg = basePrice + trendBias + noise
                history.add(
                    PricePoint(
                        variety = v.variety,
                        date = date,
                        priceMin = avg - 200,
                        priceMax = avg + 200,
                        priceAvg = avg
                    )
                )
            }
        }
        ricePriceDao.clearHistory()
        ricePriceDao.insertHistory(history)
    }

    private fun randomNear(base: Double, jitter: Double): Double {
        val noise = Random.nextDouble(-jitter, jitter)
        return ((base + noise) / 50).roundToInt() * 50.0
    }

    private data class VarietyDef(
        val variety: String,
        val minBase: Double,
        val maxBase: Double,
        val trend: String
    )

    private fun FirestoreRicePrice.toRicePrice() = RicePrice(
        id = id,
        variety = variety,
        priceMin = priceMin,
        priceMax = priceMax,
        priceAvg7d = priceAvg7d,
        region = region,
        updatedAt = updatedAt,
        traderId = traderId.takeIf { it.isNotEmpty() },
        traderName = traderName.takeIf { it.isNotEmpty() },
        trend = trend
    )
}

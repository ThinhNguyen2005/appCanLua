package com.giathinh.canlua.repository

import android.util.Log
import com.giathinh.canlua.data.dao.RicePriceDao
import com.giathinh.canlua.data.model.PricePoint
import com.giathinh.canlua.data.model.RicePrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.withContext

/**
 * Market data repository — offline-first with Room as local cache.
 *
 * Remote source: Supabase `rice_prices` table (read-only from app).
 * Write path:    Google Sheets → Apps Script → Supabase REST API.
 *
 * Strategy:
 *  1. UI reads from Room (instant, always available offline).
 *  2. On tab open, call [refreshFromSupabase] which fetches Supabase → upserts Room.
 *  3. TTL prevents hammering the API on every recomposition.
 */
@Singleton
class MarketRepository @Inject constructor(
    private val ricePriceDao: RicePriceDao,
    private val supabase: SupabasePriceRepository
) {
    private var lastRefreshTimeMs = 0L
    private val refreshTtlMs = 10 * 60 * 1000L // 10 minutes

    fun getAllPrices(): Flow<List<RicePrice>> = ricePriceDao.getAllPrices()

    fun getPriceByVariety(variety: String): Flow<RicePrice?> =
        ricePriceDao.getPriceByVariety(variety)

    fun getHistory(variety: String, days: Int): Flow<List<PricePoint>> {
        val sinceMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return ricePriceDao.getHistory(variety, sinceMs)
    }

    /**
     * Fetch fresh prices from Supabase → mirror into Room.
     * Respects [refreshTtlMs] cache unless [forceRefresh] = true.
     * On network failure, Room cache remains untouched (graceful degradation).
     */
    suspend fun refreshFromSupabase(forceRefresh: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val isFresh = !forceRefresh
                    && (now - lastRefreshTimeMs < refreshTtlMs)
                    && ricePriceDao.countPrices() > 0

            if (isFresh) {
                Log.d("MarketRepo", "Cache fresh (<10m), skipping network fetch")
                return@withContext Result.success(Unit)
            }

            val result = supabase.fetchActivePrices()
            result.fold(
                onSuccess = { prices ->
                    if (prices.isNotEmpty()) {
                        ricePriceDao.clear()
                        ricePriceDao.upsertAll(prices)
                        Log.d("MarketRepo", "Updated Room with ${prices.size} prices from Supabase")
                    }
                    lastRefreshTimeMs = now
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.w("MarketRepo", "Supabase fetch failed, using Room cache: ${error.message}")
                    Result.failure(error)
                }
            )
        }

    /**
     * Seed mock data on first launch when Room is empty.
     * Real data from Supabase will overwrite mocks on first successful fetch.
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
            RicePrice(
                id = "mock_${v.variety}",
                variety = v.variety,
                priceMin = priceMin,
                priceMax = priceMax,
                priceAvg7d = (priceMin + priceMax) / 2,
                region = "ĐBSCL",
                updatedAt = now,
                traderId = null,
                traderName = "Hệ thống tổng hợp",
                trend = v.trend
            )
        }
        ricePriceDao.upsertAll(prices)

        // Generate 30-day price history per variety
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
                val avg = basePrice + trendBias + Random.nextDouble(-150.0, 150.0)
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
}

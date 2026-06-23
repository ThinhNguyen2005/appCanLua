package com.giathinh.canlua.repository

import android.util.Log
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.data.remote.SupabaseClient
import com.giathinh.canlua.data.remote.SupabaseException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Fetches rice prices from Supabase (PostgreSQL) — replaces MarketFirestoreRepository.
 *
 * Table: rice_prices
 * RLS: public SELECT, service_role INSERT/UPDATE (via GAS or admin dashboard).
 *
 * The app is read-only. Write path is: Google Sheets → Apps Script → Supabase REST.
 */
@Singleton
class SupabasePriceRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @Named("supabaseUrl") private val baseUrl: String,
    @Named("supabaseAnonKey") private val anonKey: String
) {
    companion object {
        private const val TABLE = "rice_prices"
        private const val TAG = "SupabasePriceRepo"
    }

    /**
     * Fetch all active rice prices, ordered by most recently updated.
     * Returns empty list on network error (caller falls back to Room cache).
     */
    suspend fun fetchActivePrices(): Result<List<RicePrice>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = com.google.gson.reflect.TypeToken
                .getParameterized(List::class.java, RicePriceDto::class.java).type
            val dtos = supabase.getList<RicePriceDto>(
                baseUrl = baseUrl,
                anonKey = anonKey,
                table = TABLE,
                params = mapOf(
                    "active" to "eq.true",
                    "order" to "updated_at.desc"
                ),
                type = type
            )
            Log.d(TAG, "Fetched ${dtos.size} prices from Supabase")
            dtos.map { it.toRicePrice() }
        }.onFailure { e ->
            when (e) {
                is SupabaseException -> Log.w(TAG, "Supabase error ${e.code}: ${e.errorBody}")
                else -> Log.w(TAG, "Network error fetching prices: ${e.message}")
            }
        }
    }
}

/** DTO matching the Supabase `rice_prices` table column names (snake_case). */
data class RicePriceDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("variety") val variety: String = "",
    @SerializedName("price_min") val price_min: Double = 0.0,
    @SerializedName("price_max") val price_max: Double = 0.0,
    @SerializedName("price_avg7d") val price_avg7d: Double = 0.0,
    @SerializedName("region") val region: String = "ĐBSCL",
    @SerializedName("updated_at") val updated_at: Long = 0L,
    @SerializedName("trend") val trend: String = "STABLE",
    @SerializedName("rice_type") val rice_type: String = "lúa khô",
    @SerializedName("source") val source: String? = null,
    @SerializedName("active") val active: Boolean = true
) {
    fun toRicePrice() = RicePrice(
        id = id,
        variety = variety,
        priceMin = price_min,
        priceMax = price_max,
        priceAvg7d = price_avg7d,
        region = region,
        updatedAt = updated_at,
        traderId = null,
        traderName = source,
        trend = trend,
        riceType = rice_type
    )
}

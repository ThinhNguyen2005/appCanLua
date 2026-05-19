package com.GiaThinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.GiaThinh.canlua.data.model.PricePoint
import com.GiaThinh.canlua.data.model.RicePrice
import kotlinx.coroutines.flow.Flow

@Dao
interface RicePriceDao {

    @Query("SELECT * FROM rice_prices ORDER BY updatedAt DESC")
    fun getAllPrices(): Flow<List<RicePrice>>

    @Query("SELECT * FROM rice_prices WHERE variety = :variety LIMIT 1")
    fun getPriceByVariety(variety: String): Flow<RicePrice?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(prices: List<RicePrice>)

    @Query("DELETE FROM rice_prices")
    suspend fun clear()

    @Query("SELECT * FROM price_history WHERE variety = :variety AND date >= :sinceMs ORDER BY date ASC")
    fun getHistory(variety: String, sinceMs: Long): Flow<List<PricePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(points: List<PricePoint>)

    @Query("DELETE FROM price_history")
    suspend fun clearHistory()
}

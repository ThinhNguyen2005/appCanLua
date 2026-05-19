package com.GiaThinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.GiaThinh.canlua.data.model.WeatherCache
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    fun observe(): Flow<WeatherCache?>

    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    suspend fun get(): WeatherCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: WeatherCache)

    @Query("DELETE FROM weather_cache")
    suspend fun clear()
}

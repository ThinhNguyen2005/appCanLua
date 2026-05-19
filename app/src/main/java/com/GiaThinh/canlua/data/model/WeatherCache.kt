package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache thời tiết — luôn chỉ 1 row (id = SINGLETON_ID = 1).
 * REPLACE strategy của Room sẽ overwrite mỗi lần fetch mới.
 */
@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val location: String,
    val temperature: Int,
    val feelsLike: Int,
    val condition: String,
    val rainChance: Int,
    val humidity: Int,
    val windSpeed: Double,
    val iconKey: String,
    val advisory: String?,
    val cachedAt: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/** Convert từ domain model → cache row. */
fun com.GiaThinh.canlua.data.model.WeatherInfo.toCache(): WeatherCache = WeatherCache(
    location = location,
    temperature = temperature,
    feelsLike = feelsLike,
    condition = condition,
    rainChance = rainChance,
    humidity = humidity,
    windSpeed = windSpeed,
    iconKey = iconKey,
    advisory = advisory,
    cachedAt = updatedAt
)

/** Convert từ cache → domain model (giữ updatedAt = cachedAt). */
fun WeatherCache.toInfo(): WeatherInfo = WeatherInfo(
    location = location,
    temperature = temperature,
    feelsLike = feelsLike,
    condition = condition,
    rainChance = rainChance,
    humidity = humidity,
    windSpeed = windSpeed,
    iconKey = iconKey,
    updatedAt = cachedAt,
    advisory = advisory
)

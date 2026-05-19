package com.GiaThinh.canlua.data.model

/**
 * Snapshot thời tiết được app sử dụng — đã chuẩn hoá đơn vị Celsius, %.
 */
data class WeatherInfo(
    val location: String,
    val temperature: Int,           // °C
    val feelsLike: Int,
    val condition: String,          // tiếng Việt
    val rainChance: Int,            // % (xấp xỉ từ clouds + main)
    val humidity: Int,
    val windSpeed: Double,          // m/s
    val iconKey: String,            // OpenWeather icon key, vd "01d"
    val updatedAt: Long,
    val advisory: String?           // Cảnh báo nông nghiệp (vd: "Mưa lớn — hoãn phun thuốc")
)

package com.GiaThinh.canlua.data.remote.weather

import com.google.gson.annotations.SerializedName

/**
 * Subset của OpenWeatherMap "Current Weather" v2.5 API response.
 * Doc: https://openweathermap.org/current
 */
data class OpenWeatherResponse(
    val name: String = "",                  // city name
    val main: WeatherMain = WeatherMain(),
    val weather: List<WeatherCondition> = emptyList(),
    val wind: WeatherWind = WeatherWind(),
    val clouds: WeatherClouds = WeatherClouds(),
    @SerializedName("dt") val timestamp: Long = 0L
)

data class WeatherMain(
    val temp: Double = 0.0,
    @SerializedName("feels_like") val feelsLike: Double = 0.0,
    val humidity: Int = 0,
    val pressure: Int = 0
)

data class WeatherCondition(
    val main: String = "",         // "Rain", "Clear", "Clouds"
    val description: String = "",
    val icon: String = ""
)

data class WeatherWind(val speed: Double = 0.0, val deg: Int = 0)
data class WeatherClouds(val all: Int = 0)

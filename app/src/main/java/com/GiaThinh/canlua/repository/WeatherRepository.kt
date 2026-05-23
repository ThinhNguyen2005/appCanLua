package com.GiaThinh.canlua.repository

import android.util.Log
import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.data.dao.WeatherCacheDao
import com.GiaThinh.canlua.data.location.GeoPoint
import com.GiaThinh.canlua.data.location.LocationProvider
import com.GiaThinh.canlua.data.model.WeatherCache
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.data.model.toCache
import com.GiaThinh.canlua.data.model.toInfo
import com.GiaThinh.canlua.data.remote.HttpClient
import com.GiaThinh.canlua.data.remote.HttpException
import com.GiaThinh.canlua.data.remote.weather.OpenWeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val TAG = "WeatherRepo"

/**
 * UI state cho weather. Dùng sealed class để encode 3 trạng thái rõ ràng.
 */
sealed class WeatherState {
    object Loading : WeatherState()
    data class Data(val info: WeatherInfo, val isStale: Boolean) : WeatherState()
    data class Error(val message: String?, val cached: WeatherInfo? = null) : WeatherState()
    object RateLimited : WeatherState()
}

/**
 * Phase 2.3 + cache offline:
 * - Cache singleton row trong Room (id=1).
 * - Fresh: <60 phút → serve cache, không call API.
 * - Stale: 60 phút–24h → serve cache + refresh ngầm.
 * - Expired: >24h → coi như không có cache (vẫn dùng nếu offline).
 * - HTTP 429: emit RateLimited để UI ẩn card.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val locationProvider: LocationProvider,
    private val cacheDao: WeatherCacheDao
) {
    private val freshTtlMs = 60 * 60 * 1000L
    private val usableTtlMs = 24 * 60 * 60 * 1000L

    /**
     * Cold flow:
     * 1) Emit cache ngay nếu có (Data với isStale tuỳ tuổi).
     * 2) Nếu cache không tồn tại hoặc stale → fetch network ngầm, emit khi xong.
     * 3) Nếu network fail và có cache → giữ cache (đã emit). Nếu không có cache → Error.
     */
    fun observeWeather(forceRefresh: Boolean = false): Flow<WeatherState> = flow {
        val cached = cacheDao.get()
        val now = System.currentTimeMillis()

        if (cached != null) {
            val age = now - cached.cachedAt
            val isStale = age >= freshTtlMs
            Log.d(TAG, "observeWeather: cache hit, ageMs=$age isStale=$isStale forceRefresh=$forceRefresh")
            emit(WeatherState.Data(cached.toInfo(), isStale = isStale))

            // Fresh + không force → dừng, tiết kiệm API call
            if (!isStale && !forceRefresh) return@flow
        } else {
            Log.d(TAG, "observeWeather: no cache, forceRefresh=$forceRefresh")
            emit(WeatherState.Loading)
        }

        // Refresh
        Log.d(TAG, "observeWeather: refreshing from network…")
        val result = fetchFromNetwork()
        result.onSuccess { fresh ->
            Log.d(TAG, "observeWeather: network success location=${fresh.location} temp=${fresh.temperature}")
            cacheDao.upsert(fresh.toCache())
            emit(WeatherState.Data(fresh, isStale = false))
        }.onFailure { err ->
            if (err is HttpException) {
                Log.e(TAG, "observeWeather: HTTP ${err.code} body=${err.errorBody}")
            } else {
                Log.e(TAG, "observeWeather: network failure (${err.javaClass.simpleName}): ${err.message}", err)
            }
            if (err is HttpException && err.code == 429) {
                emit(WeatherState.RateLimited)
            } else if (cached == null) {
                emit(WeatherState.Error(err.message))
            } else {
                Log.w(TAG, "observeWeather: keeping stale cache after network failure")
            }
            // có cache rồi → giữ nguyên emit Data trước đó
        }
    }.flowOn(Dispatchers.IO)

    /** Force refresh. Dùng cho pull-to-refresh / tap widget. */
    suspend fun refresh(): Result<WeatherInfo> {
        val result = fetchFromNetwork()
        result.onSuccess { cacheDao.upsert(it.toCache()) }
        return result
    }

    private suspend fun fetchFromNetwork(): Result<WeatherInfo> {
        if (BuildConfig.OPENWEATHER_API_KEY.isEmpty()) {
            Log.e(TAG, "fetchFromNetwork: OPENWEATHER_API_KEY rỗng — kiểm tra local.properties + BuildConfig")
            return Result.failure(IllegalStateException(
                "Thiếu OPENWEATHER_API_KEY trong local.properties"
            ))
        }
        val locFromProvider = locationProvider.getCurrentLocation()
        if (locFromProvider == null) {
            Log.w(TAG, "fetchFromNetwork: locationProvider null — fallback Cần Thơ centroid (10.045, 105.746)")
        } else {
            Log.d(TAG, "fetchFromNetwork: location lat=${locFromProvider.lat} lon=${locFromProvider.lon}")
        }
        val location = locFromProvider ?: GeoPoint(10.045, 105.746)

        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=${location.lat}&lon=${location.lon}" +
                "&appid=${BuildConfig.OPENWEATHER_API_KEY}" +
                "&units=metric&lang=vi"
            // KHÔNG log full url — chứa appid. Chỉ log host + lat/lon.
            Log.d(TAG, "fetchFromNetwork: GET api.openweathermap.org lat=${location.lat} lon=${location.lon}")
            val response: OpenWeatherResponse = httpClient.get(url)

            // OWM `name` hàng thường trả tên thành phố lớn (vd "Ho Chi Minh City")
            // chứ không đúng Quận. Override bằng Geocoder native để lấy "Quận 12, TP. HCM".
            val accurateName = locationProvider.reverseGeocode(location.lat, location.lon)
            Result.success(response.toWeatherInfo(overrideName = accurateName))
        } catch (e: HttpException) {
            Log.e(TAG, "fetchFromNetwork: HTTP ${e.code} body=${e.errorBody}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "fetchFromNetwork: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun OpenWeatherResponse.toWeatherInfo(overrideName: String? = null): WeatherInfo {
        val condition = weather.firstOrNull()
        val rainChance = clouds.all
        val descvi = condition?.description?.replaceFirstChar { it.uppercase() } ?: "Không rõ"
        val finalLocation = overrideName?.takeIf { it.isNotBlank() }
            ?: name.ifEmpty { "Vị trí của bạn" }
        return WeatherInfo(
            location = finalLocation,
            temperature = main.temp.roundToInt(),
            feelsLike = main.feelsLike.roundToInt(),
            condition = descvi,
            rainChance = rainChance,
            humidity = main.humidity,
            windSpeed = wind.speed,
            iconKey = condition?.icon.orEmpty(),
            updatedAt = System.currentTimeMillis(),
            advisory = buildAdvisory(condition?.main, rainChance, main.temp.roundToInt(), main.humidity)
        )
    }

    private fun buildAdvisory(main: String?, rainChance: Int, temp: Int, humidity: Int): String? {
        return when {
            main == "Thunderstorm" -> "Sấm sét — không phun thuốc, hạn chế ra đồng"
            main == "Rain" || rainChance >= 70 -> "Mưa nhiều — hoãn phun thuốc 24h, gia cố bờ"
            temp >= 35 && humidity < 60 -> "Nắng gắt — kiểm tra đủ nước ruộng, tưới chiều mát"
            humidity >= 90 && temp in 25..32 -> "Ẩm cao — chú ý đạo ôn, rầy nâu"
            else -> null
        }
    }
}

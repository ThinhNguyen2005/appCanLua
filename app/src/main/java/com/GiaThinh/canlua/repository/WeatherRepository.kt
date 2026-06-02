package com.GiaThinh.canlua.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.Log
import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.util.ApiKeyObfuscator
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    object NoPermission : WeatherState()
}

/**
 * Phase 2.3 + cache offline:
 * - Cache singleton row trong Room (id=1).
 * - Fresh: <60 phút → serve cache, không call API.
 * - Stale: 60 phút–24h → serve cache + refresh ngầm.
 * - Expired: >24h → coi như không có cache (vẫn dùng nếu offline).
 * - HTTP 429: emit RateLimited để UI ẩn card.
 *
 * Hot StateFlow singleton: mọi subscriber (WeatherViewModel, DashboardViewModel, ...)
 * đọc cùng một StateFlow → chỉ 1 network call khi cache stale, không double-fetch.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val locationProvider: LocationProvider,
    private val cacheDao: WeatherCacheDao
) {
    private val freshTtlMs = 60 * 60 * 1000L
    private val usableTtlMs = 24 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: kotlinx.coroutines.Job? = null

    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)

    /** Hot StateFlow — mọi caller nhận cùng state, không tạo thêm network call. */
    val state: StateFlow<WeatherState> = _state.asStateFlow()



    /**
     * Gọi khi user pull-to-refresh hoặc permission được cấp.
     * Emit lại từ đầu qua cold flow, kết quả cập nhật vào shared StateFlow.
     */
    fun requestRefresh(forceRefresh: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = scope.launch { collectWeather(forceRefresh = forceRefresh) }
    }

    /** Tương thích ngược — delegate sang StateFlow để không sửa caller cũ. */
    fun observeWeather(forceRefresh: Boolean = false): StateFlow<WeatherState> {
        if (forceRefresh || _state.value is WeatherState.Loading) {
            requestRefresh(forceRefresh = forceRefresh)
        }
        return state
    }

    private suspend fun collectWeather(forceRefresh: Boolean) {
        coldWeatherFlow(forceRefresh).collect { _state.value = it }
    }

    private fun coldWeatherFlow(forceRefresh: Boolean): Flow<WeatherState> = flow {
        if (!locationProvider.hasPermission()) {
            emit(WeatherState.NoPermission)
            return@flow
        }
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
        val result = fetchFromNetwork(forceFreshLocation = forceRefresh)
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
    suspend fun refresh(): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        val result = fetchFromNetwork(forceFreshLocation = true)
        result.onSuccess { cacheDao.upsert(it.toCache()) }
        result
    }

    private suspend fun fetchFromNetwork(forceFreshLocation: Boolean = false): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        if (ApiKeyObfuscator.decode(BuildConfig.OPENWEATHER_API_KEY).isEmpty()) {
            Log.e(TAG, "fetchFromNetwork: OPENWEATHER_API_KEY rỗng — kiểm tra local.properties + BuildConfig")
            return@withContext Result.failure(IllegalStateException(
                context.getString(com.GiaThinh.canlua.R.string.weather_error_missing_api_key)
            ))
        }
        val locFromProvider = locationProvider.getCurrentLocation(forceFresh = forceFreshLocation)
        if (locFromProvider == null) {
            Log.w(TAG, "fetchFromNetwork: locationProvider null — fallback Cần Thơ centroid (10.045, 105.746)")
        } else {
            Log.d(TAG, "fetchFromNetwork: location lat=${locFromProvider.lat} lon=${locFromProvider.lon}")
        }
        val location = locFromProvider ?: GeoPoint(10.045, 105.746)

        return@withContext try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=${location.lat}&lon=${location.lon}" +
                "&appid=${ApiKeyObfuscator.decode(BuildConfig.OPENWEATHER_API_KEY)}" +
                "&units=metric&lang=vi"
            // KHÔNG log full url — chứa appid. Chỉ log host + lat/lon.
            Log.d(TAG, "fetchFromNetwork: GET api.openweathermap.org lat=${location.lat} lon=${location.lon}")
            val response: OpenWeatherResponse = httpClient.get(url)

            Result.success(response.toWeatherInfo(overrideName = null))
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
        val descvi = condition?.description?.replaceFirstChar { it.uppercase() } 
            ?: context.getString(com.GiaThinh.canlua.R.string.weather_unknown)
        val finalLocation = overrideName?.takeIf { it.isNotBlank() }
            ?: name.ifEmpty { context.getString(com.GiaThinh.canlua.R.string.weather_your_location) }
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
            main == "Thunderstorm" -> context.getString(com.GiaThinh.canlua.R.string.weather_advisory_thunderstorm)
            main == "Rain" || rainChance >= 70 -> context.getString(com.GiaThinh.canlua.R.string.weather_advisory_rain)
            temp >= 35 && humidity < 60 -> context.getString(com.GiaThinh.canlua.R.string.weather_advisory_hot_sun)
            humidity >= 90 && temp in 25..32 -> context.getString(com.GiaThinh.canlua.R.string.weather_advisory_high_humidity)
            else -> null
        }
    }
}

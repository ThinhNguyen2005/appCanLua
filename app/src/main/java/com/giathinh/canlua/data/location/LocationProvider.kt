package com.giathinh.canlua.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class GeoPoint(val lat: Double, val lon: Double)

/**
 * Wrapper FusedLocationProviderClient — luôn ưu tiên GPS chính xác cao.
 *
 * Vấn đề trước đây: dùng [Priority.PRIORITY_BALANCED_POWER_ACCURACY] → chỉ dựa
 * vào WiFi/Cell Tower, sai số 500-2000m, ở vùng giáp ranh quận hay nhảy sang
 * trạm phát sóng quận bên cạnh. Đã đổi sang [Priority.PRIORITY_HIGH_ACCURACY]
 * để bắt buộc dùng chip GPS, sai số 5-10m.
 */
@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // In-memory cache để tránh kích hoạt GPS chip mỗi lần gọi. User nông dân thường
    // ở 1 chỗ cả ngày → fresh GPS mỗi 6h là đủ; các call screen-load nên hit cache.
    // Call site cần coordinate mới (tạo phiếu, mở map) phải truyền forceFresh=true.
    @Volatile private var cachedGeo: GeoPoint? = null
    @Volatile private var cachedAt: Long = 0L
    private val cacheTtlMs = 6 * 60 * 60 * 1000L // 6 giờ

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Xoá cache để force fresh fetch lần tiếp theo (vd khi user cấp lại permission). */
    fun invalidateCache() {
        cachedGeo = null
        cachedAt = 0L
    }

    /**
     * Lấy toạ độ hiện tại, ưu tiên GPS thật.
     *
     * Logic:
     * 1. Nếu cache còn fresh (< 6h) và không forceFresh → return cache (không bật GPS).
     * 2. Nếu có FINE permission → dùng PRIORITY_HIGH_ACCURACY (GPS chip).
     * 3. Nếu chỉ có COARSE → fallback BALANCED (WiFi/Cell).
     * 4. Timeout 8 giây để tránh treo UI khi GPS không có sóng.
     * 5. Nếu fail thì lấy lastLocation (có thể stale).
     *
     * @param forceFresh true = bypass cache, bật GPS thật. Dùng khi user chủ động
     *                   refresh weather, tạo phiếu mới, hoặc mở RiceMapScreen.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(forceFresh: Boolean = false): GeoPoint? {
        if (!forceFresh) {
            val now = System.currentTimeMillis()
            cachedGeo?.let { if (now - cachedAt < cacheTtlMs) return it }
        }
        if (!hasPermission()) return null

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return try {
            val cts = CancellationTokenSource()
            val loc = withTimeoutOrNull(8_000L) {
                client.getCurrentLocation(priority, cts.token).await()
            }
            cts.cancel()
            val result = loc?.let { GeoPoint(it.latitude, it.longitude) }
                ?: client.lastLocation.await()?.let { GeoPoint(it.latitude, it.longitude) }
            result?.also {
                cachedGeo = it
                cachedAt = System.currentTimeMillis()
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reverse geocoding: toạ độ → "Quận 12, TP. Hồ Chí Minh".
     *
     * Dùng [Geocoder] native (offline-friendly, có cache hệ thống). Cố gắng ghép
     * `subLocality` (Phường) + `locality` (Quận) + `adminArea` (Tỉnh/TP).
     *
     * Trả về null nếu thiết bị không có Geocoder service hoặc network fail.
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
                val addr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }
                addr?.let { a ->
                    listOfNotNull(
                        a.subLocality,    // Phường Tân Thới Hiệp
                        a.locality,       // Quận 12
                        a.adminArea       // TP. Hồ Chí Minh
                    ).distinct().joinToString(", ").ifEmpty { null }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

package com.GiaThinh.canlua.data.location

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

    /**
     * Lấy toạ độ hiện tại, ưu tiên GPS thật.
     *
     * Logic:
     * 1. Nếu có FINE permission → dùng PRIORITY_HIGH_ACCURACY (GPS chip).
     * 2. Nếu chỉ có COARSE → fallback BALANCED (WiFi/Cell).
     * 3. Timeout 8 giây để tránh treo UI khi GPS không có sóng.
     * 4. Nếu fail thì lấy lastLocation (có thể stale).
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GeoPoint? {
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
            loc?.let { GeoPoint(it.latitude, it.longitude) }
                ?: client.lastLocation.await()?.let { GeoPoint(it.latitude, it.longitude) }
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

package com.giathinh.canlua.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class GeoPoint(val lat: Double, val lon: Double)

/**
 * Wrapper FusedLocationProviderClient — ưu tiên độ chính xác cao khi có FINE permission.
 *
 * Lưu ý: [Priority.PRIORITY_HIGH_ACCURACY] yêu cầu độ chính xác cao nhất có thể
 * từ các nguồn định vị khả dụng (GNSS/GPS, Wi-Fi, Cell tower, sensors), không đảm bảo
 * hoặc ép buộc phải kích hoạt chip GPS phần cứng hay cam kết độ chính xác cố định 5-10m.
 */
@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // In-memory cache để tránh kích hoạt định vị mỗi lần mở màn hình thô sơ.
    // Cache TTL 6h CHỈ dùng cho coarse / non-critical UI (như thời tiết trên dashboard).
    // Các thao tác nhạy cảm với toạ độ (tạo phiếu cân, ghim thửa ruộng RiceMap)
    // BẮT BUỘC phải truyền forceFresh = true.
    @Volatile private var cachedGeo: GeoPoint? = null
    @Volatile private var cachedAt: Long = 0L
    private val cacheTtl = 6.hours
    private val maxLastLocationAge = 30.minutes

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
     * Lấy toạ độ hiện tại.
     *
     * Logic:
     * 1. Nếu cache còn fresh (< 6h) và không forceFresh → trả về toạ độ cache.
     * 2. Nếu có FINE permission → dùng PRIORITY_HIGH_ACCURACY.
     * 3. Nếu chỉ có COARSE → fallback PRIORITY_BALANCED_POWER_ACCURACY.
     * 4. Timeout 8 giây để tránh treo UI khi không bắt được sóng định vị.
     * 5. Nếu timeout/thất bại → fallback sang lastLocation nếu tuổi toạ độ <= 30 phút.
     *    Toạ độ fallback từ lastLocation không được reset hạn cache 6h mới.
     *
     * @param forceFresh true = bypass cache, buộc lấy toạ độ mới nhất từ thiết bị.
     *                   Bắt buộc dùng khi user tạo phiếu mới hoặc mở bản đồ thửa ruộng.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(forceFresh: Boolean = false): GeoPoint? {
        if (!forceFresh) {
            val now = SystemClock.elapsedRealtime()
            cachedGeo?.let {
                if (now - cachedAt < cacheTtl.inWholeMilliseconds) return it
            }
        }
        if (!hasPermission()) return null

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return try {
            val cts = CancellationTokenSource()
            val loc = try {
                withTimeoutOrNull(8.seconds) {
                    client.getCurrentLocation(priority, cts.token).await()
                }
            } finally {
                cts.cancel()
            }

            if (loc != null) {
                val point = GeoPoint(loc.latitude, loc.longitude)
                cachedGeo = point
                cachedAt = SystemClock.elapsedRealtime()
                point
            } else {
                val lastLoc = client.lastLocation.await()
                if (lastLoc != null) {
                    val lastLocElapsedMs = LocationCompat.getElapsedRealtimeMillis(lastLoc)
                    val ageMs = SystemClock.elapsedRealtime() - lastLocElapsedMs
                    if (ageMs in 0..maxLastLocationAge.inWholeMilliseconds) {
                        val point = GeoPoint(lastLoc.latitude, lastLoc.longitude)
                        cachedGeo = point
                        // Ghi nhận thời điểm fix thực tế của lastLocation, tránh cộng dồn thêm 6h cache
                        cachedAt = lastLocElapsedMs
                        point
                    } else {
                        Log.w(TAG, "lastLocation discarded because it is too stale: ${ageMs}ms")
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing or revoked during fetch", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current location", e)
            null
        }
    }

    /**
     * Reverse geocoding: toạ độ → "Quận 12, TP. Hồ Chí Minh".
     *
     * Dùng [Geocoder] native (offline-friendly, có cache hệ thống). Ghép
     * `subLocality` (Phường/Xã) + `locality` (Quận/Huyện) + `adminArea` (Tỉnh/TP).
     *
     * Trên Android 13+ (API 33), Geocoder cung cấp callback bất đồng bộ native nên không
     * cần switch sang [Dispatchers.IO]. Bản legacy (< API 33) là blocking call nên chạy trên [Dispatchers.IO].
     * Timeout 5 giây để tránh coroutine bị treo khi dịch vụ định danh địa chỉ phản hồi chậm.
     *
     * Trả về null nếu thiết bị không có Geocoder service, quá thời gian hoặc network fail.
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            withTimeoutOrNull(5.seconds) {
                val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
                val addr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    }
                }
                addr?.let { a ->
                    listOfNotNull(
                        a.subLocality,    // Phường Tân Thới Hiệp
                        a.locality,       // Quận 12
                        a.adminArea       // TP. Hồ Chí Minh
                    ).distinct().joinToString(", ").ifEmpty { null }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reverse geocode lat=$lat, lon=$lon", e)
            null
        }
    }

    companion object {
        private const val TAG = "LocationProvider"
    }
}

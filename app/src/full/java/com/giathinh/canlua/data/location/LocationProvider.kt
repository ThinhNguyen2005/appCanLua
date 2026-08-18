package com.giathinh.canlua.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GeoPoint(val lat: Double, val lon: Double)

@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @Volatile
    private var cachedLocation: GeoPoint? = null

    fun hasPermission(): Boolean = hasFineLocationPermission() || hasCoarseLocationPermission()

    fun hasFineLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasCoarseLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun invalidateCache() {
        cachedLocation = null
    }

    suspend fun getCurrentLocation(forceFresh: Boolean = false): GeoPoint? {
        if (!hasPermission()) return null
        if (!forceFresh) {
            cachedLocation?.let { return it }
        }

        val priority = if (hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val location = runCatching {
            if (forceFresh) {
                fusedLocationClient.getCurrentLocation(
                    CurrentLocationRequest.Builder().setPriority(priority).build(),
                    null
                ).await()
            } else {
                fusedLocationClient.lastLocation.await()
                    ?: fusedLocationClient.getCurrentLocation(
                        CurrentLocationRequest.Builder().setPriority(priority).build(),
                        null
                    ).await()
            }
        }.getOrNull() ?: return null

        return GeoPoint(location.latitude, location.longitude).also { cachedLocation = it }
    }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull()
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

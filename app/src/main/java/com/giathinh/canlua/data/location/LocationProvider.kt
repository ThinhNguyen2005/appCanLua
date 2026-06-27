package com.giathinh.canlua.data.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GeoPoint(val lat: Double, val lon: Double)

@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean = false
    fun hasFineLocationPermission(): Boolean = false
    fun invalidateCache() {}

    suspend fun getCurrentLocation(forceFresh: Boolean = false): GeoPoint? {
        return null
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String? {
        return null
    }
}

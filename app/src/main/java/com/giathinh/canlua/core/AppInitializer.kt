package com.giathinh.canlua.core

import android.app.Application

/**
 * Interface trừu tượng hóa khởi tạo ứng dụng cho từng Flavor.
 * - Bản Full: Khởi tạo Firebase AppCheck, Analytics, Crashlytics, RemoteConfig, Sync listeners.
 * - Bản Lite: Khởi tạo offline-only (No-Op).
 */
interface AppInitializer {
    fun init(application: Application)
}

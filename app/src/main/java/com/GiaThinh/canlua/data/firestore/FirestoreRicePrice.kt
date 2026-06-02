package com.GiaThinh.canlua.data.firestore

import com.google.firebase.firestore.Exclude

/**
 * DTO cho Firestore collection `market_prices`.
 * TRADER tự nhập, FARMER đọc.
 */
data class FirestoreRicePrice(
    val id: String = "",
    val variety: String = "",
    val priceMin: Double = 0.0,
    val priceMax: Double = 0.0,
    val priceAvg7d: Double = 0.0,
    val region: String = "",
    val updatedAt: Long = 0L,
    val traderId: String = "",
    val traderName: String = "",
    val traderPhone: String = "",
    val source: String = "",       // Map đúng trường "source" từ Firestore
    val trend: String = "STABLE",
    val active: Boolean = true,    // TRADER có thể tạm ẩn
    val note: String = "",
    val riceType: String = "lúa Khô", // Thêm loại sản phẩm
    val syncSource: String = "",      // Metadata nguồn sync
    val syncedAt: Long = 0L,          // Metadata thời gian sync
    @get:Exclude @set:Exclude var isFromCache: Boolean = false
)


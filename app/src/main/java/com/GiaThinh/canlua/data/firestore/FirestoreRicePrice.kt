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
    val trend: String = "STABLE",
    val active: Boolean = true,    // TRADER có thể tạm ẩn
    val note: String = "",
    @get:Exclude @set:Exclude var isFromCache: Boolean = false
)


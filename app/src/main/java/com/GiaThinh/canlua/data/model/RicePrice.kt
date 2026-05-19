package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bản ghi giá thu mua lúa theo từng giống.
 * Phase 2.1 — MVP read-only với mock data.
 * Phase 2.2 — TRADER tự nhập, sync Firestore.
 */
@Entity(tableName = "rice_prices")
data class RicePrice(
    @PrimaryKey val id: String,
    val variety: String,            // ST25, OM18, Jasmine 85, IR50404...
    val priceMin: Double,           // VNĐ/kg
    val priceMax: Double,
    val priceAvg7d: Double,
    val region: String,             // "ĐBSCL", "Cần Thơ"...
    val updatedAt: Long,            // epoch ms
    val traderId: String? = null,   // null nếu là mock data
    val traderName: String? = null,
    val trend: String = "STABLE"    // UP, DOWN, STABLE
)

/**
 * Một điểm dữ liệu lịch sử giá theo ngày để vẽ chart.
 */
@Entity(tableName = "price_history")
data class PricePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variety: String,
    val date: Long,                 // epoch ms (ngày)
    val priceMin: Double,
    val priceMax: Double,
    val priceAvg: Double
)

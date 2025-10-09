package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cccd: String? = null,
    val date: Date,
    val totalWeight: Double = 0.0, // Tổng khối lượng
    val bagWeight: Double = 0.0, // Khối lượng bao bì
    val impurityWeight: Double = 0.0, // Khối lượng tạp chất
    val netWeight: Double = 0.0, // Khối lượng đã trừ (bao bì + tạp chất)
    val depositAmount: Double = 0.0, // Tiền cọc
    val pricePerKg: Double = 0.0, // Đơn giá/kg
    val totalAmount: Double = 0.0, // Thành tiền
    val paidAmount: Double = 0.0, // Đã trả
    val remainingAmount: Double = 0.0, // Còn lại phải trả
    val bagCount: Int = 0, // Số bao
    val isLocked: Boolean = false // Khóa để tránh nhập nhầm
)


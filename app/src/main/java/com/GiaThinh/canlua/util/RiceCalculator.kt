package com.GiaThinh.canlua.util

import java.security.MessageDigest

/**
 * Công cụ tính toán chuẩn ngành lúa gạo.
 * - Quy đổi khối lượng theo độ ẩm (standard 14%)
 * - Khối lượng thực (trừ bao bì + tạp chất)
 * - Tạo QR token xác thực giao dịch
 */
object RiceCalculator {

    private const val STANDARD_MOISTURE = 14.0

    /**
     * Quy đổi khối lượng theo độ ẩm về chuẩn 14%.
     * Công thức: W_std = W_raw × (100 - moisture) / (100 - 14)
     */
    fun calcStandardWeight(
        rawWeight: Double,
        moisturePercent: Double,
        standardMoisture: Double = STANDARD_MOISTURE
    ): Double {
        if (moisturePercent <= 0.0 || moisturePercent >= 100.0) return rawWeight
        return rawWeight * (100.0 - moisturePercent) / (100.0 - standardMoisture)
    }

    /**
     * Khối lượng thực = (Thô - Bao bì - Tạp chất) rồi quy đổi theo độ ẩm.
     */
    fun calcNetWeight(
        rawWeight: Double,
        bagWeight: Double,
        impurityWeight: Double,
        moisturePercent: Double
    ): Double {
        val gross = rawWeight - bagWeight - impurityWeight
        if (gross <= 0.0) return 0.0
        return calcStandardWeight(gross, moisturePercent)
    }

    /** Thành tiền = Khối lượng thực × Đơn giá */
    fun calcTotalAmount(netWeight: Double, pricePerKg: Double): Double {
        return netWeight * pricePerKg
    }

    /** Còn lại phải trả = Thành tiền - Đã trả - Tiền cọc */
    fun calcRemainingAmount(
        totalAmount: Double,
        paidAmount: Double,
        depositAmount: Double
    ): Double {
        return totalAmount - paidAmount - depositAmount
    }

    /**
     * Tạo QR token xác thực — hash SHA-256 của dữ liệu giao dịch.
     * Token dùng 32 ký tự hex, có time-bucket 1 phút để tránh replay.
     */
    fun generateQrToken(
        cardId: Long,
        netWeight: Double,
        totalAmount: Double
    ): String {
        val raw = "$cardId|${"%.2f".format(netWeight)}|${"%.0f".format(totalAmount)}|${System.currentTimeMillis() / 60000}"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }
}

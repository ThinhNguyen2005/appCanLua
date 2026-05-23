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

    /**
     * Tính tổng trọng lượng bao bì theo cách user đã chọn cho phiếu.
     *
     * - Cách A (default, `methodIsSampling=false`):
     *   `totalBag = bagCount × bagWeight`
     *   User nhập trọng lượng 1 bao đơn vị, nhân với số bao.
     *
     * - Cách B (mẫu, `methodIsSampling=true`):
     *   `totalBag = (sampleTotalWeight / sampleCount) × bagCount`
     *   User cân `sampleCount` bao mẫu → ra `sampleTotalWeight` kg → suy ra unit weight.
     *   Khi `sampleCount <= 0` fallback về Cách A để tránh chia 0.
     */
    fun calcTotalBagWeight(
        bagCount: Int,
        bagWeight: Double,
        methodIsSampling: Boolean,
        sampleCount: Int,
        sampleTotalWeight: Double
    ): Double {
        return if (methodIsSampling && sampleCount > 0) {
            (sampleTotalWeight / sampleCount) * bagCount
        } else {
            bagCount * bagWeight
        }
    }

    /**
     * Tính tổng kg tạp chất theo cách user đã chọn.
     *
     * - kg (default, `isPercent=false`): coi `impurityValue` là số kg tuyệt đối.
     * - % (`isPercent=true`): coi `impurityValue` là tỉ lệ % trên `rawAfterBag`
     *   (phần lúa thật sau khi đã trừ bao bì) → `impurityKg = rawAfterBag × value/100`.
     */
    fun calcTotalImpurity(
        rawAfterBag: Double,
        impurityValue: Double,
        isPercent: Boolean
    ): Double {
        return if (isPercent) {
            rawAfterBag * (impurityValue / 100.0)
        } else {
            impurityValue
        }
    }

    /**
     * Tính KL thực có ý thức về mode — wrapper bao trùm cả 2 quyết định mode
     * (bao bì A/B, tạp kg/%). Dùng trong CardRepository khi recalc per-card.
     */
    fun calcNetWeightWithModes(
        totalRaw: Double,
        bagCount: Int,
        bagWeight: Double,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        impurityValue: Double,
        impurityIsPercent: Boolean,
        moisturePercent: Double
    ): Double {
        val totalBag = calcTotalBagWeight(
            bagCount = bagCount,
            bagWeight = bagWeight,
            methodIsSampling = bagMethodIsSampling,
            sampleCount = bagSampleCount,
            sampleTotalWeight = bagSampleTotalWeight
        )
        val rawAfterBag = (totalRaw - totalBag).coerceAtLeast(0.0)
        val totalImpurity = calcTotalImpurity(rawAfterBag, impurityValue, impurityIsPercent)
        val gross = (rawAfterBag - totalImpurity).coerceAtLeast(0.0)
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

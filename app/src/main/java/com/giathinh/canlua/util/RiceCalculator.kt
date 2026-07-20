package com.giathinh.canlua.util

/**
 * Công cụ tính toán chuẩn ngành lúa gạo.
 * - Quy đổi khối lượng theo độ ẩm (standard 14%)
 * - Khối lượng thực (trừ bao bì + tạp chất)
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
        val net = calcStandardWeight(gross, moisturePercent)
        return (Math.round(net * 10.0) / 10.0).coerceAtLeast(0.0)
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
     * Tạp chất luôn là kg trực tiếp. `isPercent` chỉ còn là tham số legacy để
     * giữ tương thích dữ liệu/caller cũ; UI mới không còn cho chọn theo %.
     */
    fun calcTotalImpurity(
        rawAfterBag: Double,
        impurityValue: Double,
        isPercent: Boolean
    ): Double {
        val value = impurityValue.coerceAtLeast(0.0)
        return if (isPercent) rawAfterBag.coerceAtLeast(0.0) * value / 100.0 else value
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
        val net = calcStandardWeight(gross, moisturePercent)
        return (Math.round(net * 10.0) / 10.0).coerceAtLeast(0.0)
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

}

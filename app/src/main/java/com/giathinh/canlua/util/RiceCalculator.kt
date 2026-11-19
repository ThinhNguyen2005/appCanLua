package com.giathinh.canlua.util

/**
 * Adapter tính toán chuẩn ngành lúa gạo.
 * Ủy quyền (delegate) tính toán sang [RiceSettlement] để bảo đảm Single Source of Truth,
 * triệt tiêu sai số làm tròn giữa UI preview và Database.
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
     * Ủy quyền sang [RiceSettlement.compute] để đồng nhất với Database.
     */
    fun calcNetWeight(
        rawWeight: Double,
        bagWeight: Double,
        impurityWeight: Double,
        moisturePercent: Double
    ): Double {
        if (rawWeight <= 0.0) return 0.0
        val gross = rawWeight - bagWeight - impurityWeight
        if (gross <= 0.0) return 0.0
        val input = RiceSettlement.Input(
            entryWeightsKg = listOf(rawWeight),
            bagWeightKg = bagWeight,
            impurityValue = impurityWeight,
            moisturePercent = moisturePercent
        )
        return RiceSettlement.compute(input).totalNetKg
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
     * Tính tổng trọng lượng tạp chất (kg) theo chế độ đã chọn:
     *
     * - `isPercent = false`: `impurityValue` là số kg tạp chất cố định.
     * - `isPercent = true`: `impurityValue` là tỷ lệ `%` trên khối lượng sau khi trừ bì
     *   (`rawAfterBag * impurityValue / 100.0`).
     *
     * Giá trị âm được chuẩn hóa về `0.0`.
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
     * (bao bì A/B, tạp kg/%). Ủy quyền sang [RiceSettlement.compute].
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
        if (totalRaw <= 0.0) return 0.0
        val entries = if (bagCount > 1) {
            val avg = totalRaw / bagCount
            List(bagCount) { avg }
        } else {
            listOf(totalRaw)
        }
        val input = RiceSettlement.Input(
            entryWeightsKg = entries,
            bagWeightKg = bagWeight,
            bagMethodIsSampling = bagMethodIsSampling,
            bagSampleCount = bagSampleCount,
            bagSampleTotalWeightKg = bagSampleTotalWeight,
            impurityValue = impurityValue,
            impurityIsPercent = impurityIsPercent,
            moisturePercent = moisturePercent
        )
        return RiceSettlement.compute(input).totalNetKg
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

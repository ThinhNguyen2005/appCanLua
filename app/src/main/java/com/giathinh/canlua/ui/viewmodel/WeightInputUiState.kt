package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.util.RiceCalculator

data class WeightInputUiState(
    val currentWeight: String = "",
    val bagWeight: String = "0",
    val impurityWeight: String = "0",
    val isLocked: Boolean = false,
    val moisturePercent: Double = 0.0
) {
    val totalWeight: Double
        get() = currentWeight.toDoubleOrNull() ?: 0.0

    val bagWeightValue: Double
        get() = bagWeight.toDoubleOrNull() ?: 0.0

    val impurityWeightValue: Double
        get() = impurityWeight.toDoubleOrNull() ?: 0.0

    /**
     * Khối lượng thực tế (kg) sau khi trừ bao bì, tạp chất và quy đổi độ ẩm
     * về tiêu chuẩn 14%.
     *
     * Dùng [RiceCalculator.calcNetWeight] để nhất quán với cách tính tại
     * [WeightInputScreen] (`liveNetWeight`) và Repository (`updateCardCalculations`).
     */
    val netWeight: Double
        get() = RiceCalculator.calcNetWeight(
            rawWeight = totalWeight,
            bagWeight = bagWeightValue,
            impurityWeight = impurityWeightValue,
            moisturePercent = moisturePercent
        )
}

package com.GiaThinh.canlua.ui.viewmodel

data class WeightInputUiState(
    val currentWeight: String = "",
    val bagWeight: String = "0",
    val impurityWeight: String = "0",
    val isLocked: Boolean = false
) {
    val totalWeight: Double
        get() = currentWeight.toDoubleOrNull() ?: 0.0
    
    val bagWeightValue: Double
        get() = bagWeight.toDoubleOrNull() ?: 0.0
    
    val impurityWeightValue: Double
        get() = impurityWeight.toDoubleOrNull() ?: 0.0
    
    val netWeight: Double
        get() = totalWeight - bagWeightValue - impurityWeightValue
}


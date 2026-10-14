package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class TraderBidUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * TraderBidsViewModel — TRADER role removed.
 *
 * Price submission is now handled externally:
 *   Google Sheets → Apps Script → Supabase (admin only).
 *
 * This ViewModel is kept as a stub to avoid breaking any remaining
 * UI references during the transition. Remove it once MarketScreen
 * is cleaned up from TRADER-specific UI (bid editor, my-bids tab).
 */
@HiltViewModel
class TraderBidsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TraderBidUiState())
    val uiState: StateFlow<TraderBidUiState> = _uiState.asStateFlow()

    // Stub — no longer supported. Prices are managed via GAS → Supabase.
    @Suppress("UNUSED_PARAMETER")
    fun submitBid(
        variety: String,
        priceMin: Double,
        priceMax: Double,
        region: String,
        trend: String,
        note: String,
        riceType: String = "lúa Khô",
        existingId: String? = null
    ) {
        _uiState.value = TraderBidUiState(
            errorMessage = "Chức năng đăng giá đã được chuyển sang hệ thống quản lý."
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteBid(bidId: String) {
        _uiState.value = TraderBidUiState(
            errorMessage = "Chức năng xóa giá đã được chuyển sang hệ thống quản lý."
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

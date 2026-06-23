package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
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
class TraderBidsViewModel @Inject constructor(
    marketRepository: MarketRepository
) : ViewModel() {

    /** Market prices (read-only from Room/Supabase cache). */
    val marketPrices: StateFlow<List<RicePrice>> =
        marketRepository.getAllPrices()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _uiState = MutableStateFlow(TraderBidUiState())
    val uiState: StateFlow<TraderBidUiState> = _uiState.asStateFlow()

    // Stub — no longer supported. Prices are managed via GAS → Supabase.
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

    fun deleteBid(bidId: String) {
        _uiState.value = TraderBidUiState(
            errorMessage = "Chức năng xóa giá đã được chuyển sang hệ thống quản lý."
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.giathinh.canlua.repository.MarketRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
 * Exposes the signed-in trader's real bids from Room via [MarketRepository] while
 * write operations remain disabled pending cleanup of TRADER-specific Market UI.
 */
@HiltViewModel
class TraderBidsViewModel @Inject constructor(
    marketRepository: MarketRepository,
    auth: FirebaseAuth
) : ViewModel() {

    val myBids: StateFlow<List<FirestoreRicePrice>> =
        marketRepository.observeMyBids(auth.currentUser?.uid)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

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

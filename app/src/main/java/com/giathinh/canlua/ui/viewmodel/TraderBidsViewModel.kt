package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.giathinh.canlua.repository.MarketRepository
import com.giathinh.canlua.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TraderBidUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TraderBidsViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val myBids: StateFlow<List<FirestoreRicePrice>> = marketRepository.observeMyBids()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Bảng giá thị trường — Room đã merge mock + Firestore của tất cả thương lái. */
    val marketPrices: StateFlow<List<com.giathinh.canlua.data.model.RicePrice>> =
        marketRepository.getAllPrices()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val profile = profileRepository.latestProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(TraderBidUiState())
    val uiState: StateFlow<TraderBidUiState> = _uiState.asStateFlow()

    private var syncJob: kotlinx.coroutines.Job? = null

    init {
        syncBids()
    }

    fun syncBids() {
        if (syncJob == null || syncJob?.isActive == false) {
            syncJob = viewModelScope.launch {
                marketRepository.refreshFromFirestore()
            }
        }
    }

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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val p = profile.value
            val result = marketRepository.submitBid(
                variety = variety.trim(),
                priceMin = priceMin,
                priceMax = priceMax,
                region = region.trim().ifEmpty { p?.region.orEmpty() },
                trend = trend,
                traderName = p?.name.orEmpty(),
                traderPhone = p?.phone.orEmpty(),
                note = note.trim(),
                riceType = riceType,
                existingId = existingId
            )
            _uiState.value = if (result.isSuccess) {
                TraderBidUiState(successMessage = "Đã đăng giá thành công")
            } else {
                TraderBidUiState(errorMessage = result.exceptionOrNull()?.message ?: "Lỗi không xác định")
            }
        }
    }

    fun deleteBid(bidId: String) {
        viewModelScope.launch {
            val result = marketRepository.deleteBid(bidId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Không thể xoá"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

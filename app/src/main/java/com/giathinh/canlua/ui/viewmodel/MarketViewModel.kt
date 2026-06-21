package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.PricePoint
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketFilter(
    val variety: String? = null,    // null = tất cả
    val region: String? = null,
    val trend: String? = null       // UP / DOWN / STABLE
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _selectedVariety = MutableStateFlow<String?>(null)
    private val _timeRangeDays = MutableStateFlow(7)
    private val _isLoading = MutableStateFlow(true)
    private val _filter = MutableStateFlow(MarketFilter())
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    /** Tất cả prices từ Room (đã merge mock + Firestore) */
    private val allPrices: StateFlow<List<RicePrice>> = marketRepository.getAllPrices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Sau khi apply filter */
    val prices: StateFlow<List<RicePrice>> = combine(allPrices, _filter) { all, filter ->
        all.filter { p ->
            (filter.variety == null || p.variety.contains(filter.variety, ignoreCase = true)) &&
                (filter.region == null || p.region.contains(filter.region, ignoreCase = true)) &&
                (filter.trend == null || p.trend == filter.trend)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val selectedVariety: StateFlow<String?> = _selectedVariety.asStateFlow()
    val timeRangeDays: StateFlow<Int> = _timeRangeDays.asStateFlow()
    val filter: StateFlow<MarketFilter> = _filter.asStateFlow()

    // H-05: combine để history tự động cập nhật khi đổi cả variety lẵn timeRange
    // thay vì hack `_selectedVariety.value = _selectedVariety.value` trong setTimeRange.
    val history: StateFlow<List<PricePoint>> = combine(_selectedVariety, _timeRangeDays) { variety, days ->
        variety to days
    }.flatMapLatest { (variety, days) ->
        if (variety == null) flowOf(emptyList())
        else marketRepository.getHistory(variety, days)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var seedJob: kotlinx.coroutines.Job? = null

    init {
        seedIfNeededDeferred()
    }

    fun seedIfNeededDeferred() {
        if (seedJob == null || seedJob?.isActive == false) {
            seedJob = viewModelScope.launch {
                try {
                    marketRepository.seedMockDataIfEmpty()
                    // TỰ ĐỘNG TẢI DỮ LIỆU THẬT NGAY LÚC KHỞI ĐỘNG
                    refreshFromFirestore()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * One-shot refresh từ Firestore — gọi khi user mở tab Market hoặc pull-to-refresh.
     */
    fun refreshFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            _refreshError.value = null
            
            val result = marketRepository.refreshFromFirestore(forceRefresh = true)
            result.fold(
                onSuccess = {
                    android.util.Log.d("MarketVM", "Tải dữ liệu từ Firestore thành công!")
                },
                onFailure = { error ->
                    android.util.Log.e("MarketVM", "Lỗi tải Firestore: ${error.message}", error)
                    _refreshError.value = "Không thể tải giá mới: ${error.message ?: "Lỗi kết nối Firebase"}"
                }
            )
            _isLoading.value = false
        }
    }

    fun clearRefreshError() {
        _refreshError.value = null
    }

    fun selectVariety(variety: String?) {
        _selectedVariety.value = variety
    }

    fun setTimeRange(days: Int) {
        _timeRangeDays.value = days
        // combine(_selectedVariety, _timeRangeDays) tự động phát lại history — không cần hack nữa.
    }

    fun setTrendFilter(trend: String?) {
        _filter.value = _filter.value.copy(trend = trend)
    }

    fun setVarietyFilter(variety: String?) {
        _filter.value = _filter.value.copy(variety = variety)
    }

    fun clearFilters() {
        _filter.value = MarketFilter()
    }
}

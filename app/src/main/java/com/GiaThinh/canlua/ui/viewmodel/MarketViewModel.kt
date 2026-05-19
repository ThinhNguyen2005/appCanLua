package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.PricePoint
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.repository.MarketRepository
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

    val history: StateFlow<List<PricePoint>> = _selectedVariety
        .flatMapLatest { variety ->
            if (variety == null) flowOf(emptyList())
            else marketRepository.getHistory(variety, _timeRangeDays.value)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        seedIfNeeded()
        marketRepository.startFirestoreSync()
    }

    override fun onCleared() {
        marketRepository.stopFirestoreSync()
        super.onCleared()
    }

    private fun seedIfNeeded() {
        viewModelScope.launch {
            try {
                marketRepository.seedMockDataIfEmpty()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectVariety(variety: String?) {
        _selectedVariety.value = variety
    }

    fun setTimeRange(days: Int) {
        _timeRangeDays.value = days
        _selectedVariety.value = _selectedVariety.value
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

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                marketRepository.seedMockDataIfEmpty()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

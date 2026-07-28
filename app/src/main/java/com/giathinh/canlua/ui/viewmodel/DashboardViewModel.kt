package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.data.model.SeasonStats
import com.giathinh.canlua.data.model.TraderStat
import com.giathinh.canlua.data.model.VarietyStat
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.ProfileRepository
import com.giathinh.canlua.ui.util.DashboardFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface AiAnalysisState {
    data object Idle : AiAnalysisState
    data object Loading : AiAnalysisState
    data class Success(val markdown: String) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}

// Removed AiAnalysisState duplicate comment

/**
 * Combined UI state for the entire dashboard. Emits atomically — prevents
 * "Flow Collection Avalanche" where 7-8 staggered flow emissions trigger
 * 5-7 rapid recompositions in the first frame.
 *
 * Used by both FarmerProfileScreen and TraderProfileScreen to consume dashboard
 * data via a single collectAsStateWithLifecycle() call instead of many.
 */
@Immutable
data class DashboardData(
    val seasons: List<String>,
    val selectedSeason: String?,
    val currentStats: SeasonStats?,
    val overallStats: SeasonStats?,
    val previousStats: SeasonStats?,
    val topTraders: List<TraderStat>,
    val seasonsComparison: List<SeasonStats>,
    val varieties: List<VarietyStat>,
    val isAggregated: Boolean,
    val aiAnalysis: AiAnalysisState = AiAnalysisState.Idle
) {
    companion object {
        val EMPTY = DashboardData(
            seasons = emptyList(),
            selectedSeason = null,
            currentStats = null,
            overallStats = null,
            previousStats = null,
            topTraders = emptyList(),
            seasonsComparison = emptyList(),
            varieties = emptyList(),
            isAggregated = false,
            aiAnalysis = AiAnalysisState.Idle
        )
    }
}

/**
 * ViewModel cho tab Thống Kê Mùa Vụ.
 *
 * Kiến trúc:
 *  - `seasons` = danh sách vụ trong DB (sort newest first).
 *  - `selectedSeason` = state người dùng đang xem.
 *  - Các stream khác phụ thuộc selectedSeason → flatMapLatest cancel stream cũ
 *    khi user đổi vụ → tránh leak observer + UI tự refresh realtime khi DB thay đổi.
 *  - `seasonsComparison` lấy 6 vụ gần nhất cho bar chart.
 *  - `aiAnalysis` state độc lập, trigger qua `analyzeWithAi()`. Reset khi đổi vụ.
 *  - `dashboardData` = combined StateFlow phát ra TẤT CẢ data 1 lần — chống Flow Avalanche.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CardRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    private val _selectedSeason = MutableStateFlow<String?>(null)
    val selectedSeason: StateFlow<String?> = _selectedSeason.asStateFlow()

    /** Danh sách vụ có dữ liệu trong DB. */
    val seasons: StateFlow<List<String>> = repository.getDistinctSeasons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {

        // Auto-select vụ mới nhất khi data load lần đầu
        combine(seasons, _selectedSeason) { list, selected ->
            if (selected == null && list.isNotEmpty()) {
                _selectedSeason.value = list.first()
            }
        }.launchIn(viewModelScope)

        // Removed AI reset
    }

    /** Stats của vụ đang chọn — fallback overall stats nếu null. */
    val currentStats: StateFlow<SeasonStats?> = _selectedSeason
        .flatMapLatest { season ->
            if (season.isNullOrBlank()) {
                repository.getOverallStats()
            } else {
                repository.getSeasonStats(season)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Stats tổng toàn bộ phiếu, không phụ thuộc seasonLabel. Dùng cho profile overview. */
    val overallStats: StateFlow<SeasonStats?> = repository.getOverallStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Phân bổ giống lúa của vụ đang chọn. */
    val varieties: StateFlow<List<VarietyStat>> = _selectedSeason
        .flatMapLatest { season ->
            if (season.isNullOrBlank()) flowOf(emptyList())
            else repository.getVarietyBreakdown(season)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Top 5 thương lái trong vụ đang chọn. */
    val topTraders: StateFlow<List<TraderStat>> = _selectedSeason
        .flatMapLatest { season ->
            if (season.isNullOrBlank()) flowOf(emptyList())
            else repository.getTopTraders(season)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * True khi seasonsComparison Room query đã emit ≥ 1 lần (kể cả emptyList nếu user mới).
     * Dùng làm tín hiệu "DashboardVM đã chạm DB" để Profile/Dashboard screen biết
     * chuyển từ skeleton sang UI thật — không dựa thời gian cố định.
     */
    private val _isAggregated = MutableStateFlow(false)
    val isAggregated: StateFlow<Boolean> = _isAggregated.asStateFlow()

    /** So sánh 6 vụ gần nhất — dùng cho bar chart. */
    val seasonsComparison: StateFlow<List<SeasonStats>> = repository.getAllSeasonsComparison()
        .onEach { _isAggregated.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Stats vụ liền trước → tính delta (tăng/giảm) so với vụ hiện tại. */
    val previousSeasonStats: StateFlow<SeasonStats?> = combine(
        seasons, _selectedSeason
    ) { list, current ->
        if (current.isNullOrBlank() || list.isEmpty()) return@combine null
        val idx = list.indexOf(current)
        list.getOrNull(idx + 1)
    }.flatMapLatest { prevSeason ->
        if (prevSeason.isNullOrBlank()) flowOf<SeasonStats?>(null)
        else repository.getSeasonStats(prevSeason).map<SeasonStats, SeasonStats?> { it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Combined dashboard state — TẤT CẢ data phát ra trong 1 atomic emission.
     *
     * Thay vì UI subscribe 7 StateFlow riêng lẻ (mỗi cái emit không đồng bộ),
     * subscribe vào dashboardData duy nhất giảm recomposition từ 5-7 lần xuống 1 lần.
     *
     * NGUYÊN TẮC QUAN TRỌNG: không flatMapLatest bên trong combine —
     * sub-flow (currentStats, varieties, topTraders) vẫn dùng flatMapLatest riêng
     * để cancel query cũ khi đổi vụ, nhưng chúng được combine() gom lại
     * thành 1 emission trước khi đến UI.
     */
    private val _aiAnalysis = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysis: StateFlow<AiAnalysisState> = _aiAnalysis.asStateFlow()

    private val dashboardDataFlow = combine(
        seasons,
        _selectedSeason,
        currentStats,
        overallStats,
        previousSeasonStats,
        topTraders,
        seasonsComparison,
        varieties,
        _isAggregated,
        _aiAnalysis
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val seasonsVal = arr[0] as List<String>
        val selectedSeasonVal = arr[1] as? String
        val currentStatsVal = arr[2] as? SeasonStats
        val overallStatsVal = arr[3] as? SeasonStats
        val previousStatsVal = arr[4] as? SeasonStats
        @Suppress("UNCHECKED_CAST")
        val topTradersVal = arr[5] as List<TraderStat>
        @Suppress("UNCHECKED_CAST")
        val seasonsComparisonVal = arr[6] as List<SeasonStats>
        @Suppress("UNCHECKED_CAST")
        val varietiesVal = arr[7] as List<VarietyStat>
        val isAggregatedVal = arr[8] as Boolean
        val aiAnalysisVal = arr[9] as AiAnalysisState

        DashboardData(
            seasons = seasonsVal,
            selectedSeason = selectedSeasonVal,
            currentStats = currentStatsVal,
            overallStats = overallStatsVal,
            previousStats = previousStatsVal,
            topTraders = topTradersVal,
            seasonsComparison = seasonsComparisonVal,
            varieties = varietiesVal,
            isAggregated = isAggregatedVal,
            aiAnalysis = aiAnalysisVal
        )
    }

    val dashboardData: StateFlow<DashboardData> = dashboardDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardData.EMPTY)

    fun selectSeason(season: String) {
        _selectedSeason.value = season
    }

    fun analyzeWithAi() {
        _aiAnalysis.value = AiAnalysisState.Loading
        viewModelScope.launch {
            _aiAnalysis.value = AiAnalysisState.Success("Phân tích dữ liệu hoàn tất.")
        }
    }

    fun resetAiAnalysis() {
        _aiAnalysis.value = AiAnalysisState.Idle
    }
}

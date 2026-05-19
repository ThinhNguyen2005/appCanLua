package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.data.model.VarietyStat
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.repository.AiChatRepository
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.ProfileRepository
import com.GiaThinh.canlua.repository.WeatherRepository
import com.GiaThinh.canlua.repository.WeatherState
import com.GiaThinh.canlua.ui.util.DashboardFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State cho AI analysis.
 *  - idle: chưa bấm phân tích
 *  - loading: đang gọi API
 *  - success: có kết quả markdown
 *  - error: API lỗi
 */
sealed class AiAnalysisState {
    object Idle : AiAnalysisState()
    object Loading : AiAnalysisState()
    data class Success(val markdown: String) : AiAnalysisState()
    data class Error(val message: String) : AiAnalysisState()
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CardRepository,
    private val aiChatRepository: AiChatRepository,
    profileRepository: ProfileRepository,
    weatherRepository: WeatherRepository
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

        // Reset AI analysis khi user đổi vụ — tránh hiển thị insights cũ cho vụ mới
        _selectedSeason
            .map { _aiAnalysis.value = AiAnalysisState.Idle }
            .launchIn(viewModelScope)
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

    /** So sánh 6 vụ gần nhất — dùng cho bar chart. */
    val seasonsComparison: StateFlow<List<SeasonStats>> = repository.getAllSeasonsComparison()
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

    // === AI Analysis ===

    private val _aiAnalysis = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysis: StateFlow<AiAnalysisState> = _aiAnalysis.asStateFlow()

    private val profile: StateFlow<Profile?> = profileRepository.latestProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val weather: StateFlow<WeatherInfo?> = weatherRepository.observeWeather()
        .map { st -> if (st is WeatherState.Data) st.info else null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectSeason(season: String) {
        _selectedSeason.value = season
    }

    fun resetAiAnalysis() {
        _aiAnalysis.value = AiAnalysisState.Idle
    }

    /**
     * Trigger AI phân tích vụ đang chọn.
     * Build summary block từ data hiện tại (bao gồm cả comparison + varieties + traders)
     * và gọi AiChatRepository.analyzeSeason().
     */
    fun analyzeWithAi() {
        val stats = currentStats.value ?: return
        if (stats.isEmpty) {
            _aiAnalysis.value = AiAnalysisState.Error("Vụ này chưa có phiếu cân nào để phân tích.")
            return
        }
        if (_aiAnalysis.value is AiAnalysisState.Loading) return

        _aiAnalysis.value = AiAnalysisState.Loading
        viewModelScope.launch {
            val summary = buildSeasonSummary(
                stats = stats,
                previous = previousSeasonStats.value,
                varieties = varieties.value,
                topTraders = topTraders.value,
                comparison = seasonsComparison.value
            )
            val result = aiChatRepository.analyzeSeason(
                seasonSummary = summary,
                profile = profile.value,
                weather = weather.value
            )
            _aiAnalysis.value = if (result.isSuccess) {
                AiAnalysisState.Success(result.getOrNull().orEmpty())
            } else {
                AiAnalysisState.Error(
                    result.exceptionOrNull()?.message ?: "Không thể kết nối AI"
                )
            }
        }
    }

    /** Build context summary block đưa vào AI. Dùng tiếng Việt thuần để AI parse dễ. */
    private fun buildSeasonSummary(
        stats: SeasonStats,
        previous: SeasonStats?,
        varieties: List<VarietyStat>,
        topTraders: List<TraderStat>,
        comparison: List<SeasonStats>
    ): String {
        val sb = StringBuilder()
        sb.append("📊 SỐ LIỆU VỤ ${stats.season}:\n")
        sb.append("- Tổng số phiếu cân: ${stats.cardCount}\n")
        sb.append("- Sản lượng: ${DashboardFormatter.weight(stats.totalNetWeight)}\n")
        sb.append("- Doanh thu: ${DashboardFormatter.moneyFull(stats.totalRevenue)}\n")
        sb.append("- Đã thanh toán: ${DashboardFormatter.moneyFull(stats.totalPaid)}\n")
        sb.append("- Công nợ còn lại: ${DashboardFormatter.moneyFull(stats.totalRemaining)}\n")
        sb.append("- Giá TB: ${DashboardFormatter.moneyFull(stats.avgPricePerKg)}/kg\n")
        if (stats.avgMoisture > 0) {
            sb.append("- Độ ẩm TB: ${DashboardFormatter.percent(stats.avgMoisture)}\n")
        }
        sb.append("- Tổng số bao: ${stats.totalBags}\n")

        // Vụ trước để tính delta
        if (previous != null) {
            sb.append("\n📈 VỤ TRƯỚC (${previous.season}) ĐỂ SO SÁNH:\n")
            sb.append("- Sản lượng: ${DashboardFormatter.weight(previous.totalNetWeight)}\n")
            sb.append("- Doanh thu: ${DashboardFormatter.moneyFull(previous.totalRevenue)}\n")
            sb.append("- Số phiếu: ${previous.cardCount}\n")
            DashboardFormatter.deltaPercent(stats.totalNetWeight, previous.totalNetWeight)?.let {
                sb.append("- Delta sản lượng: ${DashboardFormatter.formatDelta(it)}\n")
            }
            DashboardFormatter.deltaPercent(stats.totalRevenue, previous.totalRevenue)?.let {
                sb.append("- Delta doanh thu: ${DashboardFormatter.formatDelta(it)}\n")
            }
        } else {
            sb.append("\n📈 VỤ TRƯỚC: Không có dữ liệu (đây là vụ đầu tiên).\n")
        }

        // Phân bổ giống lúa
        if (varieties.isNotEmpty()) {
            sb.append("\n🌾 PHÂN BỔ GIỐNG LÚA TRONG VỤ:\n")
            val totalVariety = varieties.sumOf { it.weight }
            varieties.forEach { v ->
                val pct = if (totalVariety > 0) v.weight / totalVariety * 100 else 0.0
                sb.append("- ${v.variety}: ${DashboardFormatter.weight(v.weight)} " +
                        "(${DashboardFormatter.percent(pct)}, ${v.count} phiếu)\n")
            }
        }

        // Top traders
        if (topTraders.isNotEmpty()) {
            sb.append("\n🤝 TOP THƯƠNG LÁI:\n")
            topTraders.take(3).forEachIndexed { idx, t ->
                sb.append("${idx + 1}. ${t.traderName}: " +
                        "${DashboardFormatter.moneyFull(t.revenue)} (${t.deals} phiếu)\n")
            }
        }

        // Lịch sử các vụ gần nhất (≤ 6 vụ)
        if (comparison.size > 1) {
            sb.append("\n📅 LỊCH SỬ ${comparison.size} VỤ GẦN NHẤT:\n")
            comparison.forEach { c ->
                sb.append("- ${c.season}: ${DashboardFormatter.weight(c.totalNetWeight)} · " +
                        "${DashboardFormatter.moneyFull(c.totalRevenue)} · ${c.cardCount} phiếu\n")
            }
        }

        return sb.toString().trimEnd()
    }
}

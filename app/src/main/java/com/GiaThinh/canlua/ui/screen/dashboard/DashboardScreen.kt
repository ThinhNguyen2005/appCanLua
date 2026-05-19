package com.GiaThinh.canlua.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.ui.component.dashboard.AiInsightsCard
import com.GiaThinh.canlua.ui.component.dashboard.ChartMetric
import com.GiaThinh.canlua.ui.component.dashboard.KpiCard
import com.GiaThinh.canlua.ui.component.dashboard.SeasonComparisonBarChart
import com.GiaThinh.canlua.ui.component.dashboard.SeasonSelectorChip
import com.GiaThinh.canlua.ui.component.dashboard.TopTradersCard
import com.GiaThinh.canlua.ui.component.dashboard.VarietyPieChart
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter
import com.GiaThinh.canlua.ui.viewmodel.DashboardViewModel

/**
 * Tab "Thống Kê Mùa Vụ" — Phase 3 MVP.
 *
 * Layout:
 *  1. Hero header gradient với title + subtitle
 *  2. Season selector chips (horizontal scroll)
 *  3. Hero KPIs: Sản lượng + Doanh thu (highlight, có delta vs vụ trước)
 *  4. KPI grid 2x2: Công nợ · Giá TB · Số phiếu · Độ ẩm TB
 *  5. Pie chart phân bổ giống lúa
 *  6. Bar chart so sánh các vụ
 *  7. Top thương lái
 *
 * Empty state hiển thị khi chưa có vụ nào trong DB.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val seasons by viewModel.seasons.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val currentStats by viewModel.currentStats.collectAsState()
    val previousStats by viewModel.previousSeasonStats.collectAsState()
    val varieties by viewModel.varieties.collectAsState()
    val topTraders by viewModel.topTraders.collectAsState()
    val seasonsComparison by viewModel.seasonsComparison.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        if (currentStats?.isEmpty == true && seasons.isEmpty()) {
            EmptyDashboard()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { HeaderSection() }

                if (seasons.isNotEmpty()) {
                    item {
                        SeasonSelectorChip(
                            seasons = seasons,
                            selectedSeason = selectedSeason,
                            onSelect = viewModel::selectSeason
                        )
                    }
                }

                // Hero KPIs
                currentStats?.let { stats ->
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            HeroKpis(stats = stats, previous = previousStats)
                        }
                    }

                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SecondaryKpis(stats = stats)
                        }
                    }
                }

                // Charts
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        VarietyPieChart(items = varieties)
                    }
                }

                // AI Insights — đặt giữa charts để user dễ bấm sau khi xem KPIs + pie
                if (currentStats?.isEmpty == false) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AiInsightsCard(
                                state = aiAnalysis,
                                onAnalyze = viewModel::analyzeWithAi,
                                onReset = viewModel::resetAiAnalysis
                            )
                        }
                    }
                }

                if (seasonsComparison.size >= 1) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SeasonComparisonBarChart(
                                seasons = seasonsComparison,
                                selectedSeason = selectedSeason,
                                metric = ChartMetric.WEIGHT
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TopTradersCard(items = topTraders)
                    }
                }
            }
        }
    }
}

/** Header gradient với title app + subtitle. */
@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.GreenPrimary)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Thống Kê Mùa Vụ",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tổng quan sản lượng, doanh thu và phân bổ theo từng vụ",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/** Hero KPIs row: Sản lượng + Doanh thu, highlight với delta vs vụ trước. */
@Composable
private fun HeroKpis(stats: SeasonStats, previous: SeasonStats?) {
    val weightDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalNetWeight, it.totalNetWeight)
    }
    val revenueDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalRevenue, it.totalRevenue)
    }
    val deltaLabel = previous?.season?.let { "vs $it" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Scale,
            label = "Sản lượng",
            value = DashboardFormatter.weight(stats.totalNetWeight),
            accentColor = AppColors.GreenPrimary,
            deltaPercent = weightDelta,
            deltaLabel = deltaLabel,
            highlight = true
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.AttachMoney,
            label = "Doanh thu",
            value = DashboardFormatter.money(stats.totalRevenue),
            accentColor = Color(0xFFF9A825),
            deltaPercent = revenueDelta,
            deltaLabel = deltaLabel,
            highlight = true
        )
    }
}

/** Grid 2x2: Công nợ · Giá TB · Số phiếu · Độ ẩm TB. */
@Composable
private fun SecondaryKpis(stats: SeasonStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Công nợ còn lại",
                value = DashboardFormatter.money(stats.totalRemaining),
                accentColor = AppColors.Error
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountBalance,
                label = "Đã thanh toán",
                value = DashboardFormatter.money(stats.totalPaid),
                accentColor = AppColors.Success
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Receipt,
                label = "Số phiếu cân",
                value = "${stats.cardCount}",
                accentColor = AppColors.Info
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.WaterDrop,
                label = "Độ ẩm TB",
                value = if (stats.avgMoisture > 0) DashboardFormatter.percent(stats.avgMoisture) else "—",
                accentColor = Color(0xFF1976D2)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Inventory,
                label = "Tổng số bao",
                value = "${stats.totalBags}",
                accentColor = Color(0xFF8D6E63)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AttachMoney,
                label = "Giá TB",
                value = if (stats.avgPricePerKg > 0) "${DashboardFormatter.money(stats.avgPricePerKg)}/kg" else "—",
                accentColor = Color(0xFFF9A825)
            )
        }
    }
}

/** Empty state khi chưa có phiếu nào. */
@Composable
private fun EmptyDashboard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = AppColors.GreenPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Chưa có dữ liệu",
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tạo vài phiếu cân ở tab \"Cân Lúa\" — chọn vụ mùa khi tạo " +
                    "để xem thống kê tổng quan ở đây.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

package com.giathinh.canlua.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.component.dashboard.KpiGrid
import com.giathinh.canlua.ui.component.dashboard.KpiGridItem
import com.giathinh.canlua.ui.component.dashboard.SeasonSelectorChip
import com.giathinh.canlua.ui.component.dashboard.VarietyPieChart
import com.giathinh.canlua.ui.component.dashboard.SeasonComparisonBarChart
import com.giathinh.canlua.ui.component.dashboard.TopTradersCard
import com.giathinh.canlua.ui.component.dashboard.ChartMetric
import com.giathinh.canlua.ui.component.profile.ProfileSectionTitle
import com.giathinh.canlua.ui.component.profile.SecondaryStatsRow
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.giathinh.canlua.ui.viewmodel.DashboardData
import com.giathinh.canlua.ui.viewmodel.DashboardViewModel
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.util.Locale
import com.giathinh.canlua.ui.navigation.BottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    TrackScreenRender("statistics_screen")

    val dash by dashboardViewModel.dashboardData.collectAsStateWithLifecycle(DashboardData.EMPTY)
    val isDataReady = dash.isAggregated

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_season_stats_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    val prevRoute = navController.previousBackStackEntry?.destination?.route
                    val isFromTab = prevRoute == BottomNavItem.SCALE.route ||
                            prevRoute == BottomNavItem.HISTORY.route ||
                            prevRoute == BottomNavItem.STATISTICS.route ||
                            prevRoute == "settings"
                    if (navController.previousBackStackEntry != null && !isFromTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_back),
                                tint = AppColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        containerColor = AppColors.Surface
    ) { paddingValues ->
        if (!isDataReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppColors.GreenPrimary)
            }
        } else {
            StatisticsScreenContent(
                dash = dash,
                dashboardViewModel = dashboardViewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun StatisticsScreenContent(
    dash: DashboardData,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val seasonsWithAll = remember(dash.seasons) {
        if (dash.seasons.isNotEmpty()) {
            listOf("Tất cả các vụ") + dash.seasons
        } else {
            emptyList()
        }
    }

    val stats = dash.currentStats ?: dash.overallStats

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Surface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Thanh chọn vụ mùa
        if (seasonsWithAll.isNotEmpty()) {
            item {
                SeasonSelectorChip(
                    seasons = seasonsWithAll,
                    selectedSeason = if (dash.selectedSeason.isNullOrBlank()) "Tất cả các vụ" else dash.selectedSeason,
                    onSelect = { selected ->
                        val target = if (selected == "Tất cả các vụ") "" else selected
                        dashboardViewModel.selectSeason(target)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Hero Card: Tổng quan Doanh thu & Sản lượng
        stats?.let { s ->
            item {
                HeroSummaryCard(
                    totalNetWeight = s.totalNetWeight,
                    totalRevenue = s.totalRevenue
                )
            }
        }

        // 3. Farmer KPI Grid
        stats?.let { s ->
            item {
                FarmerPrimaryKpiGrid(
                    stats = s,
                    previous = dash.previousStats
                )
            }

            // 4. Secondary Stats Row (Độ ẩm, Tạp chất, khô/ướt)
            item {
                SecondaryStatsRow(
                    avgMoisture = s.avgMoisture,
                    totalImpurity = s.totalImpurity,
                    dryCardCount = s.dryCardCount,
                    wetCardCount = s.wetCardCount
                )
            }
        }

        // 5. Biểu đồ so sánh sản lượng
        if (dash.seasonsComparison.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "So sánh sản lượng các vụ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        SeasonComparisonBarChart(
                            seasons = dash.seasonsComparison,
                            selectedSeason = dash.selectedSeason,
                            metric = ChartMetric.WEIGHT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 6. Biểu đồ tròn phân bổ giống lúa
        if (dash.varieties.isNotEmpty()) {
            item {
                VarietyPieChart(
                    items = dash.varieties,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 7. Top Traders Card
        if (dash.topTraders.isNotEmpty()) {
            item {
                TopTradersCard(
                    items = dash.topTraders,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 8. Bảng thống kê chi tiết các vụ
        if (dash.seasonsComparison.isNotEmpty()) {
            item {
                SeasonSummaryCard(dash = dash)
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    totalNetWeight: Double,
    totalRevenue: Double
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            AppColors.GreenPrimary,
            Color(0xFFF5B041) // Ripe golden color
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tổng sản lượng",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DashboardFormatter.weight(totalNetWeight),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Scale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Doanh thu tạm tính",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DashboardFormatter.money(totalRevenue),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wallet,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonSummaryCard(dash: DashboardData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tóm tắt các vụ mùa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Spacer(Modifier.height(4.dp))
            
            val formatter = remember { NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")) }
            
            dash.seasonsComparison.forEach { seasonStat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = seasonStat.season,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "${formatter.format(seasonStat.totalNetWeight)} kg / ${formatter.format(seasonStat.totalBags)} bao",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Text(
                        text = "${formatter.format(seasonStat.totalRevenue)} đ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmerPrimaryKpiGrid(
    stats: com.giathinh.canlua.data.model.SeasonStats,
    previous: com.giathinh.canlua.data.model.SeasonStats?
) {
    val weightDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalNetWeight, it.totalNetWeight)
    }
    val revenueDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalRevenue, it.totalRevenue)
    }
    val deltaLabelPrefix = stringResource(R.string.profile_delta_vs)
    val deltaLabel = previous?.season?.let { "$deltaLabelPrefix $it" }

    KpiGrid(
        items = listOf(
            KpiGridItem(
                icon = Icons.Outlined.Scale,
                label = stringResource(R.string.profile_kpi_sold_yield),
                value = DashboardFormatter.weight(stats.totalNetWeight),
                accentColor = AppColors.GreenPrimary,
                deltaPercent = weightDelta,
                deltaLabel = deltaLabel,
                highlight = true
            ),
            KpiGridItem(
                icon = Icons.Outlined.Wallet,
                label = stringResource(R.string.profile_stats_revenue),
                value = DashboardFormatter.money(stats.totalRevenue),
                accentColor = Color(0xFFF9A825),
                deltaPercent = revenueDelta,
                deltaLabel = deltaLabel,
                highlight = true
            ),
            KpiGridItem(
                icon = Icons.Outlined.Inventory,
                label = stringResource(R.string.profile_kpi_avg_kg_per_bag),
                value = if (stats.avgKgPerBag > 0) stringResource(
                    R.string.profile_kg_per_bag_value,
                    DashboardFormatter.weight(stats.avgKgPerBag)
                ) else "—",
                accentColor = Color(0xFF8D6E63)
            ),
            KpiGridItem(
                icon = Icons.Outlined.Receipt,
                label = stringResource(R.string.profile_kpi_total_bags),
                value = "${stats.totalBags}",
                accentColor = AppColors.Info
            )
        )
    )
}

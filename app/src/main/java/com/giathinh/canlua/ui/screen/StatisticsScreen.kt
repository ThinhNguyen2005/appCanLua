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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.giathinh.canlua.R
import androidx.navigation.NavController
import com.giathinh.canlua.ui.component.dashboard.KpiGrid
import com.giathinh.canlua.ui.component.dashboard.KpiGridItem
import com.giathinh.canlua.ui.component.profile.ProfileSectionTitle
import com.giathinh.canlua.ui.component.profile.QuickStatsGlassGrid
import com.giathinh.canlua.ui.component.profile.SecondaryStatsRow
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.giathinh.canlua.ui.viewmodel.DashboardData
import com.giathinh.canlua.ui.viewmodel.DashboardViewModel
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.util.Locale
import com.giathinh.canlua.ui.component.TransitionSafeWrapper

@Composable
fun StatisticsScreen(
    navController: NavController
) {
    TrackScreenRender("statistics_screen")
    val dashboardViewModel: DashboardViewModel = hiltViewModel()

    val dash by dashboardViewModel.dashboardData.collectAsStateWithLifecycle(DashboardData.EMPTY)
    val isDataReady = dash.isAggregated

    if (!isDataReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = AppColors.GreenPrimary)
        }
    } else {
        StatisticsScreenContent(
            dash = dash,
            dashboardViewModel = dashboardViewModel
        )
    }
}

@Composable
fun StatisticsScreenContent(
    dash: DashboardData,
    dashboardViewModel: DashboardViewModel
) {
    val lifetimeStats = dash.overallStats
    val lifetimeSeasonCount = dash.seasons.size
    val lifetimeTotalNetWeight = lifetimeStats?.totalNetWeight ?: 0.0
    val lifetimeTotalRevenue = lifetimeStats?.totalRevenue ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                QuickStatsGlassGrid(
                    seasonCount = lifetimeSeasonCount,
                    totalNetWeight = lifetimeTotalNetWeight,
                    totalRevenue = lifetimeTotalRevenue,
                    revenueLabel = stringResource(R.string.profile_stats_revenue)
                )
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileSectionTitle(
                        title = stringResource(R.string.profile_season_stats_title),
                        subtitle = stringResource(R.string.profile_season_stats_subtitle)
                    )
                }
            }

            dash.currentStats?.let { stats ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FarmerPrimaryKpiGrid(
                            stats = stats,
                            previous = dash.previousStats
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SecondaryStatsRow(
                            avgMoisture = stats.avgMoisture,
                            totalImpurity = stats.totalImpurity,
                            dryCardCount = stats.dryCardCount,
                            wetCardCount = stats.wetCardCount
                        )
                    }
                }
            }

            if (dash.seasonsComparison.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SeasonSummaryCard(dash = dash)
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
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tóm tắt vụ mùa",
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

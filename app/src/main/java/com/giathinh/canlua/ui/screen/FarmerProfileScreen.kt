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
import com.giathinh.canlua.ui.component.dashboard.AiInsightsCard
import com.giathinh.canlua.ui.component.dashboard.KpiGrid
import com.giathinh.canlua.ui.component.dashboard.KpiGridItem
import com.giathinh.canlua.ui.component.profile.FarmerProfileSkeleton
import com.giathinh.canlua.ui.component.profile.GradientProfileHeader
import com.giathinh.canlua.ui.component.profile.ProfileSectionTitle
import com.giathinh.canlua.ui.component.profile.QuickStatsGlassGrid
import com.giathinh.canlua.ui.component.profile.SecondaryStatsRow
import com.giathinh.canlua.ui.screen.profile.PremiumStatusCard
import com.giathinh.canlua.ui.screen.profile.PremiumUpsellCard
import com.giathinh.canlua.ui.screen.profile.GuestLoginPromoCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.giathinh.canlua.ui.viewmodel.DashboardData
import com.giathinh.canlua.ui.viewmodel.DashboardViewModel
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.util.TrackScreenRender
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.util.Locale

/**
 * Tab "Tài khoản" cho NÔNG DÂN — phiên bản Premium 2026.
 *
 * Layout 8 tiers (theo phân cấp UX):
 *  0. Gradient Hero Header (không avatar)
 *  1. Quick Stats Glass Grid (3 ô chồng lên header)
 *  2. Season Selector Chips
 *  3. Primary KPI Grid 2x2 (Sản lượng / Doanh thu / KG-bao TB / Số bao)
 *  4. Secondary Stats Pills (Độ ẩm + Tạp chất + Khô/Ướt)
 *  5. Season Comparison Bar Chart
 *  6. AI Crop Insights
 *  7. Top Traders + Trader History
 *  8. Account Operations (Thông tin · Đổi vai trò · Premium · Logout)
 *
 * PERFORMANCE: Tiêu thụ dashboardData thay vì 7 StateFlow riêng lẻ.
 * DashboardViewModel.combine() gom TẤT CẢ data thành 1 atomic emission,
 * chống Flow Avalanche — chỉ 1 recomposition thay vì 5-7.
 */
import com.giathinh.canlua.ui.component.TransitionSafeWrapper

@Composable
fun FarmerProfileScreen(
    navController: NavController
) {
    TrackScreenRender("farmer_profile")
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()

    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val dash by dashboardViewModel.dashboardData.collectAsStateWithLifecycle(DashboardData.EMPTY)
    val isDataReady = profile != null && dash.isAggregated
    val isGuestMode by profileViewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)

    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val isGoogleLoggedIn = remember(firebaseUser) {
        firebaseUser?.providerData?.any { it.providerId == "google.com" } == true
    }
    val googleAvatarUrl = remember(firebaseUser) {
        firebaseUser?.photoUrl?.toString()
    }

    TransitionSafeWrapper(
        isDataReady = isDataReady,
        skeletonContent = { FarmerProfileSkeleton() }
    ) {
        FarmerProfileScreenContent(
            navController = navController,
            profile = profile,
            dash = dash,
            profileViewModel = profileViewModel,
            dashboardViewModel = dashboardViewModel,
            isGoogleLoggedIn = isGoogleLoggedIn,
            googleAvatarUrl = googleAvatarUrl,
            isGuestMode = isGuestMode
        )
    }
}

@Composable
fun FarmerProfileScreenContent(
    navController: NavController,
    profile: com.giathinh.canlua.data.model.Profile?,
    dash: DashboardData,
    profileViewModel: ProfileViewModel,
    dashboardViewModel: DashboardViewModel,
    isGoogleLoggedIn: Boolean,
    googleAvatarUrl: String?,
    isGuestMode: Boolean
) {
    // Lifetime stats cho QuickStatsGlassGrid (tổng tất cả vụ, không lọc theo season chip)
    // Luôn tính giá trị — hiển thị 0 cho tài khoản mới thay vì ẩn hoàn toàn grid
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
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── TIER 0: Gradient Hero Header ───
            item {
                GradientProfileHeader(
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_farmer_default_name),
                    role = profile?.role ?: "FARMER",
                    email = profile?.email.orEmpty(),
                    isGoogleLoggedIn = isGoogleLoggedIn,
                    googleAvatarUrl = googleAvatarUrl
                )
            }

            // ─── Guest Mode Login Card ───
            if (isGuestMode) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GuestLoginPromoCard(onLoginClick = { profileViewModel.disableGuestMode() })
                    }
                }
            }

            // ─── TIER 1: Quick Stats Glass Grid ───
            // Luôn hiển thị (kể cả khi user mới, seasonsComparison rỗng → hiển thị 0)
            item {
                QuickStatsGlassGrid(
                    seasonCount = lifetimeSeasonCount,
                    totalNetWeight = lifetimeTotalNetWeight,
                    totalRevenue = lifetimeTotalRevenue,
                    revenueLabel = stringResource(R.string.profile_stats_revenue)
                )
            }

            // Section title cho phần thống kê mùa vụ
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileSectionTitle(
                        title = stringResource(R.string.profile_season_stats_title),
                        subtitle = stringResource(R.string.profile_season_stats_subtitle)
                    )
                }
            }


            // ─── TIER 3: Primary KPI Grid 2×2 ───
            dash.currentStats?.let { stats ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        FarmerPrimaryKpiGrid(
                            stats = stats,
                            previous = dash.previousStats
                        )
                    }
                }

                // ─── TIER 4: Secondary Stats Pills ───
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

                // ─── TIER 6: AI Crop Insights ───
                if (!stats.isEmpty) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AiInsightsCard(
                                state = dash.aiAnalysis,
                                onAnalyze = dashboardViewModel::analyzeWithAi,
                                onReset = dashboardViewModel::resetAiAnalysis
                            )
                        }
                    }
                }
            }

            // ─── TIER 7: Khối Tóm tắt Vụ mùa (thay thế cho Top thương lái mua) ───
            if (dash.seasonsComparison.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SeasonSummaryCard(dash = dash)
                    }
                }
            }

            // ─── Premium card (active hoặc upsell) ───
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val premiumInfo by com.giathinh.canlua.util.PremiumState.info.collectAsStateWithLifecycle()
                    if (premiumInfo.isActive) {
                        PremiumStatusCard(
                            plan = premiumInfo.plan,
                            sinceMs = premiumInfo.sinceMs,
                            isEarlyAdopter = premiumInfo.isEarlyAdopter,
                            onClick = { navController.navigate("premium") }
                        )
                    } else {
                        PremiumUpsellCard(
                            onClick = { navController.navigate("premium") }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Khối Tóm tắt Vụ mùa
// ─────────────────────────────────────────────────────────────
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
                    // Vế trái: Tên Vụ mùa
                    Text(
                        text = seasonStat.season,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Vế giữa: Số liệu sản lượng dưới dạng "số KG / số bao" với màu xám nhẹ
                    Text(
                        text = "${formatter.format(seasonStat.totalNetWeight)} kg / ${formatter.format(seasonStat.totalBags)} bao",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Vế phải: Số tiền cuối cùng nhận được, in đậm và dùng màu xanh lá
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

// ─────────────────────────────────────────────────────────────
// Primary KPI Grid (Farmer): Sản lượng / Doanh thu / KG-bao / Số bao
// ─────────────────────────────────────────────────────────────
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

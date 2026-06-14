package com.GiaThinh.canlua.ui.screen.trader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.R
import androidx.navigation.NavController
import com.GiaThinh.canlua.ui.component.dashboard.AiInsightsCard
import com.GiaThinh.canlua.ui.component.dashboard.ChartMetric
import com.GiaThinh.canlua.ui.component.dashboard.KpiGrid
import com.GiaThinh.canlua.ui.component.dashboard.KpiGridItem
import com.GiaThinh.canlua.ui.component.dashboard.SeasonComparisonBarChart
import com.GiaThinh.canlua.ui.component.dashboard.SeasonSelectorChip
import com.GiaThinh.canlua.ui.component.dashboard.VarietyPieChart
import com.GiaThinh.canlua.ui.component.profile.GradientProfileHeader
import com.GiaThinh.canlua.ui.component.profile.TraderProfileSkeleton
import com.GiaThinh.canlua.ui.component.profile.ProfileNavigationRow
import com.GiaThinh.canlua.ui.component.profile.ProfileSectionTitle
import com.GiaThinh.canlua.ui.component.profile.QuickStatsGlassGrid
import com.GiaThinh.canlua.ui.component.profile.SecondaryStatsRow
import com.GiaThinh.canlua.ui.screen.profile.PersonalInfoCard
import com.GiaThinh.canlua.ui.screen.profile.PremiumStatusCard
import com.GiaThinh.canlua.ui.screen.profile.PremiumUpsellCard
import com.GiaThinh.canlua.ui.screen.profile.RoleSwitcher
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.GiaThinh.canlua.ui.viewmodel.DashboardData
import com.GiaThinh.canlua.ui.viewmodel.DashboardViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.TraderTransactionsViewModel
import com.GiaThinh.canlua.util.TrackScreenRender

/**
 * Tab "Tài khoản" cho THƯƠNG LÁI — phiên bản Premium 2026.
 *
 * Layout 8 tiers (đối xứng với FarmerProfileScreen):
 *  0. Gradient Hero Header (gold-themed, không avatar)
 *  1. Quick Stats Glass Grid (3 ô: Vụ / Tấn / Đã chi)
 *  2. Season Selector Chips
 *  3. Primary KPI Grid 2x2 (Đã mua / Đã chi / KG-bao TB / Số bao)
 *  4. Secondary Stats Pills (Độ ẩm + Tạp chất + Khô/Ướt)
 *  5. Season Comparison Bar Chart
 *  6. AI Crop Insights
 *  7. Variety Pie Chart (cơ cấu giống lúa thu mua) + Sổ giao dịch
 *  8. Account Operations
 */
import com.GiaThinh.canlua.ui.component.TransitionSafeWrapper

@Composable
fun TraderProfileScreen(
    navController: NavController
) {
    TrackScreenRender("trader_profile")
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val traderTransactionsViewModel: TraderTransactionsViewModel = hiltViewModel()

    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val dash by dashboardViewModel.dashboardData.collectAsStateWithLifecycle(DashboardData.EMPTY)
    val txState by traderTransactionsViewModel.uiState.collectAsStateWithLifecycle()
    val isDataReady = profile != null && dash.isAggregated && !txState.isLoading

    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val isGoogleLoggedIn = remember(firebaseUser) {
        firebaseUser?.providerData?.any { it.providerId == "google.com" } == true
    }
    val googleAvatarUrl = remember(firebaseUser) {
        firebaseUser?.photoUrl?.toString()
    }

    TransitionSafeWrapper(
        isDataReady = isDataReady,
        skeletonContent = { TraderProfileSkeleton() }
    ) {
        TraderProfileScreenContent(
            navController = navController,
            profileViewModel = profileViewModel,
            dashboardViewModel = dashboardViewModel,
            traderTransactionsViewModel = traderTransactionsViewModel,
            isGoogleLoggedIn = isGoogleLoggedIn,
            googleAvatarUrl = googleAvatarUrl
        )
    }
}

@Composable
fun TraderProfileScreenContent(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    dashboardViewModel: DashboardViewModel,
    traderTransactionsViewModel: TraderTransactionsViewModel,
    isGoogleLoggedIn: Boolean,
    googleAvatarUrl: String?
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val traderTransactionsState by traderTransactionsViewModel.uiState.collectAsStateWithLifecycle()

    // Combined flow — 1 recomposition thay vì 7 staggered emissions.
    val dash by dashboardViewModel.dashboardData.collectAsStateWithLifecycle(DashboardData.EMPTY)

    val lifetimeStats = remember(traderTransactionsState) {
        TraderLifetimeStats(
            cardCount = traderTransactionsState.totalCards,
            totalNetWeight = traderTransactionsState.totalNetWeight,
            totalPaid = traderTransactionsState.totalPaid
        )
    }

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
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_trader_default_name),
                    role = profile?.role ?: "TRADER",
                    email = profile?.email.orEmpty(),
                    isGoogleLoggedIn = isGoogleLoggedIn,
                    googleAvatarUrl = googleAvatarUrl
                )
            }

            // ─── TIER 1: Quick Stats Glass Grid ───
            item {
                QuickStatsGlassGrid(
                    seasonCount = lifetimeStats.cardCount,
                    totalNetWeight = lifetimeStats.totalNetWeight,
                    totalRevenue = lifetimeStats.totalPaid,
                    revenueLabel = stringResource(R.string.profile_stats_paid)
                )
            }

            // Section title
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileSectionTitle(
                        title = stringResource(R.string.profile_trader_season_book_title),
                        subtitle = stringResource(R.string.profile_trader_season_book_subtitle)
                    )
                }
            }

            // ─── TIER 2: Season Selector Chips ───
            if (dash.seasons.isNotEmpty()) {
                item {
                    SeasonSelectorChip(
                        seasons = dash.seasons,
                        selectedSeason = dash.selectedSeason,
                        onSelect = dashboardViewModel::selectSeason
                    )
                }
            }

            // ─── TIER 3: Primary KPI Grid 2×2 (Trader-focused) ───
            dash.currentStats?.let { stats ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TraderPrimaryKpiGrid(
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

                // ─── TIER 5: Season Comparison Bar Chart ───
                if (dash.seasonsComparison.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SeasonComparisonBarChart(
                                seasons = dash.seasonsComparison,
                                selectedSeason = dash.selectedSeason,
                                metric = ChartMetric.WEIGHT
                            )
                        }
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

            // ─── TIER 7: Variety Pie Chart (Trader-specific) + Sổ giao dịch ───
            if (dash.varieties.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        VarietyPieChart(items = dash.varieties)
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileNavigationRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = stringResource(R.string.profile_transaction_ledger),
                        subtitle = stringResource(
                            R.string.profile_reconciled_slip_count,
                            traderTransactionsState.totalCards
                        ),
                        onClick = { navController.navigate("trader_transactions") }
                    )
                }
            }

            // ─── TIER 8: Account Operations ───
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val premiumInfo by com.GiaThinh.canlua.util.PremiumState.info.collectAsStateWithLifecycle()
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
// Trader lifetime aggregate
// ─────────────────────────────────────────────────────────────

private data class TraderLifetimeStats(
    val cardCount: Int,
    val totalNetWeight: Double,
    val totalPaid: Double
)

// ─────────────────────────────────────────────────────────────
// Primary KPI Grid (Trader): Đã mua / Đã chi / KG-bao TB / Số bao
// ─────────────────────────────────────────────────────────────

@Composable
private fun TraderPrimaryKpiGrid(
    stats: com.GiaThinh.canlua.data.model.SeasonStats,
    previous: com.GiaThinh.canlua.data.model.SeasonStats?
) {
    val weightDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalNetWeight, it.totalNetWeight)
    }
    val paidDelta = previous?.let {
        DashboardFormatter.deltaPercent(stats.totalPaid, it.totalPaid)
    }
    val deltaLabelPrefix = stringResource(R.string.profile_delta_vs)
    val deltaLabel = previous?.season?.let { "$deltaLabelPrefix $it" }

    KpiGrid(
        items = listOf(
            KpiGridItem(
                icon = Icons.Outlined.Scale,
                label = stringResource(R.string.profile_kpi_purchased_yield),
                value = DashboardFormatter.weight(stats.totalNetWeight),
                accentColor = AppColors.GreenPrimary,
                deltaPercent = weightDelta,
                deltaLabel = deltaLabel,
                highlight = true
            ),
            KpiGridItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = stringResource(R.string.profile_kpi_paid_amount),
                value = DashboardFormatter.money(stats.totalPaid),
                accentColor = Color(0xFFE65100),
                deltaPercent = paidDelta,
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
                icon = Icons.Outlined.AccountBalance,
                label = stringResource(R.string.profile_kpi_supplier_debt),
                value = DashboardFormatter.money(stats.totalRemaining),
                accentColor = AppColors.Error
            ),
            KpiGridItem(
                icon = Icons.Outlined.Receipt,
                label = stringResource(R.string.profile_kpi_slip_count),
                value = "${stats.cardCount}",
                accentColor = AppColors.Info
            ),
            KpiGridItem(
                icon = Icons.Outlined.Inventory,
                label = stringResource(R.string.profile_kpi_total_bags),
                value = "${stats.totalBags}",
                accentColor = Color(0xFF8D6E63)
            )
        )
    )
}

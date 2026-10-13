package com.giathinh.canlua.ui.screen.trader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.component.dashboard.AiInsightsCard
import com.giathinh.canlua.ui.component.dashboard.ChartMetric
import com.giathinh.canlua.ui.component.dashboard.KpiGrid
import com.giathinh.canlua.ui.component.dashboard.KpiGridItem
import com.giathinh.canlua.ui.component.dashboard.SeasonComparisonBarChart
import com.giathinh.canlua.ui.component.dashboard.SeasonSelectorChip
import com.giathinh.canlua.ui.component.dashboard.VarietyPieChart
import com.giathinh.canlua.ui.component.profile.GradientProfileHeader
import com.giathinh.canlua.ui.component.profile.ProfileNavigationRow
import com.giathinh.canlua.ui.component.profile.ProfileSectionTitle
import com.giathinh.canlua.ui.component.profile.QuickStatsGlassGrid
import com.giathinh.canlua.ui.component.profile.SecondaryStatsRow
import com.giathinh.canlua.ui.component.profile.TraderProfileSkeleton
import com.giathinh.canlua.ui.navigation.BottomNavItem
import com.giathinh.canlua.ui.screen.profile.GuestLoginPromoCard
import com.giathinh.canlua.ui.screen.profile.PremiumStatusCard
import com.giathinh.canlua.ui.screen.profile.PremiumUpsellCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.giathinh.canlua.ui.viewmodel.DashboardData
import com.giathinh.canlua.ui.viewmodel.DashboardViewModel
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.ui.viewmodel.TraderTransactionsViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import com.google.firebase.auth.FirebaseAuth

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
    val showSkeleton = profile == null || !dash.isAggregated
    val isGuestMode by profileViewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)

    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val isGoogleLoggedIn = remember(firebaseUser) {
        firebaseUser?.providerData?.any { it.providerId == "google.com" } == true
    }
    val googleAvatarUrl = remember(firebaseUser) {
        firebaseUser?.photoUrl?.toString()
    }

    if (showSkeleton) {
        TraderProfileSkeleton()
    } else {
        TraderProfileScreenContent(
            navController = navController,
            profileViewModel = profileViewModel,
            dashboardViewModel = dashboardViewModel,
            traderTransactionsViewModel = traderTransactionsViewModel,
            isGoogleLoggedIn = isGoogleLoggedIn,
            googleAvatarUrl = googleAvatarUrl,
            isGuestMode = isGuestMode
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
    googleAvatarUrl: String?,
    isGuestMode: Boolean
) {
    val context = LocalContext.current
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

            // ─── Guest Mode Login Card ───
            if (isGuestMode) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GuestLoginPromoCard(onLoginClick = {
                            HapticUtil.tick(context)
                            profileViewModel.disableGuestMode()
                        })
                    }
                }
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
                        onSelect = { season ->
                            HapticUtil.tick(context)
                            dashboardViewModel.selectSeason(season)
                        }
                    )
                }
            }

            // ─── TIER 3: Primary KPI Grid 2×2 / Empty Season Stats ───
            val currentStats = dash.currentStats
            if (currentStats == null || currentStats.isEmpty) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        EmptyTraderSeasonStatsCard(
                            onCreateSlip = {
                                HapticUtil.tick(context)
                                navController.navigate(BottomNavItem.SCALE.route)
                            }
                        )
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TraderPrimaryKpiGrid(
                            stats = currentStats,
                            previous = dash.previousStats
                        )
                    }
                }

                // ─── TIER 4: Secondary Stats Pills ───
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SecondaryStatsRow(
                            avgMoisture = currentStats.avgMoisture,
                            totalImpurity = currentStats.totalImpurity,
                            dryCardCount = currentStats.dryCardCount,
                            wetCardCount = currentStats.wetCardCount
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
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AiInsightsCard(
                            state = dash.aiAnalysis,
                            onAnalyze = {
                                HapticUtil.tick(context)
                                dashboardViewModel.analyzeWithAi()
                            },
                            onReset = {
                                HapticUtil.tick(context)
                                dashboardViewModel.resetAiAnalysis()
                            }
                        )
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
                        onClick = {
                            HapticUtil.tick(context)
                            navController.navigate("trader_transactions")
                        }
                    )
                }
            }

            // ─── TIER 8: Account Operations ───
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val premiumInfo by com.giathinh.canlua.util.PremiumState.info.collectAsStateWithLifecycle()
                    if (premiumInfo.isActive) {
                        PremiumStatusCard(
                            plan = premiumInfo.plan,
                            sinceMs = premiumInfo.sinceMs,
                            isEarlyAdopter = premiumInfo.isEarlyAdopter,
                            onClick = {
                                HapticUtil.tick(context)
                                navController.navigate("premium")
                            }
                        )
                    } else {
                        PremiumUpsellCard(
                            onClick = {
                                HapticUtil.tick(context)
                                navController.navigate("premium")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Khối Empty Season Stats cho Thương Lái khi chưa có phiếu thu mua
// ─────────────────────────────────────────────────────────────
@Composable
private fun EmptyTraderSeasonStatsCard(onCreateSlip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Scale,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = stringResource(R.string.profile_empty_season_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.profile_empty_trader_season_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onCreateSlip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
            ) {
                Text(
                    text = stringResource(R.string.profile_empty_season_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
    stats: com.giathinh.canlua.data.model.SeasonStats,
    previous: com.giathinh.canlua.data.model.SeasonStats?
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

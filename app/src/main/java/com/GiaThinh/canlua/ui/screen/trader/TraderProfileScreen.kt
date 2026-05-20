package com.GiaThinh.canlua.ui.screen.trader

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.ui.component.dashboard.AiInsightsCard
import com.GiaThinh.canlua.ui.component.dashboard.ChartMetric
import com.GiaThinh.canlua.ui.component.dashboard.KpiCard
import com.GiaThinh.canlua.ui.component.dashboard.SeasonComparisonBarChart
import com.GiaThinh.canlua.ui.component.dashboard.SeasonSelectorChip
import com.GiaThinh.canlua.ui.component.dashboard.VarietyPieChart
import com.GiaThinh.canlua.ui.component.profile.GradientProfileHeader
import com.GiaThinh.canlua.ui.component.profile.QuickStatsGlassGrid
import com.GiaThinh.canlua.ui.component.profile.SecondaryStatsRow
import com.GiaThinh.canlua.ui.screen.profile.PersonalInfoCard
import com.GiaThinh.canlua.ui.screen.profile.PremiumStatusCard
import com.GiaThinh.canlua.ui.screen.profile.PremiumUpsellCard
import com.GiaThinh.canlua.ui.screen.profile.RoleSwitcher
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import com.GiaThinh.canlua.ui.viewmodel.DashboardViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel

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
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    cardViewModel: CardViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsState(initial = null)
    val cards by cardViewModel.cards.collectAsState()

    // Dashboard data
    val seasons by dashboardViewModel.seasons.collectAsState()
    val selectedSeason by dashboardViewModel.selectedSeason.collectAsState()
    val currentStats by dashboardViewModel.currentStats.collectAsState()
    val previousStats by dashboardViewModel.previousSeasonStats.collectAsState()
    val varieties by dashboardViewModel.varieties.collectAsState()
    val seasonsComparison by dashboardViewModel.seasonsComparison.collectAsState()
    val aiAnalysis by dashboardViewModel.aiAnalysis.collectAsState()

    // Lifetime stats — cho Trader: dùng totalPaid (tổng đã chi) thay vì revenue
    val lifetimeStats = remember(seasonsComparison) {
        if (seasonsComparison.isEmpty()) null else TraderLifetimeStats(
            seasonCount = seasonsComparison.size,
            totalNetWeight = seasonsComparison.sumOf { it.totalNetWeight },
            totalPaid = seasonsComparison.sumOf { it.totalPaid }
        )
    }

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var pendingRole by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile?.uid) {
        profile?.let {
            name = it.name
            phone = it.phone
            region = it.region
            cccd = it.cccd
        }
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
                    name = profile?.name?.takeIf { it.isNotBlank() } ?: "Thương lái",
                    role = profile?.role ?: "TRADER",
                    email = profile?.email.orEmpty()
                )
            }

            // ─── TIER 1: Quick Stats Glass Grid ───
            if (lifetimeStats != null) {
                item {
                    QuickStatsGlassGrid(
                        seasonCount = lifetimeStats.seasonCount,
                        totalNetWeight = lifetimeStats.totalNetWeight,
                        totalRevenue = lifetimeStats.totalPaid,
                        revenueLabel = "Đã chi"
                    )
                }
            }

            // Section title
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle(
                        title = "📊 Sổ thu mua mùa vụ",
                        subtitle = "Số liệu chi tiết từng vụ thu mua"
                    )
                }
            }

            // ─── TIER 2: Season Selector Chips ───
            if (seasons.isNotEmpty()) {
                item {
                    SeasonSelectorChip(
                        seasons = seasons,
                        selectedSeason = selectedSeason,
                        onSelect = dashboardViewModel::selectSeason
                    )
                }
            }

            // ─── TIER 3: Primary KPI Grid 2×2 (Trader-focused) ───
            currentStats?.let { stats ->
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TraderPrimaryKpiGrid(
                            stats = stats,
                            previous = previousStats
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
            }

            // ─── TIER 5: Season Comparison Bar Chart ───
            if (seasonsComparison.isNotEmpty()) {
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

            // ─── TIER 6: AI Crop Insights ───
            if (currentStats?.isEmpty == false) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AiInsightsCard(
                            state = aiAnalysis,
                            onAnalyze = dashboardViewModel::analyzeWithAi,
                            onReset = dashboardViewModel::resetAiAnalysis
                        )
                    }
                }
            }

            // ─── TIER 7: Variety Pie Chart (Trader-specific) + Sổ giao dịch ───
            if (varieties.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        VarietyPieChart(items = varieties)
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NavigationRow(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Sổ giao dịch",
                        subtitle = "${cards.size} phiếu đã đối soát",
                        onClick = { navController.navigate("trader_transactions") }
                    )
                }
            }

            // ─── TIER 8: Account Operations ───
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionTitle(
                        title = "⚙️ Tài khoản",
                        subtitle = "Quản lý thông tin và vai trò"
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PersonalInfoCard(
                        editing = editing,
                        name = name, onName = { name = it },
                        phone = phone, onPhone = { phone = it },
                        region = region, onRegion = { region = it },
                        cccd = cccd, onCccd = { cccd = it },
                        onToggleEdit = {
                            if (editing) {
                                profile?.let { current ->
                                    profileViewModel.updateProfile(
                                        current = current,
                                        name = name,
                                        phone = phone,
                                        region = region,
                                        cccd = cccd
                                    )
                                }
                            }
                            editing = !editing
                        }
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RoleSwitcher(
                        currentRole = profile?.role ?: "TRADER",
                        onRequestChange = { newRole -> pendingRole = newRole }
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val premiumInfo by com.GiaThinh.canlua.util.PremiumState.info.collectAsState()
                    if (premiumInfo.isActive) {
                        PremiumStatusCard(
                            plan = premiumInfo.plan,
                            sinceMs = premiumInfo.sinceMs,
                            onClick = { navController.navigate("premium") }
                        )
                    } else {
                        PremiumUpsellCard(
                            onClick = { navController.navigate("premium") }
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { authViewModel.signOut() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Error.copy(alpha = 0.1f),
                            contentColor = AppColors.Error
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Đăng xuất", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirm dialog đổi role
    pendingRole?.let { newRole ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text("Đổi vai trò?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (newRole) {
                        "TRADER" -> "Bạn sẽ chuyển sang giao diện THƯƠNG LÁI với các chức năng đăng giá, sổ giao dịch, bản đồ nguồn cung."
                        else -> "Bạn sẽ quay lại giao diện NÔNG DÂN với các chức năng cân lúa, mùa vụ, AI khuyến nông. Các bảng giá đã đăng vẫn được giữ."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile?.let { current ->
                        profileViewModel.updateProfile(current = current, role = newRole)
                    }
                    pendingRole = null
                }) { Text("Đổi vai trò", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRole = null }) { Text("Hủy") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Trader lifetime aggregate
// ─────────────────────────────────────────────────────────────

private data class TraderLifetimeStats(
    val seasonCount: Int,
    val totalNetWeight: Double,
    val totalPaid: Double
)

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            color = AppColors.TextHint,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

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
    val deltaLabel = previous?.season?.let { "vs $it" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Scale,
                label = "Đã thu mua",
                value = DashboardFormatter.weight(stats.totalNetWeight),
                accentColor = AppColors.GreenPrimary,
                deltaPercent = weightDelta,
                deltaLabel = deltaLabel,
                highlight = true
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Đã chi trả",
                value = DashboardFormatter.money(stats.totalPaid),
                accentColor = Color(0xFFE65100),
                deltaPercent = paidDelta,
                deltaLabel = deltaLabel,
                highlight = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Inventory,
                label = "KG/bao TB",
                value = if (stats.avgKgPerBag > 0)
                    "${DashboardFormatter.weight(stats.avgKgPerBag)}/bao"
                else "—",
                accentColor = Color(0xFF8D6E63)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccountBalance,
                label = "Còn nợ NCC",
                value = DashboardFormatter.money(stats.totalRemaining),
                accentColor = AppColors.Error
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Receipt,
                label = "Số phiếu",
                value = "${stats.cardCount}",
                accentColor = AppColors.Info
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Inventory,
                label = "Tổng bao",
                value = "${stats.totalBags}",
                accentColor = Color(0xFF8D6E63)
            )
        }
    }
}

/**
 * Row điều hướng generic — dùng cho "Sổ giao dịch" và các sub-screen khác.
 */
@Composable
private fun NavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextHint
            )
        }
    }
}

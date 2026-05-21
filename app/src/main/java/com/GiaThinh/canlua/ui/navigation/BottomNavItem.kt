package com.GiaThinh.canlua.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storefront
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.GiaThinh.canlua.R

/**
 * Định nghĩa từng tab trong BottomNavigation.
 *
 * - [icon]: filled khi NOT selected? KHÔNG — đây là icon outlined dùng cho trạng thái idle.
 * - [selectedIcon]: icon filled hiện khi tab được chọn (animation crossfade).
 * - [label]: label tab hiển thị bên cạnh icon khi selected (pill-mode).
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    @param:StringRes val labelRes: Int
) {
    companion object {
        val SCALE = BottomNavItem(
            "scale", Icons.Outlined.Scale, Icons.Filled.Scale, R.string.nav_scale
        )
        val MARKET = BottomNavItem(
            "market",
            Icons.AutoMirrored.Outlined.TrendingUp,
            Icons.AutoMirrored.Filled.TrendingUp,
            R.string.nav_market
        )
        val AI_CHAT = BottomNavItem(
            "ai_chat", Icons.Outlined.SmartToy, Icons.Filled.SmartToy, R.string.nav_ai_chat
        )
        val ACCOUNT = BottomNavItem(
            "account", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_account
        )

        // Tab "Mùa vụ" (DASHBOARD) đã được loại bỏ — toàn bộ KPI / chart / AI insights
        // hiện đặt trong FarmerProfileScreen + TraderProfileScreen (Tier 2-7) để tránh
        // trùng lặp. Component folder `ui/component/dashboard` + DashboardViewModel
        // vẫn giữ để Profile reuse.
        val farmerNavItems = listOf(SCALE, MARKET, AI_CHAT, ACCOUNT)

        // Mirror farmer 5 tabs: SCALE / MARKET / AI_CHAT / MAP / PROFILE.
        // Reuse cùng route key với farmer để chuyển role không reset state vô tội vạ.
        val TRADER_MAP = BottomNavItem(
            "trader_map", Icons.Outlined.Map, Icons.Filled.Map, R.string.nav_map
        )
        val TRADER_PROFILE = BottomNavItem(
            "trader_profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_account
        )

        val traderNavItems = listOf(
            SCALE,           // Cân Lúa — reuse CardListScreen ở trader mode
            MARKET,          // Thị Trường — reuse MarketScreen + FAB đăng giá cho trader
            AI_CHAT,         // Hỏi đáp AI — reuse AiChatScreen, audience đã route theo profile.role
            TRADER_MAP,
            TRADER_PROFILE
        )
    }
}


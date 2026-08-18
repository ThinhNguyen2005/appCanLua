package com.giathinh.canlua.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import com.giathinh.canlua.R

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
        val HISTORY = BottomNavItem(
            "history", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List, R.string.nav_history
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
        val PROFILE = BottomNavItem(
            "profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_profile
        )
        val STATISTICS = BottomNavItem(
            "statistics", Icons.Outlined.BarChart, Icons.Filled.BarChart, R.string.home_quick_statistics
        )
        val SETTINGS = BottomNavItem(
            "settings", Icons.Outlined.Settings, Icons.Filled.Settings, R.string.topbar_settings
        )

        val TRADER_MAP = BottomNavItem(
            "trader_map", Icons.Outlined.Map, Icons.Filled.Map, R.string.nav_map
        )
        val TRADER_PROFILE = BottomNavItem(
            "trader_profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_account
        )

        /** Bản Full (Nông dân): 4 tab [Cân Lúa, Thị Trường, Hỏi đáp AI, Cá nhân] */
        val farmerNavItems: List<BottomNavItem>
            get() = listOf(SCALE, MARKET, AI_CHAT, ACCOUNT)

        /** Bản Full (Thương lái): 5 tab [Cân Lúa, Thị Trường, Hỏi đáp AI, Bản đồ, Cá nhân] */
        val traderNavItems: List<BottomNavItem>
            get() = listOf(SCALE, MARKET, AI_CHAT, TRADER_MAP, TRADER_PROFILE)

        /** Bản Lite (Offline): 4 tab [Cân Lúa, Lịch Sử, Thống Kê, Cài Đặt] */
        val liteNavItems: List<BottomNavItem>
            get() = listOf(SCALE, HISTORY, STATISTICS, SETTINGS)

        val navItems: List<BottomNavItem>
            get() = farmerNavItems
    }
}

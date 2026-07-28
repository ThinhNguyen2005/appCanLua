package com.giathinh.canlua.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
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
            "market", Icons.Outlined.Storefront, Icons.Filled.Storefront, R.string.nav_market
        )
        val AI_CHAT = BottomNavItem(
            "aichat", Icons.Outlined.Psychology, Icons.Filled.Psychology, R.string.nav_ai_chat
        )
        val PROFILE = BottomNavItem(
            "profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_profile
        )
        val STATISTICS = BottomNavItem(
            "statistics", Icons.Outlined.BarChart, Icons.Filled.BarChart, R.string.nav_account
        )
        val SETTINGS = BottomNavItem(
            "settings", Icons.Outlined.Settings, Icons.Filled.Settings, R.string.topbar_settings
        )

        val ACCOUNT = BottomNavItem(
            "account", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_account
        )
        val TRADER_MAP = BottomNavItem(
            "trader_map", Icons.Outlined.Map, Icons.Filled.Map, R.string.nav_trader_map
        )
        val TRADER_PROFILE = BottomNavItem(
            "trader_profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_trader_profile
        )

        val farmerNavItems: List<BottomNavItem>
            get() = listOf(SCALE, MARKET, AI_CHAT, ACCOUNT)

        val traderNavItems: List<BottomNavItem>
            get() = listOf(SCALE, MARKET, AI_CHAT, TRADER_MAP, TRADER_PROFILE)
        val navItems: List<BottomNavItem>
            get() = farmerNavItems
    }
}

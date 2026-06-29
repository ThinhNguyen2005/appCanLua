package com.giathinh.canlua.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.annotation.StringRes
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
        val STATISTICS = BottomNavItem(
            "statistics", Icons.Outlined.BarChart, Icons.Filled.BarChart, R.string.nav_account
        )
        val SETTINGS = BottomNavItem(
            "settings", Icons.Outlined.Settings, Icons.Filled.Settings, R.string.topbar_settings
        )

        val navItems = listOf(SCALE, HISTORY, STATISTICS, SETTINGS)
    }
}

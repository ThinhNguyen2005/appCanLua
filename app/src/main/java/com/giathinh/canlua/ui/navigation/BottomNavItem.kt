package com.giathinh.canlua.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Scale
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
        val ACCOUNT = BottomNavItem(
            "account", Icons.Outlined.BarChart, Icons.Filled.BarChart, R.string.nav_account
        )

        val navItems = listOf(SCALE, ACCOUNT)
    }
}

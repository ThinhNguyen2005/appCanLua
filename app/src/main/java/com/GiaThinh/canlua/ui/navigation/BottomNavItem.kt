package com.GiaThinh.canlua.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    companion object {
        // Fallback or old constants for backward compatibility in AppNavHost temporarily
        val SCALE = BottomNavItem("scale", Icons.Outlined.Scale, "Cân Lúa")
        val MARKET = BottomNavItem("market", Icons.Outlined.TrendingUp, "Thị Trường")
        val AI_CHAT = BottomNavItem("ai_chat", Icons.Outlined.SmartToy, "Hỏi đáp AI")
        val DASHBOARD = BottomNavItem("dashboard", Icons.Outlined.BarChart, "Mùa vụ")

        val farmerNavItems = listOf(
            SCALE, // Nút 1: Cân lúa
            MARKET, // Nút 2: Giá lúa & Thời tiết
            AI_CHAT, // Nút 3: Hỏi đáp AI
            DASHBOARD // Nút 4: Mùa vụ
        )

        val traderNavItems = listOf(
            BottomNavItem("trader_bids", Icons.Outlined.Sell, "Rao mua"), // Nút 1: Rao mua / Đặt giá
            BottomNavItem("trader_transactions", Icons.Outlined.Storefront, "Sổ giao dịch"), // Nút 2: Sổ giao dịch
            BottomNavItem("trader_map", Icons.Outlined.Map, "Nguồn cung"), // Nút 3: Bản đồ nguồn cung
            BottomNavItem("trader_profile", Icons.Outlined.Person, "Cá nhân") // Nút 4: Cá nhân & Uy tín
        )
    }
}

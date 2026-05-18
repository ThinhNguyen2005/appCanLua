package com.GiaThinh.canlua.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.GiaThinh.canlua.ui.screen.CardDetailScreen
import com.GiaThinh.canlua.ui.screen.CardListScreen
import com.GiaThinh.canlua.ui.screen.SettingsScreen
import com.GiaThinh.canlua.ui.screen.SyncStatusScreen
import com.GiaThinh.canlua.ui.screen.WeightInputScreen
import com.GiaThinh.canlua.ui.screen.aichat.AiChatScreen
import com.GiaThinh.canlua.ui.screen.dashboard.DashboardScreen
import com.GiaThinh.canlua.ui.screen.market.MarketScreen
import com.GiaThinh.canlua.ui.screen.qr.QrGenerateScreen
import com.GiaThinh.canlua.ui.screen.qr.QrScanScreen

/**
 * NavHost chính cho app — bao gồm cả 4 tab và các sub-screens.
 * Auth flow nằm riêng ở AuthNavHost (trong MainScreen).
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = BottomNavItem.SCALE.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // === Tab 1: Cân Lúa ===
        composable(BottomNavItem.SCALE.route) {
            CardListScreen(navController = navController)
        }

        composable("card_detail/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull() ?: 0L
            CardDetailScreen(
                cardId = cardId,
                navController = navController
            )
        }

        composable("weight_input/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull() ?: 0L
            WeightInputScreen(
                cardId = cardId,
                navController = navController
            )
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }

        composable("sync_status") {
            SyncStatusScreen(navController = navController)
        }

        composable("qr_generate/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull() ?: 0L
            QrGenerateScreen(cardId = cardId, navController = navController)
        }

        composable("qr_scan") {
            QrScanScreen(navController = navController)
        }

        // === Tab 2: Thị Trường ===
        composable(BottomNavItem.MARKET.route) {
            MarketScreen()
        }

        // === Tab 3: AI Chat ===
        composable(BottomNavItem.AI_CHAT.route) {
            AiChatScreen()
        }

        // === Tab 4: Thống Kê ===
        composable(BottomNavItem.DASHBOARD.route) {
            DashboardScreen()
        }

        // === Tab Thương Lái ===
        composable("trader_bids") {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Tính năng Rao Mua đang được phát triển")
            }
        }
        
        composable("trader_transactions") {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Sổ giao dịch đang được phát triển")
            }
        }
        
        composable("trader_map") {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Bản đồ nguồn cung đang được phát triển")
            }
        }
        
        composable("trader_profile") {
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.Text("Cá nhân và Uy tín đang được phát triển")
            }
        }
    }
}

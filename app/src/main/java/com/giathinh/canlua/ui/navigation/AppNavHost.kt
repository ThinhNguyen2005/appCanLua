package com.giathinh.canlua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.giathinh.canlua.ui.screen.AuthScreen
import com.giathinh.canlua.ui.screen.CardDetailScreen
import com.giathinh.canlua.ui.screen.CardListScreen
import com.giathinh.canlua.ui.screen.DeletedCardsScreen
import com.giathinh.canlua.ui.screen.HomeScreen
import com.giathinh.canlua.ui.screen.ProfileScreen
import com.giathinh.canlua.ui.screen.ProfileSetupScreen
import com.giathinh.canlua.ui.screen.SettingsScreen
import com.giathinh.canlua.ui.screen.StatisticsScreen
import com.giathinh.canlua.ui.screen.SyncStatusScreen
import com.giathinh.canlua.ui.screen.TraderHistoryScreen
import com.giathinh.canlua.ui.screen.WeightInputScreen
import com.giathinh.canlua.ui.screen.aichat.AiChatScreen
import com.giathinh.canlua.ui.screen.map.RiceMapScreen
import com.giathinh.canlua.ui.screen.market.MarketScreen
import com.giathinh.canlua.ui.screen.profile.FeedbackScreen
import com.giathinh.canlua.ui.screen.profile.PremiumScreen

import com.giathinh.canlua.ui.screen.trader.TraderTransactionsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = BottomNavItem.SCALE.route,
    modifier: Modifier = Modifier,
    deeplinkCardId: String? = null,
    onDeeplinkConsumed: () -> Unit = {}
) {
    LaunchedEffect(deeplinkCardId) {
        if (!deeplinkCardId.isNullOrBlank()) {
            navController.navigate("card_detail/$deeplinkCardId")
            onDeeplinkConsumed()
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = FadeScaleEnter,
        exitTransition = FadeScaleExit,
        popEnterTransition = FadeScalePopEnter,
        popExitTransition = FadeScalePopExit
    ) {
        // === Tab 1: Cân Lúa (Danh sách phiếu cân thực địa) ===
        composable(BottomNavItem.SCALE.route) {
            CardListScreen(navController = navController)
        }

        // === Tab 2: Thị Trường ===
        composable(BottomNavItem.MARKET.route) {
            MarketScreen()
        }
        composable("market") {
            MarketScreen()
        }

        // === Tab 3: Hỏi đáp AI ===
        composable(BottomNavItem.AI_CHAT.route) {
            AiChatScreen()
        }
        composable("ai_chat") {
            AiChatScreen()
        }
        composable("aichat") {
            AiChatScreen()
        }

        // === Tab 4 (Farmer): Tài khoản / Cá nhân ===
        composable(BottomNavItem.ACCOUNT.route) {
            ProfileScreen(navController = navController)
        }
        composable("account") {
            ProfileScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }

        // === Tab 4 (Trader): Bản đồ ===
        composable(BottomNavItem.TRADER_MAP.route) {
            RiceMapScreen(navController = navController)
        }
        composable("trader_map") {
            RiceMapScreen(navController = navController)
        }
        composable("rice_map") {
            RiceMapScreen(navController = navController)
        }

        // === Tab 5 (Trader): Hồ sơ Thương lái ===
        composable(BottomNavItem.TRADER_PROFILE.route) {
            ProfileScreen(navController = navController)
        }
        composable("trader_profile") {
            ProfileScreen(navController = navController)
        }

        // === Tab cho bản Lite ===
        composable(BottomNavItem.HISTORY.route) {
            DeletedCardsScreen(navController = navController)
        }
        composable("history") {
            DeletedCardsScreen(navController = navController)
        }

        composable(BottomNavItem.STATISTICS.route) {
            StatisticsScreen(navController = navController)
        }
        composable("statistics") {
            StatisticsScreen(navController = navController)
        }

        composable(BottomNavItem.SETTINGS.route) {
            SettingsScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }

        // === Các Sub-screens ===
        composable("trader_transactions") {
            TraderTransactionsScreen(navController = navController)
        }

        composable("trader_history") {
            TraderHistoryScreen(navController = navController)
        }

        composable("deleted_cards") {
            DeletedCardsScreen(navController = navController)
        }

        composable("sync_status") {
            SyncStatusScreen(navController = navController)
        }

        composable("feedback") {
            FeedbackScreen(navController = navController)
        }

        composable("premium") {
            PremiumScreen(navController = navController)
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

        composable("auth") {
            AuthScreen(
                onSuccess = { navController.popBackStack() },
                onSkipLogin = { navController.popBackStack() }
            )
        }

        composable("profile_setup") {
            ProfileSetupScreen(navController = navController)
        }
    }
}

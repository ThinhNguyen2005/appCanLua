package com.giathinh.canlua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.giathinh.canlua.ui.screen.CardDetailScreen
import com.giathinh.canlua.ui.screen.CardListScreen
import com.giathinh.canlua.ui.screen.DeletedCardsScreen
import com.giathinh.canlua.ui.screen.FarmerProfileScreen
import com.giathinh.canlua.ui.screen.SettingsScreen
import com.giathinh.canlua.ui.screen.SyncStatusScreen
import com.giathinh.canlua.ui.screen.TraderHistoryScreen
import com.giathinh.canlua.ui.screen.WeightInputScreen
import com.giathinh.canlua.ui.screen.RoleRequestScreen
import com.giathinh.canlua.ui.screen.aichat.AiChatScreen
import com.giathinh.canlua.ui.screen.market.MarketScreen
import com.giathinh.canlua.ui.screen.map.RiceMapScreen
import com.giathinh.canlua.ui.screen.profile.PremiumScreen
import com.giathinh.canlua.ui.screen.profile.FeedbackScreen
import com.giathinh.canlua.ui.screen.trader.TraderProfileScreen
import com.giathinh.canlua.ui.screen.trader.TraderTransactionsScreen

/**
 * NavHost chính cho app — bao gồm cả 4 tab và các sub-screens.
 * Auth flow nằm riêng ở AuthNavHost (trong MainScreen).
 */
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
        // === Transition mặc định: Fade + Scale 250ms ===
        // Áp dụng cho tất cả composable bên trong → đồng nhất, tránh lặp code.
        // Có thể override ở từng composable bằng cách truyền enterTransition/exitTransition.
        enterTransition = FadeScaleEnter,
        exitTransition = FadeScaleExit,
        popEnterTransition = FadeScalePopEnter,
        popExitTransition = FadeScalePopExit
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

        // === Tab 2: Thị Trường ===
        composable(BottomNavItem.MARKET.route) {
            MarketScreen()
        }

        // === Tab 3: AI Chat ===
        composable(BottomNavItem.AI_CHAT.route) {
            AiChatScreen()
        }

        // === Tab 4: Tài khoản (FARMER) ===
        composable(BottomNavItem.ACCOUNT.route) {
            RoleAwareProfileWrapper(navController = navController)
        }

        // === Premium upgrade screen — share cho cả farmer & trader ===
        composable("premium") {
            PremiumScreen(navController = navController)
        }



        composable("trader_history") {
            TraderHistoryScreen(navController = navController)
        }

        composable("deleted_cards") {
            DeletedCardsScreen(navController = navController)
        }

        // === Trader-only routes ===
        // SCALE / MARKET / AI_CHAT route được share với farmer (xem ở trên).
        // Các screen tự đọc profile.role để render UI phù hợp.
        composable("trader_map") {
            RiceMapScreen(navController = navController)
        }

        composable("trader_profile") {
            RoleAwareProfileWrapper(navController = navController)
        }

        // Sub-screen của trader_profile (mở qua row "Sổ giao dịch").
        composable("trader_transactions") {
            TraderTransactionsScreen(navController = navController)
        }

        // === Màn yêu cầu nâng cấp Role TRADER ===
        composable("role_request") {
            RoleRequestScreen(navController = navController)
        }

        // === Màn hình gửi/nhận phản hồi ===
        composable("feedback") {
            FeedbackScreen(navController = navController)
        }
    }
}

@Composable
fun RoleAwareProfileWrapper(
    navController: NavHostController,
    profileViewModel: com.giathinh.canlua.ui.viewmodel.ProfileViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    if (profile?.role == "TRADER") {
        TraderProfileScreen(navController = navController)
    } else {
        FarmerProfileScreen(navController = navController)
    }
}

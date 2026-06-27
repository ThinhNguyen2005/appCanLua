package com.giathinh.canlua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.giathinh.canlua.ui.screen.CardDetailScreen
import com.giathinh.canlua.ui.screen.CardListScreen
import com.giathinh.canlua.ui.screen.DeletedCardsScreen
import com.giathinh.canlua.ui.screen.StatisticsScreen
import com.giathinh.canlua.ui.screen.SettingsScreen
import com.giathinh.canlua.ui.screen.WeightInputScreen

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

        composable(BottomNavItem.ACCOUNT.route) {
            StatisticsScreen(navController = navController)
        }

        composable("deleted_cards") {
            DeletedCardsScreen(navController = navController)
        }
    }
}

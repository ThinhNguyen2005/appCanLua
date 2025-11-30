package com.GiaThinh.canlua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.GiaThinh.canlua.ui.screen.CardDetailScreen
import com.GiaThinh.canlua.ui.screen.CardListScreen
import com.GiaThinh.canlua.ui.screen.SettingsScreen
import com.GiaThinh.canlua.ui.screen.SyncStatusScreen
import com.GiaThinh.canlua.ui.screen.WeightInputScreen

@Composable
fun CanLuaNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "card_list"
    ) {
        composable("card_list") {
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
            SyncStatusScreen()
        }
    }
}

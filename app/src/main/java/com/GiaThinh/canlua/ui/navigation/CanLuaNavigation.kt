package com.GiaThinh.canlua.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.GiaThinh.canlua.ui.screen.AuthScreen
import com.GiaThinh.canlua.ui.screen.CardDetailScreen
import com.GiaThinh.canlua.ui.screen.CardListScreen
import com.GiaThinh.canlua.ui.screen.SettingsScreen
import com.GiaThinh.canlua.ui.screen.SyncStatusScreen
import com.GiaThinh.canlua.ui.screen.WeightInputScreen
import com.GiaThinh.canlua.ui.screen.ProfileSetupScreen

@Composable
fun CanLuaNavigation(
    navController: NavHostController,
    isAuthenticated: Boolean = true,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) "card_list" else "login",
        modifier = modifier
    ) {
        composable("login") {
            AuthScreen(
                onSuccess = {
                    navController.navigate("profile_setup") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("profile_setup") {
            ProfileSetupScreen(navController = navController)
        }
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
            SyncStatusScreen(navController = navController)
        }
    }
}

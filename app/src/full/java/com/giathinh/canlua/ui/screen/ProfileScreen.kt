package com.giathinh.canlua.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.ui.screen.trader.TraderProfileScreen
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    navController: NavController
) {
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)

    if (profile?.role == "TRADER") {
        TraderProfileScreen(navController = navController)
    } else {
        FarmerProfileScreen(navController = navController)
    }
}

package com.giathinh.canlua.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.giathinh.canlua.R
import androidx.compose.ui.res.stringResource

enum class UserRole { FARMER, TRADER, STAFF }

@Composable
fun ProfileSetupScreen(navController: NavController, onComplete: () -> Unit = {}) {
    Text(text = stringResource(R.string.app_name))
}
package com.giathinh.canlua.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.giathinh.canlua.R

@Composable
fun ProfileScreen(navController: NavController) {
    Text(text = stringResource(R.string.nav_account))
}
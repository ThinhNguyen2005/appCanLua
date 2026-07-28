package com.giathinh.canlua.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.giathinh.canlua.R

@Composable
fun AuthScreen(onSuccess: () -> Unit, onSkipLogin: () -> Unit) {
    Text(text = stringResource(R.string.app_name))
}
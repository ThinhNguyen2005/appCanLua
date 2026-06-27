package com.giathinh.canlua.util

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

object PerformanceTracker {
    fun setActivity(activity: ComponentActivity) {
        // No-op
    }

    fun clearActivity() {
        // No-op
    }

    fun onScreenChanged(route: String?) {
        // No-op
    }

    fun stopLoadTrace(route: String?) {
        // No-op
    }
}

@Composable
fun TrackScreenRender(screenRoute: String) {
    LaunchedEffect(screenRoute) {
        PerformanceTracker.stopLoadTrace(screenRoute)
    }
}

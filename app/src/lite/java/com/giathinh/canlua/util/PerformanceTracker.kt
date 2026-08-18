package com.giathinh.canlua.util

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

object PerformanceTracker {
    data class FrameMetricsData(
        val totalFrames: Int = 0,
        val slowFrames: Int = 0,
        val frozenFrames: Int = 0
    )

    fun setActivity(activity: ComponentActivity) {}
    fun clearActivity() {}
    fun onScreenChanged(route: String) {}
    fun startScreenTrace(route: String) {}
    fun stopScreenTrace(route: String) {}
    fun startLoadTrace(route: String) {}
    fun stopLoadTrace(route: String) {}
}

@Composable
fun TrackScreenRender(screenRoute: String) {}

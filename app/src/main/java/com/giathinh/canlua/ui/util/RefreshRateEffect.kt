package com.giathinh.canlua.ui.util

import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

private object RefreshRateLock {
    private var count = 0
    fun acquire(w: Window, hz: Float) { count++; w.setRate(hz) }
    fun release(w: Window) { if (--count <= 0) { count = 0; w.setRate(0f) } }
    private fun Window.setRate(hz: Float) {
        attributes = attributes.apply { preferredRefreshRate = hz }
    }
}

@Composable
fun RequestLowRefreshRate(targetHz: Float = 60f) {
    val window = LocalActivity.current?.window
    DisposableEffect(window, targetHz) {
        window?.let { RefreshRateLock.acquire(it, targetHz) }
        onDispose { window?.let(RefreshRateLock::release) }
    }
}
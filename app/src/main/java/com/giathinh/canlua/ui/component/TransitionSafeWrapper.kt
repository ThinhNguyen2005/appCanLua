package com.giathinh.canlua.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Wrapper giúp trì hoãn việc load data/vẽ UI thật trong lúc chạy transition animation.
 *
 * Trong thời gian animation chạy, chỉ render giao diện giả lập nhẹ (skeletonContent)
 * tĩnh để giải phóng CPU cho animation đạt 60fps/120fps. Sau đó mới render content thật.
 *
 * Dùng Crossfade để transition không bị giật đột ngột.
 */
@Composable
fun TransitionSafeWrapper(
    isDataReady: Boolean,
    delayMs: Long = 350L,
    crossfadeDurationMs: Int = 220,
    skeletonContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val isDataReadyAtStart = remember { isDataReady }
    var isTransitionFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isDataReadyAtStart) {
            delay(delayMs)
            isTransitionFinished = true
        }
    }

    val shouldShowContent = isDataReadyAtStart || (isTransitionFinished && isDataReady)

    Crossfade(
        targetState = shouldShowContent,
        animationSpec = tween(durationMillis = crossfadeDurationMs),
        label = "transition_safe_crossfade"
    ) { showContent ->
        if (showContent) {
            content()
        } else {
            skeletonContent()
        }
    }
}

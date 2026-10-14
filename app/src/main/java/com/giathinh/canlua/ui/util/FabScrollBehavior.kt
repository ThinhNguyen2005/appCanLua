package com.giathinh.canlua.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
/**
 * Helpers để FAB tự ẩn khi user scroll xuống, hiện lại khi scroll lên.
 * Pattern Material 3 chuẩn — FAB không che nội dung khi đang đọc.
 */

@Stable
class ScrollDirectionState(private val thresholdPx: Float) : NestedScrollConnection {
    var isScrollingUp by mutableStateOf(true); private set
    private var acc = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy == 0f) return Offset.Zero
        if (acc * dy < 0) acc = 0f
        acc += dy
        if (acc >= thresholdPx) isScrollingUp = true
        else if (acc <= -thresholdPx) isScrollingUp = false
        return Offset.Zero
    }

    fun reset() { acc = 0f; isScrollingUp = true }
}

@Composable
fun rememberScrollDirectionState(threshold: Dp = 16.dp): ScrollDirectionState {
    val px = with(LocalDensity.current) { threshold.toPx() }
    return remember(px) { ScrollDirectionState(px) }
}
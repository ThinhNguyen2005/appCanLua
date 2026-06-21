package com.giathinh.canlua.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * Helpers để FAB tự ẩn khi user scroll xuống, hiện lại khi scroll lên.
 * Pattern Material 3 chuẩn — FAB không che nội dung khi đang đọc.
 */

/**
 * Theo dõi hướng scroll của một LazyColumn / LazyRow.
 * Trả `true` nếu user đang scroll lên (FAB nên hiện), `false` nếu đang scroll xuống.
 *
 * Mặc định: `true` (FAB hiện) khi list đứng yên.
 */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var isScrollingUp by remember(this) { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousIndex = firstVisibleItemIndex
        var previousOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                isScrollingUp = if (index != previousIndex) {
                    index < previousIndex
                } else {
                    offset <= previousOffset
                }
                previousIndex = index
                previousOffset = offset
            }
    }
    return isScrollingUp
}

/**
 * Phiên bản cho [ScrollState] (Column scroll thông thường).
 */
@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var isScrollingUp by remember(this) { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousValue = value
        snapshotFlow { value }
            .collect { current ->
                isScrollingUp = current <= previousValue
                previousValue = current
            }
    }
    return isScrollingUp
}

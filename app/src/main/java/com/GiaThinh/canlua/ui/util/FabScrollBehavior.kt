package com.GiaThinh.canlua.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            val scrollingUp = if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousOffset >= firstVisibleItemScrollOffset
            }
            previousIndex = firstVisibleItemIndex
            previousOffset = firstVisibleItemScrollOffset
            scrollingUp
        }
    }.value
}

/**
 * Phiên bản cho [ScrollState] (Column scroll thông thường).
 */
@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var previousValue by remember(this) { mutableIntStateOf(value) }
    return remember(this) {
        derivedStateOf {
            val scrollingUp = previousValue >= value
            previousValue = value
            scrollingUp
        }
    }.value
}

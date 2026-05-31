package com.GiaThinh.canlua.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Wrapper giúp trì hoãn việc load data/vẽ UI thật trong lúc chạy transition animation (350ms).
 *
 * Trong thời gian animation chạy, chỉ render giao diện giả lập nhẹ (skeletonContent)
 * tĩnh để giải phóng CPU cho animation đạt 60fps/120fps. Sau đó mới render content thật.
 */
@Composable
fun TransitionSafeWrapper(
    isDataReady: Boolean,
    delayMs: Long = 350L,
    skeletonContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    // Ghi nhớ xem dữ liệu đã sẵn sàng từ frame đầu tiên hay chưa
    val isDataReadyAtStart = remember { isDataReady }
    var isTransitionFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isDataReadyAtStart) {
            delay(delayMs)
            isTransitionFinished = true
        }
    }

    // Hiển thị giao diện thật nếu dữ liệu có sẵn từ đầu (warm start)
    // Hoặc khi transition kết thúc VÀ dữ liệu hiện tại đã sẵn sàng.
    val shouldShowContent = isDataReadyAtStart || (isTransitionFinished && isDataReady)

    if (shouldShowContent) {
        content()
    } else {
        skeletonContent()
    }
}

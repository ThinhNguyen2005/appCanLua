package com.GiaThinh.canlua.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

/**
 * Bộ preset transition chuẩn cho NavHost — FADE-ONLY (no scale).
 *
 * v2 (2026-05-19): Bỏ scaleIn/scaleOut vì gây jank trên CardDetailScreen.
 *
 * ROOT CAUSE phân tích:
 *  - scale + fade ép Compose tạo offscreen layer cho mỗi screen → render
 *    toàn bộ content tree vào texture, apply transform matrix mỗi frame.
 *  - CardDetailScreen có LazyColumn + 5 cards + dividers + ripples → texture
 *    lớn → mỗi frame transition copy ~full screen → drop frame trên máy yếu.
 *  - Cộng thêm DetailSkeleton's rememberInfiniteTransition + data swap giữa
 *    transition → 3 animation tranh GPU cùng lúc → cảm giác "không mượt".
 *
 * Fade-only thì sao?
 *  - Không cần offscreen layer cho transform → animate alpha trực tiếp trên
 *    view layer → cực rẻ với GPU compositor.
 *  - Material 3 standard cho navigation: fade là transition mặc định an toàn,
 *    không gây mất phương hướng.
 *  - Duration 220ms: nhanh hơn 250ms tiêu chuẩn để content stabilize sớm.
 */
private const val NAV_DURATION_MS = 220

/** Forward navigation — trang mới xuất hiện. */
val FadeScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing))
}

/** Forward navigation — trang cũ thoát ra. */
val FadeScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing))
}

/** Pop back — trang trước hiện lại. */
val FadeScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing))
}

/** Pop back — trang hiện tại biến mất. */
val FadeScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing))
}

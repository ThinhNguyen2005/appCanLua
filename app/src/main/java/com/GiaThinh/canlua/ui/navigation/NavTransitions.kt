package com.GiaThinh.canlua.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

/**
 * Bộ preset transition chuẩn cho NavHost — Fade + Scale nhẹ.
 *
 * Tại sao chọn pattern này?
 *  - Hiện đại, có chiều sâu (giống Material 3 Container Transform)
 *  - Nhẹ hơn slide horizontal đáng kể: chỉ animate alpha + transform matrix,
 *    GPU compositor xử lý native → ít rớt frame trên máy yếu
 *  - Pop transition tự nhiên (zoom out nhẹ) — không gây mất phương hướng
 *
 * Thông số:
 *  - Duration 250ms (Material 3 standard)
 *  - Scale delta nhỏ 0.04 — đủ để tạo depth, không gây "phồng/xẹp"
 *  - Asymmetric easing: LinearOutSlowIn (vào) / FastOutSlowIn (ra)
 */
private const val NAV_DURATION_MS = 250
private const val SCALE_ENTER_FROM = 0.96f      // Forward push: vào nhỏ → lớn (zoom in)
private const val SCALE_EXIT_TO = 1.04f         // Forward push: ra lớn nhẹ (như đẩy về phía sau)
private const val SCALE_POP_ENTER_FROM = 1.04f  // Pop back: vào lớn → bình thường (zoom out)
private const val SCALE_POP_EXIT_TO = 0.96f     // Pop back: ra nhỏ nhẹ (lùi về sau)

/** Forward navigation — trang mới xuất hiện. */
val FadeScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing)) +
    scaleIn(
        initialScale = SCALE_ENTER_FROM,
        animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing)
    )
}

/** Forward navigation — trang cũ thoát ra. */
val FadeScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing)) +
    scaleOut(
        targetScale = SCALE_EXIT_TO,
        animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing)
    )
}

/** Pop back — trang trước hiện lại. */
val FadeScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing)) +
    scaleIn(
        initialScale = SCALE_POP_ENTER_FROM,
        animationSpec = tween(NAV_DURATION_MS, easing = LinearOutSlowInEasing)
    )
}

/** Pop back — trang hiện tại biến mất (zoom out nhẹ). */
val FadeScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing)) +
    scaleOut(
        targetScale = SCALE_POP_EXIT_TO,
        animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing)
    )
}

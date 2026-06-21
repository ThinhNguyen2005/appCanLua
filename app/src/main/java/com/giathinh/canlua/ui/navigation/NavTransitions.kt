package com.giathinh.canlua.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/**
 * Horizontal slide transitions cho NavHost.
 *
 * v6 (2026-05-26): Tab-aware direction ở CẢ 4 slot.
 *
 * Lý do: Navigation Compose với `popUpTo(start, saveState=true) + restoreState=true`
 * khi quay lại tab cũ → framework dùng popEnter/popExit, không phải enter/exit.
 * v5 chỉ smart trong enter/exit nên Cá nhân → Thị trường (Market đã visit trước đó)
 * vẫn ngược hướng. Fix bằng cách áp dụng smart direction cho cả 4 hook.
 *
 * Logic:
 *  - tab→tab switch (cả 2 route đều trong TAB_ORDER): hướng = sign(toIdx - fromIdx)
 *  - non-tab navigation (sub-screen push): enter/exit dùng forward, popEnter/popExit dùng backward
 *
 * Duration 300ms với FastOutSlowInEasing — M3 motion standard.
 */
private const val NAV_DURATION_MS = 300

private val OFFSET_TWEEN = tween<IntOffset>(
    durationMillis = NAV_DURATION_MS,
    easing = FastOutSlowInEasing
)

/**
 * Thứ tự tab — index càng cao càng "ở bên phải" trong bottom bar.
 * Bao gồm cả farmer (ACCOUNT) và trader (TRADER_MAP, TRADER_PROFILE).
 * User không switch chéo giữa farmer-only ↔ trader-only tab nên không xung đột.
 */
private val TAB_ORDER: List<String> = listOf(
    BottomNavItem.SCALE.route,           // 0
    BottomNavItem.MARKET.route,          // 1
    BottomNavItem.AI_CHAT.route,         // 2
    BottomNavItem.TRADER_MAP.route,      // 3 (trader only)
    BottomNavItem.ACCOUNT.route,         // 4 (farmer profile)
    BottomNavItem.TRADER_PROFILE.route   // 5 (trader profile)
)

/**
 * +1 nếu target nằm BÊN PHẢI source trong bottom bar (forward swipe).
 * -1 nếu BÊN TRÁI (backward swipe).
 *  0 nếu không phải tab→tab switch (1 trong 2 route là sub-screen).
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val fromIdx = TAB_ORDER.indexOf(initialState.destination.route)
    val toIdx = TAB_ORDER.indexOf(targetState.destination.route)
    if (fromIdx < 0 || toIdx < 0) return 0
    return when {
        toIdx > fromIdx -> 1
        toIdx < fromIdx -> -1
        else -> 0
    }
}

/** Slide-in helper. `forward=true` → vào từ PHẢI; false → vào từ TRÁI. */
private fun buildEnter(forward: Boolean): EnterTransition = slideInHorizontally(
    initialOffsetX = { if (forward) it else -it },
    animationSpec = OFFSET_TWEEN
)

/** Slide-out helper. `forward=true` → ra qua TRÁI; false → ra qua PHẢI. */
private fun buildExit(forward: Boolean): ExitTransition = slideOutHorizontally(
    targetOffsetX = { if (forward) -it else it },
    animationSpec = OFFSET_TWEEN
)

/**
 * Forward push (navigate sub-screen). Sub-screen: → (slide from right).
 * Nếu tab→tab: dùng hướng theo TAB_ORDER.
 */
val FadeScaleEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val dir = tabDirection()
    val forward = if (dir != 0) dir > 0 else true
    buildEnter(forward)
}

val FadeScaleExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val dir = tabDirection()
    val forward = if (dir != 0) dir > 0 else true
    buildExit(forward)
}

/**
 * Pop back (popBackStack hoặc restoreState từ saved tab).
 * Sub-screen pop: ← (slide from left).
 * Nếu tab→tab: dùng hướng theo TAB_ORDER (không phụ thuộc framework gọi pop hay push).
 */
val FadeScalePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    val dir = tabDirection()
    val forward = if (dir != 0) dir > 0 else false
    buildEnter(forward)
}

val FadeScalePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    val dir = tabDirection()
    val forward = if (dir != 0) dir > 0 else false
    buildExit(forward)
}

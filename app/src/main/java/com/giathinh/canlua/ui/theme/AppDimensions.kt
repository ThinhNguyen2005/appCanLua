package com.giathinh.canlua.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens cho spacing, corner radius, icon sizes, và avatar sizes.
 * Dùng thay hardcoded dp values trong code để đảm bảo nhất quán.
 *
 * Sử dụng: `AppDimensions.SpacingMd`, `AppDimensions.CornerRadiusSm`, etc.
 */
object AppDimensions {
    // ── Spacing ────────────────────────────────────────────────────────────────
    val SpacingXxs = 2.dp
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 16.dp
    val SpacingLg = 24.dp
    val SpacingXl = 32.dp

    // ── Corner Radius ─────────────────────────────────────────────────────────
    val CornerRadiusSm = 6.dp
    val CornerRadiusMd = 12.dp
    val CornerRadiusLg = 24.dp

    // ── Icon Sizes ────────────────────────────────────────────────────────────
    val IconSm = 16.dp
    val IconMd = 24.dp
    val IconLg = 32.dp

    // ── Avatar Sizes ─────────────────────────────────────────────────────────
    val AvatarSm = 32.dp
    val AvatarMd = 48.dp
    val AvatarLg = 64.dp

    // ── Bottom Navigation ─────────────────────────────────────────────────────
    val BottomBarHeight = 80.dp
}

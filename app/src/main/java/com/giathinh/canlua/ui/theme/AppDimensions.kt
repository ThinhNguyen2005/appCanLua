package com.giathinh.canlua.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens chuẩn trên thang 4dp (4, 8, 12, 16, 20, 24, 32, 48...).
 * Thay thế toàn bộ các số .dp lẻ tẻ trong các màn hình UI.
 */
object AppDimensions {
    // ── Spacing (Bội số của 4dp) ──────────────────────────────────────────────
    val SpacingXxs = 4.dp
    val SpacingXs = 8.dp
    val SpacingSm = 12.dp
    val SpacingMd = 16.dp
    val SpacingLg = 20.dp
    val SpacingXl = 24.dp
    val SpacingXxl = 32.dp

    // ── Corner Radius (1 họ bo góc thống nhất) ────────────────────────────────
    val CornerRadiusSm = 8.dp
    val CornerRadiusMd = 12.dp
    val CornerRadiusLg = 20.dp
    val CornerRadiusXl = 24.dp

    // ── Icon Sizes ────────────────────────────────────────────────────────────
    val IconSm = 18.dp
    val IconMd = 20.dp
    val IconLg = 24.dp
    val IconXl = 40.dp

    // ── Avatar & Touch Targets (Fitts's Law >= 48.dp) ─────────────────────────
    val TouchTargetMin = 48.dp
    val ButtonHeight = 52.dp
    val InputFieldMinHeight = 56.dp
    val FormMaxWidth = 440.dp

    // ── Bottom Navigation ─────────────────────────────────────────────────────
    val BottomBarHeight = 80.dp
}
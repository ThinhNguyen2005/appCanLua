package com.giathinh.canlua.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.giathinh.canlua.data.model.AppThemeMode

/**
 * Xác định chính xác trạng thái Dark Mode cho cả 4 chế độ (LIGHT, HIGH_CONTRAST, OLED, AUTO).
 * Khắc phục lỗi cũ: khi ở chế độ AUTO vào ban ngày (System Light), app không còn bị nhận nhầm là Dark.
 */
internal val isAppInDarkTheme: Boolean
    @Composable get() = when (LocalAppThemeMode.current) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.HIGH_CONTRAST, AppThemeMode.OLED -> true
        AppThemeMode.AUTO -> isSystemInDarkTheme()
    }

object AppColors {
    // Primary — xanh lá lúa
    val GreenPrimary: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.GreenPrimary else Color(0xFF2E7D32),
            label = "GreenPrimary"
        ).value
    val GreenLight = Color(0xFF4CAF50)
    val GreenSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.GreenSurface else Color(0xFFE8F5E9),
            label = "GreenSurface"
        ).value
    val GreenDark = Color(0xFF1B5E20)

    // Accent — vàng lúa chín
    val GoldAccent = Color(0xFFF9A825)
    val GoldLight = Color(0xFFFFF8E1)
    val GoldDark = Color(0xFFE65100)

    // Neutral (Dynamic with animation for Dark/Light mode)
    val Surface: Color
        @Composable get() = animateColorAsState(
            targetValue = MaterialTheme.colorScheme.background,
            label = "Surface"
        ).value

    val CardBg: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.CardBg else Color(0xFFFFFFFF),
            label = "CardBg"
        ).value

    val TextPrimary: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.TextPrimary else Color(0xFF1A1C1A),
            label = "TextPrimary"
        ).value

    val TextSecondary: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.TextSecondary else Color(0xFF424940),
            label = "TextSecondary"
        ).value

    val TextHint: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.TextHint else Color(0xFF727970),
            label = "TextHint"
        ).value

    val Divider: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.Divider else Color(0xFFD0D5CE),
            label = "Divider"
        ).value

    /** Divider đậm hơn cho ngữ cảnh cần phân tách rõ (ngoài trời, độ sáng thấp). */
    val DividerStrong: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.DividerStrong else Color(0xFFB4BCB1),
            label = "DividerStrong"
        ).value

    // Status
    val Success = Color(0xFF43A047)
    val Warning = Color(0xFFFF8F00)
    val Error = Color(0xFFE53935)
    val Info = Color(0xFF1976D2)

    // Extended colors for settings & badges
    val Blue = Color(0xFF1976D2)
    val BlueSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Color(0xFF0D47A1) else Color(0xFFE3F2FD),
            label = "BlueSurface"
        ).value
    val Orange = Color(0xFFFF7043)
    val OrangeSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Color(0xFFE65100) else Color(0xFFFFF3E0),
            label = "OrangeSurface"
        ).value

    // Offline badge
    val OfflineBg = Color(0xFFFFF3E0)
    val OfflineText = Color(0xFFE65100)

    // Locked card
    val LockedBg = Color(0xFFFCE4EC)
    val LockedText = Color(0xFFC62828)

    /** Tint nền card khi phiếu khoá — hồng rất nhạt, không nuốt nội dung. */
    val LockedSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.LockedSurface else Color(0xFFFFF1F3),
            label = "LockedSurface"
        ).value

    /** Tint nền card khi đã trả đủ tiền — xanh rất nhạt. */
    val PaidSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.PaidSurface else Color(0xFFF1F8F2),
            label = "PaidSurface"
        ).value

    // ── Detail screen surfaces (Fluent Tonal Layers) ─────────────────────
    /** Highlight cho dòng "Còn lại" — đỏ đậm light, đỏ mềm dark */
    val RemainingHighlight: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.RemainingHighlight else Color(0xFFB71C1C),
            label = "RemainingHighlight"
        ).value

    /** Surface cho card highlight tổng khối lượng */
    val WeightSurface: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.WeightSurface else Color(0xFFFFF8E1),
            label = "WeightSurface"
        ).value

    /** Surface lighter (sub card depth layer 2) */
    val SurfaceContainer: Color
        @Composable get() = animateColorAsState(
            targetValue = if (isAppInDarkTheme) Dark.SurfaceContainer else Color(0xFFF4F6F3),
            label = "SurfaceContainer"
        ).value

    // Dark theme overrides (Phân cấp rõ TextPrimary vs TextSecondary theo UI Playbook)
    object Dark {
        val Surface = Color(0xFF121212)
        val CardBg = Color(0xFF1E1E1E)
        val SurfaceContainer = Color(0xFF262925)
        val TextPrimary = Color(0xFFE8ECE7)
        val TextSecondary = Color(0xFFB5BDB3)
        val TextHint = Color(0xFF889186)
        val Divider = Color(0xFF383D37)
        val DividerStrong = Color(0xFF525850)
        val LockedSurface = Color(0xFF2A1F22)
        val PaidSurface = Color(0xFF1A2820)
        val GreenPrimary = Color(0xFFA5D6A7)
        val GreenSurface = Color(0xFF1E3A24)
        val RemainingHighlight = Color(0xFFEF5350)
        val WeightSurface = Color(0xFF332B14)
    }
}
package com.GiaThinh.canlua.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.GiaThinh.canlua.data.model.AppThemeMode

/**
 * Design tokens chuẩn Fluent Design cho app Cân Lúa.
 * Bảng màu lấy cảm hứng từ lúa gạo Việt Nam.
 * Dark mode được xác định qua [LocalAppThemeMode] — KHÔNG còn dùng
 * [androidx.compose.foundation.isSystemInDarkTheme] trực tiếp nữa, tránh conflict
 * khi user chọn theme mode không đồng bộ với system preference.
 */
private val isDarkTheme: Boolean
    @Composable get() = LocalAppThemeMode.current != AppThemeMode.LIGHT

object AppColors {
    // Primary — xanh lá lúa
    val GreenPrimary: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.GreenPrimary else Color(0xFF2E7D32), label = "GreenPrimary").value
    val GreenLight = Color(0xFF4CAF50)
    val GreenSurface: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.GreenSurface else Color(0xFFE8F5E9), label = "GreenSurface").value
    val GreenDark = Color(0xFF1B5E20)

    // Accent — vàng lúa chín
    val GoldAccent = Color(0xFFF9A825)
    val GoldLight = Color(0xFFFFF8E1)
    val GoldDark = Color(0xFFE65100)

    // Neutral (Dynamic with animation for Dark/Light mode)
    val Surface: Color
        @Composable get() = animateColorAsState(MaterialTheme.colorScheme.background, label = "Surface").value
    
    val CardBg: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.CardBg else Color(0xFFFFFFFF), label = "CardBg").value
        
    val TextPrimary: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.TextPrimary else Color(0xFF1A1A1A), label = "TextPrimary").value
        
    val TextSecondary: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.TextSecondary else Color(0xFF424242), label = "TextSecondary").value
        
    val TextHint: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.TextHint else Color(0xFF757575), label = "TextHint").value
        
    val Divider: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.Divider else Color(0xFFD0D0D0), label = "Divider").value

    /** Divider đậm hơn cho ngữ cảnh cần phân tách rõ (ngoài trời, độ sáng thấp). */
    val DividerStrong: Color
        @Composable get() = animateColorAsState(if (isDarkTheme) Dark.DividerStrong else Color(0xFFB8B8B8), label = "DividerStrong").value

    // Status
    val Success = Color(0xFF43A047)
    val Warning = Color(0xFFFF8F00)
    val Error = Color(0xFFE53935)
    val Info = Color(0xFF1976D2)

    // Offline badge
    val OfflineBg = Color(0xFFFFF3E0)
    val OfflineText = Color(0xFFE65100)

    // Sync badge
    val SyncingBg = Color(0xFFE3F2FD)
    val SyncingText = Color(0xFF1565C0)

    // Locked card
    val LockedBg = Color(0xFFFCE4EC)
    val LockedText = Color(0xFFC62828)

    /** Tint nền card khi phiếu khoá — hồng rất nhạt, không nuốt nội dung. */
    val LockedSurface: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.LockedSurface else Color(0xFFFFF1F3),
            label = "LockedSurface"
        ).value

    /** Tint nền card khi đã trả đủ tiền — xanh rất nhạt. */
    val PaidSurface: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.PaidSurface else Color(0xFFF1F8F2),
            label = "PaidSurface"
        ).value

    // Coming Soon
    val ComingSoonBg = Color(0xFFF3E5F5)
    val ComingSoonText = Color(0xFF6A1B9A)

    // ── Detail screen surfaces (Fluent Tonal Layers) ─────────────────────
    /** Highlight cho dòng "Còn lại" — đỏ đậm light, đỏ mềm dark */
    val RemainingHighlight: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.RemainingHighlight else Color(0xFFB71C1C),
            label = "RemainingHighlight"
        ).value

    /** Surface cho card highlight tổng khối lượng */
    val WeightSurface: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.WeightSurface else Color(0xFFFFF8E1),
            label = "WeightSurface"
        ).value

    /** Surface cho card tài chính */
    val MoneySurface: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.MoneySurface else Color(0xFFE8F5E9),
            label = "MoneySurface"
        ).value

    /** Surface lighter (sub card depth layer 2) */
    val SurfaceContainer: Color
        @Composable get() = animateColorAsState(
            if (isDarkTheme) Dark.SurfaceContainer else Color(0xFFF5F5F5),
            label = "SurfaceContainer"
        ).value

    /** Acrylic overlay nhẹ — cho gradient & glassmorphism */
    val AcrylicLight = Color(0x14FFFFFF)   // 8% white
    val AcrylicMedium = Color(0x33FFFFFF)  // 20% white

    /** Shadow tokens — Fluent depth layering */
    val ShadowAmbient = Color(0x1A000000)  // 10% black
    val ShadowDirect = Color(0x33000000)   // 20% black

    // Dark theme overrides
    object Dark {
        val Surface = Color(0xFF121212)
        val CardBg = Color(0xFF1E1E1E)
        val SurfaceContainer = Color(0xFF252525)
        val TextPrimary = Color(0xFFE0E0E0)
        val TextSecondary = Color(0xFFE0E0E0)
        val TextHint = Color(0xFFAAAAAA)
        val Divider = Color(0xFF3D3D3D)
        val DividerStrong = Color(0xFF555555)
        val LockedSurface = Color(0xFF2A1F22)
        val PaidSurface = Color(0xFF1A2820)
        val GreenPrimary = Color(0xFFA5D6A7) // Green80 (light soft green)
        val GreenSurface = Color(0xFF1E3A24) // Soft dark forest green
        val RemainingHighlight = Color(0xFFEF5350) // Đỏ mềm cho dark
        val WeightSurface = Color(0xFF332B14)      // Vàng đất tối
        val MoneySurface = Color(0xFF1A2E1F)       // Xanh rêu tối
    }
}

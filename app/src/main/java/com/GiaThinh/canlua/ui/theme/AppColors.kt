package com.GiaThinh.canlua.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Design tokens chuẩn Fluent Design cho app Cân Lúa.
 * Bảng màu lấy cảm hứng từ lúa gạo Việt Nam.
 */
object AppColors {
    // Primary — xanh lá lúa
    val GreenPrimary: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.GreenPrimary else Color(0xFF2E7D32), label = "GreenPrimary").value
    val GreenLight = Color(0xFF4CAF50)
    val GreenSurface: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.GreenSurface else Color(0xFFE8F5E9), label = "GreenSurface").value
    val GreenDark = Color(0xFF1B5E20)

    // Accent — vàng lúa chín
    val GoldAccent = Color(0xFFF9A825)
    val GoldLight = Color(0xFFFFF8E1)
    val GoldDark = Color(0xFFE65100)

    // Neutral (Dynamic with animation for Dark/Light mode)
    val Surface: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.Surface else Color(0xFFFAFAFA), label = "Surface").value
    
    val CardBg: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.CardBg else Color(0xFFFFFFFF), label = "CardBg").value
        
    val TextPrimary: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.TextPrimary else Color(0xFF1A1A1A), label = "TextPrimary").value
        
    val TextSecondary: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.TextSecondary else Color(0xFF424242), label = "TextSecondary").value
        
    val TextHint: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.TextHint else Color(0xFF757575), label = "TextHint").value
        
    val Divider: Color
        @Composable get() = animateColorAsState(if (isSystemInDarkTheme()) Dark.Divider else Color(0xFFEEEEEE), label = "Divider").value

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

    // Coming Soon
    val ComingSoonBg = Color(0xFFF3E5F5)
    val ComingSoonText = Color(0xFF6A1B9A)

    // Dark theme overrides
    object Dark {
        val Surface = Color(0xFF121212)
        val CardBg = Color(0xFF1E1E1E)
        val TextPrimary = Color(0xFFE0E0E0)
        val TextSecondary = Color(0xFFE0E0E0)
        val TextHint = Color(0xFFAAAAAA)
        val Divider = Color(0xFF333333)
        val GreenPrimary = Color(0xFFA5D6A7) // Green80 (light soft green)
        val GreenSurface = Color(0xFF1E3A24) // Soft dark forest green
    }
}

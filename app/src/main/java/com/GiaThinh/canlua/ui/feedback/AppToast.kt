package com.GiaThinh.canlua.ui.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * AppToast — pill snackbar trong-app, thay thế `Toast.makeText` cũ.
 *
 * Lý do tách khỏi Material Snackbar:
 *  - 4 variant ngữ nghĩa (success / info / warning / error) tự động kèm icon
 *    + màu theme + haptic feedback phù hợp.
 *  - Trượt từ đáy (thumb-zone) thay vì top, không che FAB nhờ
 *    `windowInsetsPadding(navigationBars)`.
 *  - Auto-dismiss 1.8s, queue đơn giản (chỉ giữ message mới nhất, message
 *    cũ bị thay ngay) — tránh chồng nhau như Toast.
 *
 * Cách dùng:
 * ```
 * val appToast = LocalAppToast.current
 * appToast.success("Đã lưu phiếu cân")
 * appToast.error("Không kết nối được mạng")
 * ```
 */
class AppToastController internal constructor(
    private val state: MutableState<AppToastMessage?>
) {
    fun show(message: AppToastMessage) {
        state.value = message
    }

    fun success(text: String) = show(AppToastMessage(text, AppToastVariant.SUCCESS))
    fun info(text: String) = show(AppToastMessage(text, AppToastVariant.INFO))
    fun warning(text: String) = show(AppToastMessage(text, AppToastVariant.WARNING))
    fun error(text: String) = show(AppToastMessage(text, AppToastVariant.ERROR))
}

enum class AppToastVariant { SUCCESS, INFO, WARNING, ERROR }

data class AppToastMessage(
    val text: String,
    val variant: AppToastVariant,
    /** Unique để LaunchedEffect re-trigger khi hiện lại cùng text. */
    val id: Long = System.nanoTime()
)

val LocalAppToast = compositionLocalOf<AppToastController> {
    error("AppToastHost chưa được mount. Bọc app trong `AppToastHost { ... }` ở root.")
}

/**
 * Host bọc toàn bộ content app, lo việc render pill toast nổi đè lên trên.
 * Đặt sát root composable (sau Theme, trước NavHost) để mọi screen đều
 * dùng được qua `LocalAppToast.current`.
 */
@Composable
fun AppToastHost(content: @Composable () -> Unit) {
    val state = remember { mutableStateOf<AppToastMessage?>(null) }
    val controller = remember { AppToastController(state) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppToast provides controller) {
            content()
        }

        val current = state.value
        // Auto-dismiss 1.8s + haptic theo variant.
        LaunchedEffect(current?.id) {
            if (current != null) {
                triggerHaptic(haptic, current.variant)
                delay(1_800)
                if (state.value?.id == current.id) state.value = null
            }
        }

        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
        ) {
            current?.let { ToastPill(it, onDismiss = { state.value = null }) }
        }
    }
}

@Composable
private fun ToastPill(message: AppToastMessage, onDismiss: () -> Unit) {
    val (icon, bg, fg) = when (message.variant) {
        AppToastVariant.SUCCESS -> Triple(Icons.Default.CheckCircle, AppColors.GreenPrimary, Color.White)
        AppToastVariant.INFO -> Triple(Icons.Default.Info, AppColors.Info, Color.White)
        AppToastVariant.WARNING -> Triple(Icons.Default.Warning, AppColors.GoldDark, Color.White)
        AppToastVariant.ERROR -> Triple(Icons.Default.Error, AppColors.Error, Color.White)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

/**
 * Map variant → haptic effect. SUCCESS = LongPress (chốt giao dịch),
 * ERROR/WARNING = TextHandleMove (rung nhẹ thay vì chấn rung mạnh).
 */
private fun triggerHaptic(haptic: HapticFeedback, variant: AppToastVariant) {
    val effect = when (variant) {
        AppToastVariant.SUCCESS -> HapticFeedbackType.LongPress
        AppToastVariant.INFO -> HapticFeedbackType.TextHandleMove
        AppToastVariant.WARNING -> HapticFeedbackType.TextHandleMove
        AppToastVariant.ERROR -> HapticFeedbackType.LongPress
    }
    runCatching { haptic.performHapticFeedback(effect) }
}

private operator fun <A, B, C> Triple<A, B, C>.component1() = first
private operator fun <A, B, C> Triple<A, B, C>.component2() = second
private operator fun <A, B, C> Triple<A, B, C>.component3() = third

package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Dấu chấm pulse hiển thị trạng thái đồng bộ.
 *
 * - Idle/Success: chấm tĩnh xanh nhạt (sẵn sàng)
 * - Syncing: chấm xanh "thở" (scale + alpha lặp 1200ms)
 * - Error: chấm đỏ tĩnh
 */
@Composable
fun SyncStatusPulse(
    status: SyncStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val (dotColor, label) = when (status) {
        is SyncStatus.Syncing -> AppColors.GreenPrimary to "Đang đồng bộ"
        is SyncStatus.Success -> AppColors.GreenPrimary to "Đã đồng bộ"
        is SyncStatus.Error   -> Color(0xFFE53935) to "Lỗi đồng bộ"
        else                  -> AppColors.GreenLight to "Sẵn sàng"
    }

    val infinite = rememberInfiniteTransition(label = "sync_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isSyncing = status is SyncStatus.Syncing
    val effectiveScale = if (isSyncing) pulseScale else 1f
    val effectiveAlpha = if (isSyncing) pulseAlpha else 1f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(effectiveScale)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = effectiveAlpha))
        )
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

package com.giathinh.canlua.ui.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.repository.SyncStatus
import com.giathinh.canlua.ui.theme.AppColors

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
        is SyncStatus.Syncing -> AppColors.GreenPrimary to stringResource(R.string.sync_status_syncing)
        is SyncStatus.Success -> AppColors.GreenPrimary to stringResource(R.string.sync_status_success)
        is SyncStatus.Error   -> Color(0xFFE53935) to stringResource(R.string.sync_status_error)
        else                  -> AppColors.GreenLight to stringResource(R.string.sync_status_ready)
    }

    // Chỉ chạy infiniteRepeatable khi đang sync — tránh tick 60fps khi idle.
    // Trước đây animation chạy mãi và chỉ if/else giá trị → engine vẫn tick mỗi frame.
    val isSyncing = status is SyncStatus.Syncing
    val effectiveScale: Float
    val effectiveAlpha: Float
    if (isSyncing) {
        val infinite = rememberInfiniteTransition(label = "sync_pulse")
        effectiveScale = infinite.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        ).value
        effectiveAlpha = infinite.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        ).value
    } else {
        effectiveScale = 1f
        effectiveAlpha = 1f
    }

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

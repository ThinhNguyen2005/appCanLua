package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode

/**
 * Shimmer placeholder modifier — vẽ gradient dải sáng quét ngang object.
 * Dùng cho skeleton loading state khi mạng chậm.
 *
 * Apply LÊN [Box] / [Spacer] đã có background màu xám nhạt.
 *
 * Usage:
 * ```
 * Box(Modifier.background(AppColors.SurfaceContainer).shimmer())
 * ```
 */
@Composable
fun Modifier.shimmer(
    highlightColor: Color = Color.White.copy(alpha = 0.55f),
    durationMillis: Int = 1100
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-progress"
    )

    drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        // Sweep dải sáng từ trái sang phải
        val startX = width * (progress * 2f - 1f)
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                highlightColor,
                Color.Transparent
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + width / 2f, height)
        )
        drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
    }
}

package com.giathinh.canlua.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

const val SHIMMER_DURATION = 1400

/**
 * CompositionLocal cung cấp tiến trình shimmer dùng chung trong cây UI composable.
 */
val LocalShimmerProgress = androidx.compose.runtime.compositionLocalOf<Float?> { null }

/**
 * Quản lý tiến trình quét shimmer dùng chung (progress) giữa các skeleton block,
 * giúp đồng bộ animation và chỉ chạy 1 infinite transition duy nhất.
 */
@Composable
fun rememberShimmerProgress(durationMillis: Int = SHIMMER_DURATION): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    return transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    ).value
}

/**
 * Bộ màu skeleton thích ứng Light/Dark mode từ theme.
 * @param surface Màu nền bề mặt card placeholder.
 * @param shimmerBase Màu nền cơ bản của gradient shimmer.
 * @param shimmerHighlight Màu dải sáng quét qua giữa gradient.
 */
data class SkeletonColors(
    val surface: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
) {
    val shimmerGradient: List<Color> = listOf(shimmerBase, shimmerHighlight, shimmerBase)
}

/**
 * Lấy bộ màu skeleton phù hợp với theme hiện tại (light hoặc dark),
 * kế thừa từ [AppColors] và [MaterialTheme.colorScheme], có điểm xuyết nhẹ sắc xanh thương hiệu.
 */
@Composable
fun skeletonColors(): SkeletonColors {
    val surfaceColor = AppColors.CardBg
    val isDark = surfaceColor.luminance() < 0.5f
    val brandGreen = AppColors.GreenPrimary

    return if (isDark) {
        val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        val highlight = MaterialTheme.colorScheme.surfaceVariant
            .copy(alpha = 0.65f)
            .compositeOver(brandGreen.copy(alpha = 0.08f))
        SkeletonColors(
            surface = surfaceColor,
            shimmerBase = base,
            shimmerHighlight = highlight,
        )
    } else {
        val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val highlight = Color.White
            .copy(alpha = 0.7f)
            .compositeOver(brandGreen.copy(alpha = 0.05f))
        SkeletonColors(
            surface = surfaceColor,
            shimmerBase = base,
            shimmerHighlight = highlight,
        )
    }
}

/**
 * Hiệu ứng shimmer quét đè (overlay) — dùng cho element đã có sẵn màu nền
 * (ví dụ: `Box(Modifier.background(color).shimmer())`).
 * Giữ nguyên màu nền gốc và quét dải sáng mờ [highlightColor] qua bề mặt.
 */
fun Modifier.shimmer(
    highlightColor: Color = Color.White.copy(alpha = 0.45f),
    durationMillis: Int = SHIMMER_DURATION,
    progress: Float? = null
): Modifier = composed {
    val shimmerProgress = progress ?: LocalShimmerProgress.current ?: rememberShimmerProgress(durationMillis)
    val gradientColors = remember(highlightColor) {
        listOf(
            Color.Transparent,
            highlightColor,
            Color.Transparent
        )
    }

    this.drawWithCache {
        val width = size.width
        val height = size.height
        val diagonal = width + height
        val startX = shimmerProgress * diagonal - width
        val brush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(startX, 0f),
            end = Offset(startX + width, height),
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush)
        }
    }
}

/**
 * Hiệu ứng shimmer tạo nền (background) — dùng trực tiếp làm nền cho các khối skeleton.
 * Quét theo góc 45° tỷ lệ thuận theo kích thước thực tế của composable.
 */
fun Modifier.shimmerEffect(
    skeletonColors: SkeletonColors? = null,
    progress: Float? = null
): Modifier = composed {
    val colors = skeletonColors ?: skeletonColors()
    val shimmerProgress = progress ?: LocalShimmerProgress.current ?: rememberShimmerProgress()

    this.drawWithCache {
        val width = size.width
        val height = size.height
        val diagonal = width + height
        val startX = shimmerProgress * diagonal - width
        val brush = Brush.linearGradient(
            colors = colors.shimmerGradient,
            start = Offset(startX, 0f),
            end = Offset(startX + width, height),
        )
        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

/**
 * Shared Shimmer Box — khối placeholder nguyên tử dùng chung trên toàn ứng dụng.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    colors: SkeletonColors = skeletonColors(),
    progress: Float? = LocalShimmerProgress.current,
    isCircle: Boolean = false,
    shape: Shape? = null
) {
    val resolvedShape = shape ?: if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(resolvedShape)
            .shimmerEffect(colors, progress)
    )
}

/**
 * Placeholder dự phòng mặc định khi tải màn hình.
 */
@Composable
fun DefaultSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

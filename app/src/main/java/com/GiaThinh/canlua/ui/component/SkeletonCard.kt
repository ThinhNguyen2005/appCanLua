package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Modifier shimmer 45° sweep — gradient sáng quét chéo qua bề mặt.
 * Tạo cảm giác "đang load" tinh tế, không nhịp full-block alpha.
 * MÀU tự động thích ứng light/dark mode qua [skeletonColors].
 *
 * Áp dụng trực tiếp lên element có background.
 */
fun Modifier.shimmerEffect(skeletonColors: SkeletonColors? = null): Modifier = composed {
    val colors = skeletonColors ?: skeletonColors()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer_sweep")
    val startOffsetX by transition.animateFloat(
        initialValue = -2f * size.width.toFloat(),
        targetValue = 2f * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
        ),
        label = "shimmer_offset",
    )

    background(
        brush = Brush.linearGradient(
            colors = colors.shimmerGradient,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
        ),
    ).onGloballyPositioned { size = it.size }
}

/**
 * Bộ màu skeleton thích ứng light/dark mode.
 * @param surface Màu nền card skeleton.
 * @param shimmerBase Màu base của shimmer gradient (2 vế).
 * @param shimmerHighlight Màu highlight giữa của shimmer gradient.
 */
data class SkeletonColors(
    val surface: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
) {
    val shimmerGradient: List<Color>
        get() = listOf(shimmerBase, shimmerHighlight, shimmerBase)
}

/**
 * Lấy bộ màu skeleton phù hợp với theme hiện tại (light hoặc dark).
 * Dùng trong @Composable context để đọc [MaterialTheme.colorScheme].
 */
@Composable
fun skeletonColors(): SkeletonColors {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) {
        SkeletonColors(
            surface = Color(0xFF2C2C2C),
            shimmerBase = Color(0xFF1E1E1E),
            shimmerHighlight = Color(0xFF3A3A3A),
        )
    } else {
        SkeletonColors(
            surface = Color(0xFFF5F5F5),
            shimmerBase = Color(0xFFD6D6D6),
            shimmerHighlight = Color(0xFFEDEDED),
        )
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

/**
 * Skeleton loading card — placeholder với shimmer 45° sweep.
 * MÀU tự động thích ứng light/dark mode.
 */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val colors = skeletonColors()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmerEffect(colors)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(widthFraction = 0.72f, height = 16.dp, corner = 8.dp, colors = colors)
                ShimmerBlock(widthFraction = 0.46f, height = 12.dp, corner = 6.dp, colors = colors)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBlock(widthFraction = 0.32f, height = 26.dp, corner = 10.dp, colors = colors)
                    ShimmerBlock(widthFraction = 0.28f, height = 26.dp, corner = 10.dp, colors = colors)
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(widthFraction = 0.18f, height = 16.dp, corner = 8.dp, colors = colors)
                ShimmerBlock(widthFraction = 0.22f, height = 28.dp, corner = 12.dp, colors = colors)
            }
        }
    }
}

@Composable
private fun ShimmerBlock(
    widthFraction: Float,
    height: Dp,
    corner: Dp = 4.dp,
    colors: SkeletonColors = skeletonColors(),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmerEffect(colors),
    )
}

@Composable
fun SkeletonList(count: Int = 3) {
    Column {
        repeat(count) {
            SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

/**
 * Skeleton "Hôm nay" — placeholder cho [CardListSummaryCard].
 * MÀU tự động thích ứng light/dark mode.
 */
@Composable
fun SummaryCardSkeleton(modifier: Modifier = Modifier) {
    val colors = skeletonColors()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(widthFraction = 0.34f, height = 12.dp, corner = 6.dp, colors = colors)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(widthFraction = 0.52f, height = 18.dp, corner = 8.dp, colors = colors)
                }
                ShimmerBlock(widthFraction = 0.18f, height = 24.dp, corner = 12.dp, colors = colors)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.shimmerBase.copy(alpha = 0.38f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .shimmerEffect(colors)
                        )
                        ShimmerBlock(widthFraction = 0.82f, height = 10.dp, corner = 5.dp, colors = colors)
                        ShimmerBlock(widthFraction = 0.7f, height = 16.dp, corner = 8.dp, colors = colors)
                    }
                }
            }
        }
    }
}

/**
 * Skeleton tổng hợp cho màn "Cân lúa" — summary card + danh sách phiếu.
 * Dùng LazyColumn ĐỂ KHỚP CẤU TRÚC với real content trong CardListScreen:
 *   - contentPadding: top=8dp, bottom=96dp, start/end=16dp
 *   - verticalArrangement: spacedBy(8dp)
 *   - item đầu tiên: summary card
 *   - item thứ hai: empty-state-height card (placeholder cho list)
 *   - items còn lại: skeleton cards với header trước mỗi nhóm
 * Khi skeleton → real content swap, Compose thấy CÙNG layout tree →
 * không phải unmeasure → measure lại → KHÔNG còn jank/bottom-up pop.
 */
@Composable
fun CardListSkeleton(
    count: Int = 3,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "skeleton_summary_header") {
            Spacer(Modifier.height(0.dp))
        }
        item(key = "skeleton_summary") {
            SummaryCardSkeleton()
        }
        item(key = "skeleton_filter_placeholder") {
            Spacer(Modifier.height(44.dp))
        }
        item(key = "skeleton_list_header") {
            Text(
                text = "",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        items(
            count = count,
            key = { "skeleton_card_$it" }
        ) { _ ->
            SkeletonCard(modifier = Modifier.padding(vertical = 0.dp))
        }
    }
}

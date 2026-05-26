package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Modifier shimmer 45° sweep — gradient sáng quét chéo qua bề mặt.
 * Tạo cảm giác "đang load" tinh tế, không nhịp full-block alpha.
 *
 * Áp dụng trực tiếp lên element có background.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
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
            colors = listOf(
                Color(0xFFD6D6D6),
                Color(0xFFEDEDED),
                Color(0xFFD6D6D6),
            ),
            // 45° sweep: from (x, 0) to (x + width, height) — kéo chéo
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
        ),
    ).onGloballyPositioned { size = it.size }
}

/**
 * Skeleton loading card — placeholder với shimmer 45° sweep.
 * Thay thế full-block alpha tween cũ bằng gradient sáng quét chéo.
 */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBlock(widthFraction = 0.6f, height = 20.dp)
            Spacer(Modifier.height(12.dp))
            ShimmerBlock(widthFraction = 0.9f, height = 14.dp)
            Spacer(Modifier.height(8.dp))
            ShimmerBlock(widthFraction = 0.75f, height = 14.dp)
            Spacer(Modifier.height(12.dp))
            ShimmerBlock(widthFraction = 0.4f, height = 32.dp, corner = 8.dp)
        }
    }
}

@Composable
private fun ShimmerBlock(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp = 4.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmerEffect(),
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
 * Cùng kích thước/khoảng cách để khi data thật xuất hiện không bị "pop in" từ dưới.
 */
@Composable
fun SummaryCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F0)),
        shape = RoundedCornerShape(16.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.5f)) {
                ShimmerBlock(widthFraction = 0.8f, height = 14.dp)
                Spacer(Modifier.height(10.dp))
                ShimmerBlock(widthFraction = 0.7f, height = 28.dp, corner = 6.dp)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.End,
            ) {
                ShimmerBlock(widthFraction = 0.7f, height = 14.dp)
                Spacer(Modifier.height(10.dp))
                ShimmerBlock(widthFraction = 0.9f, height = 28.dp, corner = 6.dp)
            }
        }
    }
}

/**
 * Skeleton tổng hợp cho màn "Cân lúa" — summary card + danh sách phiếu.
 * Layout match với [CardListScreen] để fade-out → fade-in mượt, không pop-in.
 */
@Composable
fun CardListSkeleton(count: Int = 3) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        SummaryCardSkeleton()
        Spacer(Modifier.height(12.dp))
        repeat(count) {
            SkeletonCard(modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

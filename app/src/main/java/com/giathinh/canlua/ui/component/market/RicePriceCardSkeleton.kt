package com.giathinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.shimmer
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Skeleton placeholder thay cho `RicePriceCard` khi đang load dữ liệu lần đầu.
 * Hiệu ứng shimmer chạy ngang để báo hiệu data đang đến.
 */
@Composable
fun RicePriceCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg)
            .border(
                1.dp,
                AppColors.Divider.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: title + trend + chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(width = 140.dp, height = 18.dp)
                Spacer(Modifier.weight(1f))
                ShimmerBlock(width = 56.dp, height = 18.dp, shape = RoundedCornerShape(8.dp))
                ShimmerBlock(size = 18.dp, shape = RoundedCornerShape(4.dp))
            }
            // Meta row: rice type + region + updated
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBlock(width = 64.dp, height = 16.dp, shape = RoundedCornerShape(8.dp))
                Spacer(Modifier.width(8.dp))
                ShimmerBlock(width = 52.dp, height = 16.dp, shape = RoundedCornerShape(8.dp))
                Spacer(Modifier.width(8.dp))
                ShimmerBlock(width = 90.dp, height = 12.dp)
            }
            // Price grid: 3 columns
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBlock(modifier = Modifier.weight(1f), height = 64.dp, shape = RoundedCornerShape(12.dp))
                ShimmerBlock(modifier = Modifier.weight(1f), height = 64.dp, shape = RoundedCornerShape(12.dp))
                ShimmerBlock(modifier = Modifier.weight(1f), height = 64.dp, shape = RoundedCornerShape(12.dp))
            }
        }
    }
}

@Composable
fun MarketSkeletonList(modifier: Modifier = Modifier, items: Int = 4) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(items) { RicePriceCardSkeleton() }
    }
}

@Composable
private fun ShimmerBlock(
    modifier: Modifier = Modifier,
    width: Dp = 0.dp,
    height: Dp = 0.dp,
    size: Dp = 0.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val mod = modifier.then(
        when {
            size > 0.dp -> Modifier.size(size)
            else -> Modifier.width(width).height(height)
        }
    )
    Box(
        modifier = mod
            .clip(shape)
            .background(AppColors.SurfaceContainer)
            .shimmer()
    )
}

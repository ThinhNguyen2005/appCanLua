package com.GiaThinh.canlua.ui.component.market

import androidx.compose.foundation.background
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
import com.GiaThinh.canlua.ui.component.shimmer
import com.GiaThinh.canlua.ui.theme.AppColors

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
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBlock(size = 40.dp, shape = CircleShape)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBlock(width = 140.dp, height = 16.dp)
                    ShimmerBlock(width = 90.dp, height = 12.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            ShimmerBlock(width = 180.dp, height = 24.dp)
            ShimmerBlock(width = 220.dp, height = 14.dp)
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
    width: Dp = 0.dp,
    height: Dp = 0.dp,
    size: Dp = 0.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val mod = when {
        size > 0.dp -> Modifier.size(size)
        else -> Modifier.width(width).height(height)
    }
    Box(
        modifier = mod
            .clip(shape)
            .background(AppColors.SurfaceContainer)
            .shimmer()
    )
}
